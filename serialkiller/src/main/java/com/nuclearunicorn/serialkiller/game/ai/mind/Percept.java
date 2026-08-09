package com.nuclearunicorn.serialkiller.game.ai.mind;

import org.lwjgl.util.Point;

import java.io.Serializable;

/**
 * What one NPC believes about one other entity, and when it last had reason to believe it.
 *
 * <p>This is the game-AI standard and it goes by three names: Halo 2 calls them <i>props</i>
 * ("a believed world-state separate from actual world conditions"), Thief calls them
 * <i>sense links</i> ("time, location, and line-of-sight"), F.E.A.R. calls them working-memory
 * facts with a confidence value. All three exist for the same reason — an agent that reads
 * the world directly cannot be wrong, and an agent that cannot be wrong cannot be surprised,
 * fooled, or slow on the uptake.
 *
 * <p>What this replaces is a scatter of parallel fields: a {@code List<EntityActor>} of known
 * criminals on the FSM brain, and {@code threatUid}/{@code suspectUid} plus four turn
 * counters on the LLM one. Those were the same fact — "this person is dangerous and I know
 * where they were" — recorded three times in three formats, which is why the FSM and the
 * LLM brains could not share a single line of policing.
 */
public class Percept implements Serializable {

    /** How we came to know. Firsthand knowledge outranks a radio call — see {@link #confidence}. */
    public enum Source { SEEN, REPORTED }

    public final String uid;

    /** Last turn we had eyes on them, or -1 if we never have. */
    public long lastSeenTurn = -1;
    /** Where they were then. Believed, not current: this is the point of the whole class. */
    public Point lastKnownAt;
    public Source source = Source.REPORTED;

    /** Last turn they hurt us. Drives the flee reflex. */
    public long attackedUsTurn = -1;
    /** Last turn we learned they committed a crime. Drives the police one. */
    public long crimeTurn = -1;
    /** Where that crime happened, for an officer who has to go and look. */
    public Point crimeAt;

    public Percept(String uid) {
        this.uid = uid;
    }

    /** Fold in a sighting. Firsthand never downgrades to hearsay. */
    public void saw(long turn, Point at, Source how) {
        if (turn < lastSeenTurn) {
            return;
        }
        lastSeenTurn = turn;
        lastKnownAt = at == null ? lastKnownAt : new Point(at);
        if (how == Source.SEEN || source == null) {
            source = how;
        }
    }

    public void noteAttack(long turn) {
        attackedUsTurn = Math.max(attackedUsTurn, turn);
    }

    public void noteCrime(long turn, Point where) {
        if (turn < crimeTurn) {
            return;
        }
        crimeTurn = turn;
        if (where != null) {
            crimeAt = new Point(where);
        }
    }

    public boolean isThreat(long now, int ttl) {
        return attackedUsTurn >= 0 && now - attackedUsTurn <= ttl;
    }

    public boolean isCriminal(long now, int ttl) {
        return crimeTurn >= 0 && now - crimeTurn <= ttl;
    }

    /**
     * How much this belief is still worth, 0..1, fading with age. Thief degrades awareness
     * through intermediate states rather than snapping it off, which is what stops an AI
     * from forgetting you the instant you break line of sight — and what lets a policeman
     * keep searching a corner you are no longer standing in.
     */
    public float confidence(long now, int ttl) {
        if (lastSeenTurn < 0 || ttl <= 0) {
            return 0f;
        }
        float fresh = 1f - (float) (now - lastSeenTurn) / ttl;
        float ceiling = source == Source.SEEN ? 1f : 0.6f;   // hearsay is never certainty
        return Math.max(0f, Math.min(ceiling, fresh));
    }

    @Override
    public String toString() {
        return uid + "@" + lastKnownAt + " seen=" + lastSeenTurn
                + (attackedUsTurn >= 0 ? " attackedUs=" + attackedUsTurn : "")
                + (crimeTurn >= 0 ? " crime=" + crimeTurn : "");
    }
}
