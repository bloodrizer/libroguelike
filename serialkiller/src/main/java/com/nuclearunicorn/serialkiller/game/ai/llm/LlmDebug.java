package com.nuclearunicorn.serialkiller.game.ai.llm;

/**
 * One-line tracer for the LLM-NPC pipeline. Gated by {@code llm.debug} in config so it is
 * silent in normal play. Prints to stdout with a fixed {@code [LLM]} prefix so the whole
 * reactor→interpreter→action chain can be followed in the game log.
 */
public final class LlmDebug {

    private static volatile boolean enabled = false;
    private static volatile boolean prompts = false;

    private LlmDebug() {}

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    /** {@code llm.debugPrompts}: mirror every submitted prompt in full. See {@link #prompt}. */
    public static void setPromptsEnabled(boolean value) {
        prompts = value;
    }

    public static boolean isPromptsEnabled() {
        return prompts;
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

    /**
     * The exact prompt an NPC was given, verbatim, delimited so it can be lifted back out of
     * a replay whole.
     *
     * <p>Off by default and worth the switch. Submits used to be logged as {@code prompt 1109
     * chars} and nothing else, so working out why an NPC said something meant rebuilding the
     * prompt by hand from the builder's source and checking the reconstruction against that
     * character count. The one that started this — an NPC who could not name her own husband
     * — took an hour to find that way and would have taken a minute with the text in hand.
     */
    public static void prompt(String uid, String text) {
        if (!prompts) {
            return;
        }
        log("---- prompt for %s (%d chars) ----\n%s---- end prompt ----",
                uid, text.length(), text.endsWith("\n") ? text : text + "\n");
    }
}
