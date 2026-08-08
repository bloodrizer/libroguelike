package com.nuclearunicorn.libroguelike.core.replay;

import com.nuclearunicorn.libroguelike.utils.Rng;

/**
 * Deterministic-input replay: record a session's player input to a file, then drive a later
 * session from that file instead of the keyboard.
 *
 * <p>It exists because the interesting bugs in this game are in the NPC brains, and those
 * are asynchronous, model-driven and several turns deep — "walk up to the NPC in the
 * starting building, say something, hit it" is not something a unit test can express and
 * not something a human can repeat identically twice. A replay makes one session an
 * artifact that a test harness (or an LLM reading the log) can run and inspect.
 *
 * <p>The file is JSONL, and carries two kinds of record:
 * <ul>
 *   <li><b>input</b> — the only thing played back. Key, character and modifier state per
 *       frame. Modifiers matter: attack is ctrl+direction, so a replay that dropped
 *       {@code key_state_ctrl} would silently turn every attack into a walk.</li>
 *   <li><b>observation</b> — turns, speech, damage, NPC brain state, LLM trace. Never
 *       replayed; this is the output channel you diff and read.</li>
 * </ul>
 *
 * <h3>Interface</h3>
 * <pre>
 *   -Dreplay.record=&lt;path|true&gt;   record to path (true =&gt; replays/replay-&lt;ts&gt;.jsonl)
 *   -Dreplay.play=&lt;path&gt;          drive input from this file instead of the keyboard
 *   -Dreplay.speed=1.0            playback rate; &gt;1 fast-forwards (careful: async
 *                                 inference does not speed up with it)
 *   -Dreplay.tailFrames=600       frames to keep running after the last input, so
 *                                 in-flight NPC reactions still land in the log
 *   -Dreplay.exitAtEnd=true       quit once the tail elapses (for the harness)
 *   -Dreplay.seed=&lt;long&gt;          override the seed (playback uses the recorded one)
 * </pre>
 *
 * Every gameplay roll comes from {@link Rng}, whose seed is written into the header and read
 * back on playback, so a replay reproduces the same town, the same names and the same rolls.
 * Model sampling is the one thing outside that guarantee — see {@code LlamaHttpInferenceService}.
 */
public final class Replay {

    private static ReplayRecorder recorder;
    private static ReplayPlayer player;
    private static int frame;
    private static int readyFrame;
    private static boolean initialized;

    private Replay() {}

    /** Read the system properties and open the files. Safe to call more than once. */
    public static synchronized void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        // Seed first, and before anything builds a world: the seed only reproduces a run if
        // it is in force for the very first roll. Playback inherits the recorded seed unless
        // -Dreplay.seed overrides it, which is how you re-roll one scenario deliberately.
        String play = System.getProperty("replay.play");
        Long recorded = play == null || play.isEmpty() ? null : ReplayPlayer.readSeed(play);
        String override = System.getProperty("replay.seed");
        long seed = override != null && !override.isEmpty() ? Long.parseLong(override)
                : recorded != null ? recorded : System.nanoTime();
        Rng.seed(seed);
        System.out.println("replay: seed " + seed
                + (recorded != null && override == null ? " (from replay)" : ""));

        // Recording is on by default: the session worth replaying is always the one you
        // did not think to arm beforehand. Opt out with -Dreplay.record=false.
        String record = System.getProperty("replay.record", "true");
        if (!record.isEmpty() && !"false".equalsIgnoreCase(record) && !"off".equalsIgnoreCase(record)) {
            recorder = ReplayRecorder.open(record);
        }

        if (play != null && !play.isEmpty()) {
            player = ReplayPlayer.open(play);
        }
    }

    /**
     * The world exists and is accepting input. Replays are anchored here rather than at
     * frame 0 because the loading phase is not reproducible — staging a model can take
     * seconds or minutes — and inputs released during loading would be swallowed by
     * {@code InGameMode}'s inactive-mode guard and silently lost.
     */
    public static void markReady() {
        readyFrame = frame;
        if (player != null) {
            player.markReady(frame);
        }
        observe("ready", "frame", frame);
    }

    /** Frames since the world became playable — the timeline replays are written against. */
    public static int sessionFrame() {
        return frame - readyFrame;
    }

    public static boolean isRecording() {
        return recorder != null;
    }

    public static boolean isPlaying() {
        return player != null && !player.isFinished();
    }

    /** True while a replay is loaded, including its post-input tail. */
    public static boolean isPlaybackSession() {
        return player != null;
    }

    public static ReplayPlayer player() {
        return player;
    }

    /** Frame counter, advanced once per rendered frame from the main loop. */
    public static int frame() {
        return frame;
    }

    public static void nextFrame() {
        frame++;
        if (player != null) {
            player.tick(frame);
        }
    }

    /** Record one keypress plus the modifier state that gives it its meaning. */
    public static void recordInput(int key, char chr, boolean ctrl, boolean shift, boolean alt) {
        if (recorder == null) {
            return;
        }
        // Relative to ready, so a replay recorded after a slow model download still plays
        // back promptly on a warm start.
        recorder.write("input",
                "frame", sessionFrame(),
                "key", key,
                "chr", chr == 0 ? "" : String.valueOf(chr),
                "ctrl", ctrl,
                "shift", shift,
                "alt", alt);
    }

    /**
     * Append an observation. {@code kv} alternates key and value; Strings are quoted and
     * escaped, numbers and booleans are written bare. No-op unless recording, so callers
     * can sprinkle these freely.
     */
    public static void observe(String type, Object... kv) {
        if (recorder == null) {
            return;
        }
        recorder.write(type, kv);
    }

    /** Free-text trace line (LLM pipeline chatter). */
    public static void trace(String message) {
        observe("trace", "msg", message);
    }

    public static void shutdown() {
        if (recorder != null) {
            recorder.close();
            recorder = null;
        }
    }
}
