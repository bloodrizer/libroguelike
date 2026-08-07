package com.nuclearunicorn.serialkiller.game.ai.llm.sense;

/**
 * The world turn counter, advanced once per {@code InGameMode.makeTurn()}.
 *
 * <p>The LLM loop used to gate itself on {@code Timer.get_time()} — wall-clock milliseconds
 * — in a game where nothing moves until the player acts. That made cadence unrelated to
 * anything happening in the world: standing still burned the clock, and holding shift
 * outran it. Stimulus age, decay and cadence are all measured in turns instead.
 */
public final class GameTurn {

    private static volatile long turn = 0;

    private GameTurn() {}

    public static void advance() {
        turn++;
    }

    public static long current() {
        return turn;
    }
}
