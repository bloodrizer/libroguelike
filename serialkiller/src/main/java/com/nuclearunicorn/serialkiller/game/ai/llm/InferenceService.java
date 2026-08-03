package com.nuclearunicorn.serialkiller.game.ai.llm;

/**
 * The only backend-specific seam (LLM_NPC_SPEC.md §11). The game thread submits a
 * prompt for a uid and polls for the raw completion on a later tick — never blocks.
 * A WASM build later provides an alternate implementation; everything above this
 * interface stays unchanged.
 */
public interface InferenceService {

    /** Queue a prompt for this uid. Ignored if a request for the uid is already in flight. */
    void submit(String uid, String prompt);

    /** A request for this uid is queued or running. */
    boolean isBusy(String uid);

    /**
     * Non-blocking. Returns the raw model completion (a JSON command array as a String)
     * once ready, then clears it; returns null while pending or on failure.
     */
    String poll(String uid);

    void shutdown();
}
