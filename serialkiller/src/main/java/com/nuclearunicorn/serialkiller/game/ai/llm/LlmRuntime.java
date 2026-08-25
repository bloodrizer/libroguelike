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
    private static InferenceService director;
    private static LlamaServerManager serverManager;
    private static volatile String degradedReason;

    private LlmRuntime() {}

    public static synchronized void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        config = peekConfig();
        if (!config.enabled) {
            LlmDebug.log("disabled (llm.enabled=false) — NPCs use the FSM");
            return;
        }
        LlmDebug.log("enabled; booting reactor tier (binary=%s, model=%s, port=%d)",
                config.serverBinary, config.reactor.model, config.reactor.port);

        registry = new CommandRegistry();

        serverManager = new LlamaServerManager(config.serverBinary);
        boolean ready = serverManager.startTier(config.reactor, "reactor");

        if (ready) {
            LlmDebug.log("reactor server healthy on port %d — using live inference", config.reactor.port);
            reactor = new LlamaHttpInferenceService(
                    config.reactor.port, registry.getGrammar(), config.reactor.maxTokens,
                    config.reactor.queueCapacity);
        } else {
            degradedReason = serverManager.getLastError();
            System.err.println("LlmRuntime: reactor server unavailable (" + degradedReason
                    + "), using stub inference");
            LlmDebug.log("reactor server UNAVAILABLE — falling back to canned StubInferenceService");
            reactor = new StubInferenceService();
        }

        bootDirector();
    }

    /**
     * The long-term / reflection tier (M2). A second llama-server on its own port, free-text
     * (no command grammar — reflections are prose, not programs). Failure degrades silently:
     * the reactor keeps working, and {@link #director()} stays null so reflection is skipped.
     */
    private static void bootDirector() {
        if (config.director == null || config.director.model == null
                || config.director.model.isEmpty()) {
            return;   // no director configured: reactor-only
        }

        // Both tiers on the same GGUF is the ordinary case now that one model is good enough
        // for both jobs, and a second llama-server for it is a second full copy of the weights
        // in RAM and a second cold load before the game opens. The tiers differ in cadence,
        // token budget and grammar - all per-request - so one server serves both.
        if (sharesReactorModel()) {
            if (reactor instanceof LlamaHttpInferenceService) {
                director = new LlamaHttpInferenceService(
                        config.reactor.port, null, config.director.maxTokens,
                        config.director.queueCapacity);
                LlmDebug.log("director shares the reactor server on port %d (same model %s)",
                        config.reactor.port, config.director.model);
            } else {
                LlmDebug.log("director shares the reactor tier, which is not live - reflection disabled");
            }
            return;
        }

        LlmDebug.log("booting director tier (model=%s, port=%d, threads=%d)",
                config.director.model, config.director.port, config.director.threads);
        if (serverManager.startTier(config.director, "director")) {
            director = new LlamaHttpInferenceService(
                    config.director.port, null, config.director.maxTokens,
                    config.director.queueCapacity);
            LlmDebug.log("director server healthy on port %d", config.director.port);
        } else {
            System.err.println("LlmRuntime: director server unavailable ("
                    + serverManager.getLastError() + "), reflection disabled");
            LlmDebug.log("director server UNAVAILABLE — reflection disabled");
        }
    }

    /** Same weights as the reactor, so a server of its own would buy nothing. */
    private static boolean sharesReactorModel() {
        return config.reactor != null && config.director.model.equals(config.reactor.model);
    }

    /**
     * Why NPCs are on canned replies instead of live inference, or null when inference is
     * live. Surfaced by the loading screen — a silent fallback reads like a config that
     * simply didn't take.
     */
    public static String degradedReason() {
        return degradedReason;
    }

    /** Config without booting anything — the loading screen needs it to stage models first. */
    public static synchronized LlmConfig peekConfig() {
        if (config == null) {
            config = LlmConfig.load();
            LlmDebug.setEnabled(config.debug);
            LlmDebug.setPromptsEnabled(config.debugPrompts);
        }
        return config;
    }

    /**
     * Give up on the LLM tiers for this session (models missing, staging failed). Marks the
     * runtime initialized so nothing later blocks for a minute waiting on a server that
     * cannot start — NPCs just run the FSM.
     */
    public static synchronized void disable(String reason) {
        peekConfig();
        config.enabled = false;
        initialized = true;
        LlmDebug.log("LLM disabled for this session: %s", reason);
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

    /** The reflection tier, or null when it did not boot or is not configured. */
    public static InferenceService director() {
        return director;
    }
}
