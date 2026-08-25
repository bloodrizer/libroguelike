package com.nuclearunicorn.serialkiller.game.ai.llm;

/**
 * Web build: kept only so the type referenced by Deliberation's priority probe
 * still resolves. Never constructed — LlmRuntime falls back to
 * {@link StubInferenceService} because no server is reachable.
 */
public class LlamaHttpInferenceService implements InferenceService {

    public LlamaHttpInferenceService(int port, String grammar, int maxTokens, int queueCapacity) {
    }

    @Override
    public void submit(String uid, String prompt, int priority) {
    }

    @Override
    public boolean isBusy(String uid) {
        return false;
    }

    public int pendingPriority(String uid) {
        return 0;
    }

    // Debug-overlay counters. Nothing is ever queued here, so they read as an idle service.

    public int queueDepth() {
        return 0;
    }

    public int inFlightCount() {
        return 0;
    }

    public long lastLatencyMs() {
        return 0;
    }

    @Override
    public String poll(String uid) {
        return null;
    }

    @Override
    public void shutdown() {
    }
}
