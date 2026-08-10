package com.nuclearunicorn.libroguelike.core.replay;

/**
 * Direct world manipulation from inside a replay file, so a scenario can state its
 * preconditions instead of walking to them.
 *
 * <p>This is the difference between "attack the person who lives with you" as a test and as
 * an errand. Done through the keyboard it means solving the house as a maze, finding out
 * where the other person wandered to, and hoping the swing connects; the last attempt to
 * script one took six runs and never landed a blow. Done as {@code tp} then {@code hurt} it
 * is two lines and it happens every time.
 *
 * <p><b>Only during playback.</b> A live session ignores these records entirely — the game
 * has no console and this is not one. What makes that safe is not the file format but
 * {@link Replay#isPlaybackSession()}: nothing here can run unless a harness is already
 * driving the session from a file.
 *
 * <p>The verbs are game-specific (what is a "policeman"?), so the game registers the handler
 * and the engine only carries the plumbing.
 */
public interface Scenario {

    /**
     * Run one command. Return false for a verb this handler does not know, so the player can
     * say so rather than failing silently — a typo in a scenario should not look like a bug
     * in the thing under test.
     */
    boolean run(String verb, String[] args);

    /** The handler for this session, or null when nothing has registered one. */
    static Scenario handler() {
        return Holder.handler;
    }

    static void register(Scenario handler) {
        Holder.handler = handler;
    }

    /** Interfaces cannot hold mutable state; this is the smallest place to put it. */
    final class Holder {
        private static Scenario handler;
        private Holder() {}
    }
}
