package com.nuclearunicorn.serialkiller.game.ai.llm;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Talks to a llama.cpp {@code llama-server} /completion endpoint with GBNF-constrained
 * decoding, so the model can only emit a valid JSON command array (§5.2).
 *
 * Threading (§3): a single worker thread does the blocking HTTP call. Results land in a
 * concurrent map the game thread drains via {@link #poll}. The worker never touches game
 * state — it receives a String prompt and produces a String completion.
 */
public class LlamaHttpInferenceService implements InferenceService {

    private final String endpoint;
    private final String grammar;
    private final int maxTokens;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();
    private final Gson gson = new Gson();

    // Single worker: CPU inference is serial anyway, and this keeps ordering simple.
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "llm-inference");
        t.setDaemon(true);
        return t;
    });

    private final ConcurrentHashMap<String, String> results = new ConcurrentHashMap<>();
    // uids with a request queued or running — enforces drop-if-busy (one per uid).
    private final ConcurrentHashMap<String, Boolean> inFlight = new ConcurrentHashMap<>();

    public LlamaHttpInferenceService(int port, String grammar, int maxTokens) {
        this.endpoint = "http://127.0.0.1:" + port + "/completion";
        this.grammar = grammar;
        this.maxTokens = maxTokens;
    }

    @Override
    public void submit(String uid, String prompt) {
        if (inFlight.putIfAbsent(uid, Boolean.TRUE) != null) {
            return;   // drop-if-busy
        }
        LlmDebug.log("%s: HTTP submit -> %s (prompt %d chars)", uid, endpoint, prompt.length());
        worker.submit(() -> {
            try {
                String completion = complete(prompt);
                if (completion != null) {
                    results.put(uid, completion);
                }
            } catch (Exception e) {
                // Timeout / refused / non-200 → leave no result; NPC keeps its agenda (§12).
                System.err.println("llm inference failed for " + uid + ": " + e);
                LlmDebug.log("%s: HTTP inference FAILED: %s", uid, e);
            } finally {
                inFlight.remove(uid);
            }
        });
    }

    @Override
    public boolean isBusy(String uid) {
        return inFlight.containsKey(uid);
    }

    @Override
    public String poll(String uid) {
        return results.remove(uid);
    }

    @Override
    public void shutdown() {
        worker.shutdownNow();
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
