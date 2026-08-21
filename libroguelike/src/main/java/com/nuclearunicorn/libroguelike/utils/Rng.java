package com.nuclearunicorn.libroguelike.utils;

import java.util.HashMap;
import java.util.Map;
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
 * <h3>Streams</h3>
 *
 * <p>One seed is not the same as one sequence. With a single shared {@link Random}, every
 * roll depends on how many rolls came before it — so changing where a crate is placed
 * reshuffles the names, the combat and the AI too, and the same seed stops meaning "the
 * same town". That makes a before/after comparison across a code change worthless, which is
 * exactly when you most want one.
 *
 * <p>So randomness is split into independent named streams. A stream's sequence is a
 * function of {@code (seed, name)} alone — not of what any other stream has consumed, and
 * not of the order in which streams are first touched. Change the town generator and the
 * combat rolls stay put.
 *
 * <pre>
 *   Rng.nextInt(Rng.WORLDGEN, 10)   // layout, furniture, decoration
 *   Rng.random(Rng.AI)              // what an NPC decides to do
 *   Rng.derive(Rng.NAMES)           // a private Random off the names stream
 * </pre>
 *
 * <p><b>Scope.</b> This makes the game thread deterministic. LLM sampling is not covered —
 * see {@code LlamaHttpInferenceService}, which seeds each request but cannot control the
 * order in which asynchronous replies arrive.
 */
public final class Rng {

    /** Town layout, buildings, furniture, terrain — anything the generators roll. */
    public static final String WORLDGEN = "worldgen";
    /** Person and place names. */
    public static final String NAMES = "names";
    /** What an NPC decides to do: roaming, patrol targets, dithering, dialogue picks. */
    public static final String AI = "ai";
    /** Combat, stats and the body simulation. */
    public static final String COMBAT = "combat";
    /** Ambient world events on the clock — weather, spawns, the passage of time. */
    public static final String WORLD = "world";
    /** Who the player is: preset choice, starting sex, age and kit. */
    public static final String CHARACTER = "character";
    /** Anything not yet assigned a stream of its own. */
    public static final String DEFAULT = "default";

    private static final Map<String, Random> streams = new HashMap<String, Random>();
    private static long seed;
    private static boolean seeded;

    private Rng() {}

    /** Fix the sequence. Call before anything generates a world. */
    public static synchronized void seed(long value) {
        seed = value;
        seeded = true;
        streams.clear();
    }

    /** The seed in force, so it can be recorded and replayed. */
    public static synchronized long seed() {
        ensureSeeded();
        return seed;
    }

    /**
     * The named stream, created on first use. Independent of every other stream: two runs
     * with the same seed produce the same sequence here however much the rest of the game
     * has rolled in between.
     */
    public static synchronized Random stream(String name) {
        ensureSeeded();
        Random random = streams.get(name);
        if (random == null) {
            random = new Random(mix(seed, name));
            streams.put(name, random);
        }
        return random;
    }

    /** Drop-in for {@code Math.random()} on the default stream. */
    public static synchronized double random() {
        return random(DEFAULT);
    }

    public static synchronized double random(String stream) {
        return stream(stream).nextDouble();
    }

    /** Uniform in [0, bound). Tolerates bound &lt;= 0, which the Math.random idiom did. */
    public static synchronized int nextInt(int bound) {
        return nextInt(DEFAULT, bound);
    }

    public static synchronized int nextInt(String stream, int bound) {
        return bound <= 0 ? 0 : stream(stream).nextInt(bound);
    }

    /**
     * A private stream for a class that holds its own {@code Random} — one per generator,
     * per NPC, per chunk. Derived from the named stream, so a class that derives a dozen of
     * these perturbs only its own neighbourhood.
     */
    public static synchronized Random derive(String stream) {
        return new Random(stream(stream).nextLong());
    }

    /** @deprecated name the stream — see {@link #derive(String)}. */
    @Deprecated
    public static synchronized Random derive() {
        return derive(DEFAULT);
    }

    /**
     * A seed for a chunk generator: a function of the session seed and the chunk's own
     * coordinates, and of nothing else. Regenerating a chunk therefore reproduces it exactly,
     * whatever else has happened in between — that part the generators already relied on.
     *
     * <p>What they were missing is the session seed. {@code seed = x*10000 + y} made the town
     * a pure function of the map coordinates, so every seed produced the same town and
     * {@code --seed} varied nothing at all.
     */
    public static synchronized long chunkSeed(String stream, int x, int y) {
        ensureSeeded();
        return mix(seed, stream) ^ (x * 0x9E3779B97F4A7C15L) ^ (y * 0xC2B2AE3D27D4EB4FL);
    }

    /**
     * FNV-1a over the stream name, xored into the seed. Any mixing function would do; what
     * matters is that it depends on the name and not on call order.
     */
    private static long mix(long seed, String name) {
        long hash = 0xcbf29ce484222325L;
        for (int i = 0; i < name.length(); i++) {
            hash ^= name.charAt(i);
            hash *= 0x100000001b3L;
        }
        return seed ^ hash;
    }

    private static void ensureSeeded() {
        if (!seeded) {
            seed(System.nanoTime());   // unseeded run: still pick one, so it can be reported
        }
    }
}
