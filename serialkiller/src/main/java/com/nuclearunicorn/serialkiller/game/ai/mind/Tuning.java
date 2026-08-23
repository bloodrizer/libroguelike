package com.nuclearunicorn.serialkiller.game.ai.mind;

import com.nuclearunicorn.serialkiller.game.ai.llm.LlmConfig;
import com.nuclearunicorn.serialkiller.game.ai.llm.LlmRuntime;

/**
 * The behaviour numbers, in one place, readable whether or not the LLM ever booted.
 *
 * <p>Reflexes are the same reflexes with the model off — a pedestrian still runs, a
 * policeman still gives chase — so reaching into {@code LlmRuntime.config()} from a
 * behaviour would tie the FSM path to a subsystem it does not use, and that field is null
 * until something initialises the runtime. {@code peekConfig()} always returns a config.
 */
public final class Tuning {

    private Tuning() {}

    public static LlmConfig.Priority priority() {
        return LlmRuntime.peekConfig().priority;
    }

    public static LlmConfig.Speech speech() {
        return LlmRuntime.peekConfig().speech;
    }

    public static LlmConfig.Memory memory() {
        return LlmRuntime.peekConfig().memory;
    }

    public static LlmConfig.Tier director() {
        return LlmRuntime.peekConfig().director;
    }
}
