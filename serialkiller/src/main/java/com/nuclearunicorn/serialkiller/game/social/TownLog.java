package com.nuclearunicorn.serialkiller.game.social;

import com.nuclearunicorn.libroguelike.core.replay.Replay;
import com.nuclearunicorn.serialkiller.game.ai.llm.sense.GameTurn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * What the town did, whether or not the player was there to see it.
 *
 * <p>The message console is the <i>player's</i> record and is gated on the player's senses —
 * that is the point of {@link com.nuclearunicorn.serialkiller.game.sound.PlayerEars}. Which
 * leaves nowhere to answer "what is actually going on out there", and a social simulation you
 * cannot observe is a social simulation you cannot tune. This is the god's-eye half: every
 * notable social act, in order, with the turn it happened on, read back by the ALT overlay.
 *
 * <p>Deliberately not the message log and deliberately not the replay trace. The console is
 * for the player, the trace is for a post-mortem, and this is for the frame you are looking
 * at right now.
 */
public final class TownLog {

    private TownLog() {}

    /** What kind of thing happened. Drives the overlay's colour and its per-kind tallies. */
    public enum Kind {
        SEX, RAPE, CRIME, DEATH, BIRTH, ARREST, OTHER
    }

    public static final class Entry {
        public final long turn;
        public final Kind kind;
        public final String text;
        /** Where it happened, for the overlay's map ticks. Either coordinate may be 0. */
        public final int x;
        public final int y;

        Entry(long turn, Kind kind, String text, int x, int y) {
            this.turn = turn;
            this.kind = kind;
            this.text = text;
            this.x = x;
            this.y = y;
        }
    }

    /** Long enough to cover a night in town, short enough to render and to never grow. */
    private static final int CAPACITY = 64;

    private static final List<Entry> entries = new ArrayList<Entry>();
    private static final int[] tally = new int[Kind.values().length];

    public static synchronized void record(Kind kind, String text, int x, int y) {
        Entry entry = new Entry(GameTurn.current(), kind, text, x, y);
        entries.add(entry);
        tally[kind.ordinal()]++;
        //and into the replay, so a post-mortem has the same chronology the overlay shows
        Replay.trace("town " + kind + " t" + entry.turn + " " + text + " @" + x + "," + y);
        while (entries.size() > CAPACITY) {
            entries.remove(0);
        }
    }

    /** Newest last, as it happened. */
    public static synchronized List<Entry> entries() {
        return Collections.unmodifiableList(new ArrayList<Entry>(entries));
    }

    /** How many of this kind have happened all session, not just how many are still listed. */
    public static synchronized int count(Kind kind) {
        return tally[kind.ordinal()];
    }

    /** A new town is a new record; the statics outlive the world otherwise. */
    public static synchronized void clear() {
        entries.clear();
        for (int i = 0; i < tally.length; i++) {
            tally[i] = 0;
        }
    }
}
