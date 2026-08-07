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
        String line = args.length == 0 ? fmt : String.format(fmt, args);
        // Always mirror into a replay, even when stdout tracing is off: the recording is
        // the artifact you read afterwards, and a replay missing the reasoning is useless.
        com.nuclearunicorn.libroguelike.core.replay.Replay.trace(line);
        if (!enabled) {
            return;
        }
        System.out.println("[LLM] " + line);
    }
}
