package com.nuclearunicorn.serialkiller.game.ai.llm.sense;

/**
 * The one priority scale the whole LLM pipeline resolves signals on (LLM_NPC_SPEC.md §7-9).
 *
 * <p>Before this existed, perception was a flat list of strings and there was no way to
 * express "this outranks whatever you are doing". Salience is used by three layers, and it
 * has to be the same number in all of them or the priority inverts somewhere:
 *
 * <ol>
 *   <li><b>Memory</b> — eviction drops the <i>least</i> salient, not the oldest, so
 *       ambient churn can never push out a line spoken to your face.</li>
 *   <li><b>Queue</b> — {@code LlamaHttpInferenceService} serves highest-salience first, so
 *       a conversation partner jumps the backlog of wandering pedestrians.</li>
 *   <li><b>Trigger</b> — anything at or above {@code interruptAt} preempts the running
 *       plan instead of waiting for it to drain.</li>
 * </ol>
 *
 * Bands are spaced so recency decay can move a stimulus down a band over a plausible number
 * of turns without ever letting AMBIENT overtake DIRECTED.
 */
public final class Salience {

    /** Background churn: FOV deltas, chunk changes. Never worth an interrupt. */
    public static final int AMBIENT = 10;

    /** Worth knowing: speech overheard at a distance, a crime seen nearby. */
    public static final int NOTABLE = 40;

    /** Someone addressed <i>you</i>. Preempts the current plan. */
    public static final int DIRECTED = 70;

    /** Attacked, or witnessed a murder. Preempts everything and cancels in-flight work. */
    public static final int URGENT = 95;

    private Salience() {}

    /** Band name for prompts and debug lines. */
    public static String label(int salience) {
        if (salience >= URGENT) {
            return "URGENT";
        }
        if (salience >= DIRECTED) {
            return "DIRECTED";
        }
        if (salience >= NOTABLE) {
            return "NOTABLE";
        }
        return "AMBIENT";
    }
}
