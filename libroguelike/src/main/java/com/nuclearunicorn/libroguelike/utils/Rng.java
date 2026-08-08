package com.nuclearunicorn.libroguelike.utils;

import java.util.Random;

/**
 * The one source of game randomness, so a session can be reproduced from a single number.
 *
 * <p>Replays used to be "comparable, not identical": world generation is seeded per chunk
 * origin, but name rolls, roaming, combat and body simulation all went through
 * {@code Math.random()} or a bare {@code new Random()}. That is not a cosmetic difference —
 * replaying an attack scenario, whether the blow landed at all depended on whether the
 * target happened to drift a tile that run, so the same file reproduced the bug roughly
 * half the time.
 *
 * <p>{@code Math.random()} cannot be seeded (it hides a static {@code Random} inside
 * {@link Math}), so every gameplay roll has to come through here instead. The seed is
 * chosen once at startup, written into the replay header, and read back on playback.
 *
 * <p><b>Scope.</b> This makes the game thread deterministic. LLM sampling is not covered —
 * see {@code LlamaHttpInferenceService}, which seeds each request but cannot control the
 * order in which asynchronous replies arrive.
 */
public final class Rng {

    private static Random random;
    private static long seed;

    private Rng() {}

    /** Fix the sequence. Call before anything generates a world. */
    public static synchronized void seed(long value) {
        seed = value;
        random = new Random(value);
    }

    /** The seed in force, so it can be recorded and replayed. */
    public static synchronized long seed() {
        rnd();
        return seed;
    }

    /** Drop-in for {@code Math.random()}. */
    public static synchronized double random() {
        return rnd().nextDouble();
    }

    /** Uniform in [0, bound). Tolerates bound &lt;= 0, which the Math.random idiom did. */
    public static synchronized int nextInt(int bound) {
        return bound <= 0 ? 0 : rnd().nextInt(bound);
    }

    /**
     * A private stream for a class that holds its own {@code Random}. Derived from the
     * session seed, so it is reproducible as long as the deriving order is.
     */
    public static synchronized Random derive() {
        return new Random(rnd().nextLong());
    }

    private static Random rnd() {
        if (random == null) {
            seed(System.nanoTime());   // unseeded run: still pick one, so it can be reported
        }
        return random;
    }
}
