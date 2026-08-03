package com.nuclearunicorn.serialkiller.game.ai.llm;

/**
 * One-line tracer for the LLM-NPC pipeline. Gated by {@code llm.debug} in config so it is
 * silent in normal play. Prints to stdout with a fixed {@code [LLM]} prefix so the whole
 * reactor→interpreter→action chain can be followed in the game log.
 */
public final class LlmDebug {

    private static volatile boolean enabled = false;

    private LlmDebug() {}

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void log(String fmt, Object... args) {
        if (!enabled) {
            return;
        }
        System.out.println("[LLM] " + (args.length == 0 ? fmt : String.format(fmt, args)));
    }
}
