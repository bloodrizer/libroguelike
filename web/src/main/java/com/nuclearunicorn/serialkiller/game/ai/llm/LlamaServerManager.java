package com.nuclearunicorn.serialkiller.game.ai.llm;

/**
 * Web build: a browser tab cannot spawn a llama-server process. Reporting the
 * binary as unavailable makes LoadingMode take its existing "no local model"
 * branch rather than needing a web-specific code path.
 */
public class LlamaServerManager {

    private static final String REASON = "no local inference server in the browser build";

    public LlamaServerManager(String serverBinary) {
    }

    public boolean startTier(LlmConfig.Tier tier) {
        return false;
    }

    public String getLastError() {
        return REASON;
    }

    public static boolean isBinaryAvailable(String binary) {
        return false;
    }

    public void shutdown() {
    }
}
