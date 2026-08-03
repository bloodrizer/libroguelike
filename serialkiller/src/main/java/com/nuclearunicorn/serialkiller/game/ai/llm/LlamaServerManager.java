package com.nuclearunicorn.serialkiller.game.ai.llm;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Spawns and tears down {@code llama-server} processes for the game (§10). One instance
 * per tier. The game owns the process lifetime: started when LLM NPCs boot, killed by a
 * JVM shutdown hook so no orphan server survives the game.
 */
public class LlamaServerManager {

    private final String serverBinary;
    private final List<Process> processes = new ArrayList<>();
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(1))
            .build();

    public LlamaServerManager(String serverBinary) {
        this.serverBinary = serverBinary;
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown, "llama-shutdown"));
    }

    /**
     * Start one server for a tier and block until {@code /health} reports ready.
     * Returns false if the binary can't launch or never becomes healthy — the caller
     * then degrades that tier (§10, §12).
     */
    public boolean startTier(LlmConfig.Tier tier) {
        ProcessBuilder pb = new ProcessBuilder(
                serverBinary,
                "-m", tier.model,
                "--port", Integer.toString(tier.port),
                "--host", "127.0.0.1");
        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);

        try {
            processes.add(pb.start());
        } catch (IOException e) {
            System.err.println("Failed to start llama-server (" + serverBinary + "): " + e);
            return false;
        }

        return awaitHealth(tier.port, 60);
    }

    /** Poll {@code /health} once per second until ready or the timeout elapses. */
    private boolean awaitHealth(int port, int timeoutSeconds) {
        URI health = URI.create("http://127.0.0.1:" + port + "/health");
        HttpRequest request = HttpRequest.newBuilder(health)
                .timeout(Duration.ofSeconds(1)).GET().build();

        for (int i = 0; i < timeoutSeconds; i++) {
            try {
                HttpResponse<String> r = http.send(request, HttpResponse.BodyHandlers.ofString());
                if (r.statusCode() == 200) {
                    return true;
                }
            } catch (Exception ignored) {
                // not up yet
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        System.err.println("llama-server on port " + port + " never became healthy");
        return false;
    }

    public void shutdown() {
        for (Process p : processes) {
            if (p.isAlive()) {
                p.destroy();
                try {
                    if (!p.waitFor(3, java.util.concurrent.TimeUnit.SECONDS)) {
                        p.destroyForcibly();
                    }
                } catch (InterruptedException e) {
                    p.destroyForcibly();
                    Thread.currentThread().interrupt();
                }
            }
        }
        processes.clear();
    }
}
