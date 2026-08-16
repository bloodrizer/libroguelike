package com.nuclearunicorn.serialkiller.game.ai.llm;

/**
 * Web build: there is no filesystem to read a config from, and no local
 * llama-server to point one at, so the disabled default is the honest answer.
 * NPCs run the FSM brain — the same path the desktop takes without a model.
 */
final class LlmConfigLoader {

    private LlmConfigLoader() {
    }

    static LlmConfig read() {
        return new LlmConfig();
    }
}
