package com.nuclearunicorn.serialkiller.game.ai.llm;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.nuclearunicorn.serialkiller.game.ai.llm.sense.Salience;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Talks to a llama.cpp {@code llama-server} /completion endpoint with GBNF-constrained
 * decoding, so the model can only emit a valid JSON command array (§5.2).
 *
 * <p>Threading (§3): one worker thread does the blocking HTTP call. Results land in a
 * concurrent map the game thread drains via {@link #poll}. The worker never touches game
 * state — it receives a String prompt and produces a String completion.
 *
 * <h3>Why the queue is a priority queue</h3>
 * CPU inference costs one to three seconds per request. Every pedestrian and policeman in
 * the chunk is an LLM agent and the near bucket is a 24-tile radius, so requests arrive far
 * faster than they can be served and the queue is permanently backed up. A plain FIFO
 * therefore made <i>arrival order</i> the arbiter: the NPC a human was standing in front of
 * queued behind a dozen NPCs doing nothing in particular, and answered minutes later or
 * never. Serving by {@link Salience} instead means a DIRECTED request skips the ambient
 * backlog, and a saturated queue degrades by dropping idle chatter rather than conversation.
 */
public class LlamaHttpInferenceService implements InferenceService {

    private final String endpoint;
    private final String grammar;
    private final int maxTokens;
    private final int queueCapacity;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();
    private final Gson gson = new Gson();

    /** Pending work, highest salience first; ties break oldest-first so nobody starves outright. */
    private final PriorityQueue<Request> queue = new PriorityQueue<>(
            Comparator.comparingInt((Request r) -> -r.priority).thenComparingLong(r -> r.seq));
    private final AtomicLong sequence = new AtomicLong();

    private final ConcurrentHashMap<String, String> results = new ConcurrentHashMap<>();
    // uid -> priority of the request queued or running for it.
    private final ConcurrentHashMap<String, Integer> inFlight = new ConcurrentHashMap<>();

    private final Thread worker;
    private volatile boolean running = true;

    private static class Request {
        final String uid;
        final String prompt;
        final int priority;
        final long seq;

        Request(String uid, String prompt, int priority, long seq) {
            this.uid = uid;
            this.prompt = prompt;
            this.priority = priority;
            this.seq = seq;
        }
    }

    public LlamaHttpInferenceService(int port, String grammar, int maxTokens, int queueCapacity) {
        this.endpoint = "http://127.0.0.1:" + port + "/completion";
        this.grammar = grammar;
        this.maxTokens = maxTokens;
        this.queueCapacity = Math.max(1, queueCapacity);

        this.worker = new Thread(this::workerLoop, "llm-inference");
        this.worker.setDaemon(true);
        this.worker.start();
    }

    @Override
    public void submit(String uid, String prompt, int priority) {
        synchronized (queue) {
            Integer pending = inFlight.get(uid);
            if (pending != null) {
                // Already working on this NPC. Let a more urgent signal replace a stale
                // ambient one — otherwise drop-if-busy would swallow the interrupt that
                // matters most (being spoken to while an idle re-plan is queued).
                if (priority <= pending) {
                    return;
                }
                queue.removeIf(r -> r.uid.equals(uid));
                LlmDebug.log("%s: upgrading queued request %s -> %s",
                        uid, Salience.label(pending), Salience.label(priority));
            }

            if (queue.size() >= queueCapacity && !evictWeakest(priority)) {
                LlmDebug.log("%s: queue full (%d) and nothing weaker to drop - request skipped",
                        uid, queue.size());
                return;
            }

            inFlight.put(uid, priority);
            queue.add(new Request(uid, prompt, priority, sequence.getAndIncrement()));
            LlmDebug.log("%s: queued at %s (depth %d, prompt %d chars)",
                    uid, Salience.label(priority), queue.size(), prompt.length());
            queue.notifyAll();
        }
    }

    /**
     * Make room by dropping the least important queued request, but only if it is strictly
     * less important than the incoming one. Called with the queue lock held.
     */
    private boolean evictWeakest(int incomingPriority) {
        Request weakest = null;
        for (Request r : queue) {
            if (weakest == null || r.priority < weakest.priority
                    || (r.priority == weakest.priority && r.seq < weakest.seq)) {
                weakest = r;
            }
        }
        if (weakest == null || weakest.priority >= incomingPriority) {
            return false;
        }
        queue.remove(weakest);
        inFlight.remove(weakest.uid);
        LlmDebug.log("%s: evicted from full queue to make room for a %s request",
                weakest.uid, Salience.label(incomingPriority));
        return true;
    }

    @Override
    public boolean isBusy(String uid) {
        return inFlight.containsKey(uid);
    }

    /** Priority of this uid's pending request, or 0 when it has none. */
    public int pendingPriority(String uid) {
        Integer p = inFlight.get(uid);
        return p == null ? 0 : p;
    }

    @Override
    public String poll(String uid) {
        return results.remove(uid);
    }

    @Override
    public void shutdown() {
        running = false;
        synchronized (queue) {
            queue.notifyAll();
        }
        worker.interrupt();
    }

    private void workerLoop() {
        while (running) {
            Request request;
            synchronized (queue) {
                while (running && queue.isEmpty()) {
                    try {
                        queue.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
                if (!running) {
                    return;
                }
                request = queue.poll();
            }

            try {
                String completion = complete(request.prompt);
                if (completion != null) {
                    results.put(request.uid, completion);
                }
            } catch (Exception e) {
                // Timeout / refused / non-200 -> leave no result; NPC keeps its agenda (§12).
                System.err.println("llm inference failed for " + request.uid + ": " + e);
                LlmDebug.log("%s: HTTP inference FAILED: %s", request.uid, e);
            } finally {
                inFlight.remove(request.uid);
            }
        }
    }

    /** Blocking HTTP round-trip. Runs only on the worker thread. */
    private String complete(String prompt) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("prompt", prompt);
        body.addProperty("n_predict", maxTokens);
        body.addProperty("temperature", 0.7);
        body.addProperty("cache_prompt", true);
        if (grammar != null && !grammar.isEmpty()) {
            body.addProperty("grammar", grammar);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                .build();

        long start = System.currentTimeMillis();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            System.err.println("llama-server returned " + response.statusCode());
            LlmDebug.log("HTTP non-200: %d", response.statusCode());
            return null;
        }

        JsonObject json = gson.fromJson(response.body(), JsonObject.class);
        String content = json.has("content") ? json.get("content").getAsString() : null;
        LlmDebug.log("HTTP 200 in %dms, content=%s",
                System.currentTimeMillis() - start, content == null ? "null" : content.replace('\n', ' '));
        return content;
    }
}
