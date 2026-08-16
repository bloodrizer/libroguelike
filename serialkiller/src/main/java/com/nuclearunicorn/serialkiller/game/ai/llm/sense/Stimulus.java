package com.nuclearunicorn.serialkiller.game.ai.llm.sense;

import java.io.Serializable;

/**
 * One thing an NPC sensed, with enough structure to rank it (§7). Replaces the bare
 * observation strings, which carried no channel, no source, no time and no priority — so
 * "the player is talking to you" and "EEntityChangeChunk" were indistinguishable.
 *
 * <p>Serializable along with the AI that owns it.
 */
public class Stimulus implements Serializable {

    /** Which sensor produced this. Channels exist so the prompt can group by modality. */
    public enum Channel { SPEECH, SIGHT, EVENT, PAIN }

    public final long turn;
    public final Channel channel;
    public final int salience;
    /** Entity uid that caused it, or null for sourceless events. Drives conversation focus. */
    public final String sourceUid;
    /** Already folded into a submitted prompt — kept for context but no longer a trigger. */
    public boolean consumed;

    private final String text;

    public Stimulus(long turn, Channel channel, int salience, String sourceUid, String text) {
        this.turn = turn;
        this.channel = channel;
        this.salience = salience;
        this.sourceUid = sourceUid;
        this.text = text;
    }

    /**
     * Salience faded by age (§7). A shout you ignored for fifty turns should stop
     * outranking what is in front of you now, and stale stimuli should fall below the
     * interrupt threshold on their own rather than needing to be swept.
     */
    public int effectiveSalience(long now, int decayPerTurn) {
        long age = Math.max(0, now - turn);
        return Math.max(0, salience - (int) (age * decayPerTurn));
    }

    /** The sentence shown to the model. */
    public String text() {
        return text;
    }

    @Override
    public String toString() {
        return "[" + Salience.label(salience) + "/" + channel + "] " + text;
    }
}
