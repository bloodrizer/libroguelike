package com.nuclearunicorn.serialkiller.game.ai.llm;

/**
 * Web build: a browser tab cannot spawn a llama-server process. Reporting the
 * binary as unavailable makes LoadingMode take its existing "no local model"
 * branch rather than needing a web-specific code path.
 *
 * <p>The boot-progress statics exist only so the shared loading screen compiles;
 * nothing here ever boots, so they stay at their "idle" values.
 */
public class LlamaServerManager {

    private static final String REASON = "no local inference server in the browser build";

    public LlamaServerManager(String serverBinary) {
    }

    public boolean startTier(LlmConfig.Tier tier, String label) {
        return false;
    }

    public String getLastError() {
        return REASON;
    }

    public static boolean isBinaryAvailable(String binary) {
        return false;
    }

    public static String bootTier() {
        return null;
    }

    public static String bootStage() {
        return "";
    }

    public static String bootDetail() {
        return "";
    }

    public static String bootLog() {
        return "";
    }

    public static int bootElapsedSeconds() {
        return 0;
    }

    public void shutdown() {
    }
}
