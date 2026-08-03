package com.nuclearunicorn.serialkiller.game.ai.llm;

import com.nuclearunicorn.serialkiller.game.ai.llm.command.CommandRegistry;

/**
 * Process-wide LLM services shared by every LLM NPC: config, the command registry, and
 * the reactor inference service (M1 is reactor-only — director + second server land in
 * M2). Booted lazily on first access so the game runs untouched when disabled.
 *
 * <p>If {@code llm.enabled} is true but the server can't start, the runtime falls back
 * to a {@link StubInferenceService} so the loop still exercises end-to-end (§10, §13).
 */
public final class LlmRuntime {

    private static boolean initialized = false;
    private static LlmConfig config;
    private static CommandRegistry registry;
    private static InferenceService reactor;
    private static LlamaServerManager serverManager;

    private LlmRuntime() {}

    public static synchronized void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        config = LlmConfig.load();
        LlmDebug.setEnabled(config.debug);
        if (!config.enabled) {
            LlmDebug.log("disabled (llm.enabled=false) — NPCs use the FSM");
            return;
        }
        LlmDebug.log("enabled; booting reactor tier (binary=%s, model=%s, port=%d)",
                config.serverBinary, config.reactor.model, config.reactor.port);

        registry = new CommandRegistry();

        serverManager = new LlamaServerManager(config.serverBinary);
        boolean ready = serverManager.startTier(config.reactor);

        if (ready) {
            LlmDebug.log("reactor server healthy on port %d — using live inference", config.reactor.port);
            reactor = new LlamaHttpInferenceService(
                    config.reactor.port, registry.getGrammar(), config.reactor.maxTokens);
        } else {
            System.err.println("LlmRuntime: reactor server unavailable, using stub inference");
            LlmDebug.log("reactor server UNAVAILABLE — falling back to canned StubInferenceService");
            reactor = new StubInferenceService();
        }
    }

    public static boolean isEnabled() {
        if (!initialized) {
            init();
        }
        return config != null && config.enabled;
    }

    public static LlmConfig config() {
        return config;
    }

    public static CommandRegistry registry() {
        return registry;
    }

    public static InferenceService reactor() {
        return reactor;
    }
}
