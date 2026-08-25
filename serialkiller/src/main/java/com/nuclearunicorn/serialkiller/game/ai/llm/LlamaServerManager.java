package com.nuclearunicorn.serialkiller.game.ai.llm;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Spawns and tears down {@code llama-server} processes for the game (§10). One instance
 * per tier. The game owns the process lifetime: started when LLM NPCs boot, killed by a
 * JVM shutdown hook so no orphan server survives the game.
 *
 * <h3>Why the server's own output is kept</h3>
 * It used to go to {@code Redirect.DISCARD} and the boot was a fixed 60-second poll of
 * {@code /health}. Between those two decisions, every possible failure looked identical from
 * the outside — a minute of nothing, then "never became healthy" — including the ones the
 * server had already explained in one line and exited over. A backend package that stopped
 * being a dependency of llama.cpp is diagnosed in the first 200ms of its log and cost two
 * silent minutes at every launch instead. So: the log is kept, the last line of it is what
 * the loading screen shows, and a process that has exited is noticed immediately rather than
 * waited out.
 */
public class LlamaServerManager {

    private final String serverBinary;
    private String lastError;
    private final List<Process> processes = new ArrayList<>();
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(1))
            .build();

    /** Where the servers' logs go. Beside the game, so a bug report can just attach them. */
    private static final File LOG_DIR = new File("logs");

    // The tier currently coming up. Only one boots at a time, and the loading screen is the
    // only reader, so a handful of volatiles beats threading a progress object through.
    private static volatile String bootTier;
    private static volatile String bootStage = "";
    private static volatile String bootDetail = "";
    private static volatile String bootLog = "";
    private static volatile long bootStartedAt;

    public LlamaServerManager(String serverBinary) {
        this.serverBinary = serverBinary;
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown, "llama-shutdown"));
    }

    /** Which tier is coming up, or null when nothing is. */
    public static String bootTier() {
        return bootTier;
    }

    /** Coarse phase: "starting", "loading model", "ready", "failed". */
    public static String bootStage() {
        return bootStage;
    }

    /** The server's own last word — the line that says what it is actually doing. */
    public static String bootDetail() {
        return bootDetail;
    }

    /** Path of the log file the current tier is writing, for the "see also" line. */
    public static String bootLog() {
        return bootLog;
    }

    public static int bootElapsedSeconds() {
        return bootStartedAt == 0 ? 0
                : (int) ((System.currentTimeMillis() - bootStartedAt) / 1000);
    }

    /**
     * Start one server for a tier and block until {@code /health} reports ready.
     * Returns false if the binary can't launch, dies, or never becomes healthy — the caller
     * then degrades that tier (§10, §12).
     */
    public boolean startTier(LlmConfig.Tier tier, String label) {
        bootTier = label;
        bootStage = "starting";
        bootDetail = "";
        bootStartedAt = System.currentTimeMillis();

        File model = new File(tier.model);
        if (!model.isFile()) {
            return fail("model file missing: " + tier.model);
        }

        List<String> cmd = new ArrayList<String>();
        cmd.add(serverBinary);
        cmd.add("-m");
        cmd.add(tier.model);
        cmd.add("--port");
        cmd.add(Integer.toString(tier.port));
        cmd.add("--host");
        cmd.add("127.0.0.1");
        if (tier.threads > 0) {
            cmd.add("-t");
            cmd.add(Integer.toString(tier.threads));
        }
        if (tier.contextSize > 0) {
            cmd.add("-c");
            cmd.add(Integer.toString(tier.contextSize));
        }
        if (tier.gpuLayers > 0) {
            cmd.add("-ngl");
            cmd.add(Integer.toString(tier.gpuLayers));
        }
        LlmDebug.log("%s: exec %s", label, String.join(" ", cmd));

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);

        Process process;
        try {
            process = pb.start();
            processes.add(process);
        } catch (IOException e) {
            //plain ASCII: this reaches the loading overlay, whose font is Latin-1 only
            System.err.println("Failed to start llama-server (" + serverBinary + "): " + e);
            return fail("'" + serverBinary + "' not found - install llama.cpp");
        }

        Tail tail = new Tail(process, label, tier.port);
        bootLog = tail.logPath();
        bootStage = "loading model (" + humanSize(model.length()) + ")";

        int timeout = tier.startupTimeoutSeconds > 0 ? tier.startupTimeoutSeconds : 180;
        if (awaitHealth(process, tier.port, timeout, tail)) {
            bootStage = "ready";
            bootTier = null;
            return true;
        }
        if (!process.isAlive()) {
            //the server said why on its way out; that is a far better error than a timeout
            return fail(tail.diagnosis("llama-server exited (code " + process.exitValue() + ")"));
        }
        return fail("'" + serverBinary + "' not ready after " + timeout + "s on port " + tier.port);
    }

    private boolean fail(String reason) {
        lastError = reason;
        bootStage = "failed";
        bootDetail = reason;
        bootTier = null;
        System.err.println("llama-server: " + reason);
        LlmDebug.log("llama-server FAILED: %s", reason);
        return false;
    }

    /** Why the last {@link #startTier} failed, or null if it didn't. */
    public String getLastError() {
        return lastError;
    }

    /**
     * True if the configured binary is runnable — a bare name is looked up on PATH, the way
     * ProcessBuilder will. Lets the loading screen warn before it spends a download on it.
     */
    public static boolean isBinaryAvailable(String binary) {
        if (binary == null || binary.isEmpty()) {
            return false;
        }
        if (binary.contains(File.separator)) {
            return new File(binary).canExecute();
        }
        String path = System.getenv("PATH");
        if (path == null) {
            return false;
        }
        for (String dir : path.split(File.pathSeparator)) {
            if (new File(dir, binary).canExecute()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Poll {@code /health} until ready, the timeout elapses, or the process exits. The
     * exit check is the point: a server that cannot load its model is gone in a fraction
     * of a second, and there is nothing to wait for after that.
     */
    private boolean awaitHealth(Process process, int port, int timeoutSeconds, Tail tail) {
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
            bootDetail = tail.last();
            if (!process.isAlive()) {
                return false;
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

    private static String humanSize(long bytes) {
        return bytes >= 1L << 30 ? String.format("%.1fGB", bytes / (double) (1L << 30))
                                 : (bytes >> 20) + "MB";
    }

    /**
     * Drains one server's output to a log file on a daemon thread, keeping the last line for
     * the loading screen and the last few error lines for the failure message.
     */
    private static final class Tail {

        private static final int ERRORS_KEPT = 4;

        private final File log;
        private final Deque<String> errors = new ArrayDeque<String>();
        private volatile String last = "";

        Tail(Process process, String label, int port) {
            LOG_DIR.mkdirs();
            this.log = new File(LOG_DIR, "llama-" + label + "-" + port + ".log");

            Thread reader = new Thread(() -> pump(process), "llama-log-" + port);
            reader.setDaemon(true);
            reader.start();
        }

        String logPath() {
            return log.getPath();
        }

        String last() {
            return last;
        }

        /**
         * The failure message: the <i>first</i> thing the server complained about, plus a
         * hint where we recognise it. llama.cpp reports a failure as a cascade — the root
         * cause, then three layers of "failed to load model" wrapping it — so the last line
         * is always the least informative one, and the first is the one worth printing.
         */
        synchronized String diagnosis(String fallback) {
            if (errors.isEmpty()) {
                return fallback + " - see " + log.getPath();
            }
            String root = errors.getFirst();
            String hint = hint(root);
            return root + (hint == null ? "" : " -> " + hint) + " (see " + log.getPath() + ")";
        }

        /** What to actually do about the llama.cpp failures that are not self-explanatory. */
        private static String hint(String error) {
            if (error.contains("no backends are loaded")) {
                return "llama.cpp has no compute backend installed"
                        + " (Arch/Manjaro: pacman -S ggml-cpu, or ggml-vulkan / ggml-cuda for GPU)";
            }
            if (error.contains("unknown model architecture")
                    || error.contains("unsupported model")) {
                return "the GGUF is newer than this llama.cpp - update llama.cpp";
            }
            if (error.contains("failed to allocate") || error.contains("out of memory")) {
                return "not enough memory - lower \"contextSize\" or \"gpuLayers\" in llm-config.json";
            }
            if (error.contains("bind") || error.contains("Address already in use")) {
                return "another llama-server already holds that port";
            }
            return null;
        }

        private void pump(Process process) {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(
                         process.getInputStream(), StandardCharsets.UTF_8));
                 PrintWriter out = new PrintWriter(log, "UTF-8")) {
                String line;
                while ((line = in.readLine()) != null) {
                    out.println(line);
                    out.flush();
                    String clean = clean(line);
                    if (!clean.isEmpty()) {
                        last = clean;
                    }
                    if (isError(line)) {
                        synchronized (this) {
                            //keep the first few, not the last: the root cause comes first
                            if (errors.size() < ERRORS_KEPT) {
                                errors.addLast(clean);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                LlmDebug.log("llama log reader stopped: %s", e);
            }
        }

        /** llama.cpp's level marker, however it is spelled across builds. */
        private static boolean isError(String line) {
            return line.contains(" E ") || line.contains("error") || line.contains("failed");
        }

        /**
         * Strip llama.cpp's {@code 0.00.115.601 E } prefix and anything the loading screen's
         * Latin-1 font cannot draw, then cut to something that fits on one line.
         */
        private static String clean(String line) {
            String text = line.trim();
            int cut = 0;
            while (cut < text.length() && (Character.isDigit(text.charAt(cut))
                    || text.charAt(cut) == '.')) {
                cut++;
            }
            if (cut > 6 && cut < text.length()) {
                text = text.substring(cut).trim();
                if (text.length() > 2 && text.charAt(1) == ' '
                        && "IWED".indexOf(text.charAt(0)) >= 0) {
                    text = text.substring(2).trim();
                }
            }
            StringBuilder sb = new StringBuilder(text.length());
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                sb.append(c >= ' ' && c < 0x100 ? c : ' ');
            }
            text = sb.toString().trim();
            return text.length() > 88 ? text.substring(0, 88) + "..." : text;
        }
    }
}
