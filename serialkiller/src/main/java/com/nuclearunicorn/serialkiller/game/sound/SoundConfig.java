package com.nuclearunicorn.serialkiller.game.sound;

/**
 * Every tunable the acoustic model has (SOUND_DESIGN.md 8).
 *
 * <p>Units are "dB-ish" on a 0-120 scale — not calibrated against anything real, but
 * ordered and spaced like decibels so the numbers stay intuitive. Everything is an
 * {@code int} on purpose: the flood in {@link Acoustics} is a bucket-queue Dijkstra
 * whose priority key is exactly this quantity, and integer keys are what make the
 * bucket queue legal (and TeaVM-cheap).
 */
public final class SoundConfig {

    private SoundConfig() {}

    // ------------------------------------------------------------- spreading

    /** dB lost per cardinal step of open air. */
    public static int AIR_CARD = 2;

    /** dB lost per diagonal step. 2/3 chamfer metric: within ~4% of Euclidean, integral. */
    public static int AIR_DIAG = 3;

    /**
     * Global cutoff. Nothing below this is audible to anybody, so the flood stops here
     * and a sound quieter than this never allocates at all.
     */
    public static int FLOOR = 12;

    /** Just-noticeable difference: a sound must beat the local ambient by this much. */
    public static int JND = 3;

    /** Received level at or above which speech counts as addressed rather than overheard. */
    public static int DIRECTED_LEVEL = 26;

    // ----------------------------------------------- transmission loss, 4.1

    public static int TL_OPEN = 0;
    public static int TL_PROP = 4;
    public static int TL_TREE = 3;
    public static int TL_WINDOW = 14;
    public static int TL_WINDOW_BROKEN = 2;
    public static int TL_DOOR_OPEN = 1;

    /**
     * A shut but unlocked door.
     *
     * <p>Set by the eavesdrop case in SOUND_DESIGN.md 4.5, not picked for roundness:
     * speaker and listener flanking a shut door gives {@code 32 - (2+14) - 2 = 14},
     * which clears the threshold of 12, and one tile further back gives exactly 12,
     * which does not. At 16 the near case computes to 12 and ear-to-the-door stops
     * working at all. Recheck {@code AcousticsTest.eavesdropThroughShutDoor} before
     * changing this.
     */
    public static int TL_DOOR_SHUT = 14;

    public static int TL_DOOR_LOCKED = 22;
    public static int TL_WALL_INNER = 32;

    /** The primary tuning knob: how private a building is. */
    public static int TL_WALL_OUTER = 46;

    // -------------------------------------------------- listener thresholds

    public static int HEAR_BASE = 12;
    public static int HEAR_ASLEEP = 16;
    public static int HEAR_STUNNED = 8;
    // SOUND_DESIGN.md also lists a "mid-conversation" penalty. Left out rather than left
    // dead: nothing in the AI currently exposes "is talking right now" without reaching
    // into the LLM transcript, and a constant nobody reads is worse than a missing one.

    // ------------------------------------------------------- ambient, 4.4

    public static int AMB_STREET_DAY = 22;
    public static int AMB_STREET_NIGHT = 8;
    public static int AMB_PARK_DAY = 6;
    public static int AMB_PARK_NIGHT = 4;
    public static int AMB_INDOOR_DAY = 10;
    public static int AMB_INDOOR_NIGHT = 4;
    public static int AMB_PUBLIC_DAY = 26;
    public static int AMB_PUBLIC_NIGHT = 6;
    public static int AMB_BROTHEL = 30;
    public static int AMB_SEALED = 4;
}
