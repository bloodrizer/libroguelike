package com.nuclearunicorn.serialkiller.game.sound;

import com.nuclearunicorn.libroguelike.events.PointBasedEvent;

/**
 * What one listener actually got: the noise, how loud it arrived <i>here</i>, and which way
 * it came from (SOUND_DESIGN.md 6.1).
 *
 * <p>Those three numbers are the whole interface between acoustics and AI, and they are
 * deliberately the same three that Project Acoustics found a game engine needs. Note what is
 * <i>not</i> here: the listener is never handed the source position as a thing it knows. It
 * knows a direction to start walking, which is how an NPC ends up going to the doorway the
 * scream came through rather than at the wall it happened behind.
 */
public class SoundHeard extends PointBasedEvent {

    public final SoundEvent sound;

    /** Level at the listener's tile, in dB. Always above their threshold or this is not sent. */
    public final int received;

    /**
     * Index into {@link Acoustics#DX}/{@link Acoustics#DY} pointing one step back along the
     * path the sound took, or -1 when the listener is standing on the source tile.
     */
    public final int fromDir;

    public SoundHeard(SoundEvent sound, int received, int fromDir) {
        super(sound.getOrigin());
        this.sound = sound;
        this.received = received;
        this.fromDir = fromDir;
    }

    public SoundKind kind() {
        return sound.kind;
    }

    /** How muffled it sounded: 0 = right on top of it, 1 = only just audible. */
    public float muffling() {
        int span = sound.loudness - SoundConfig.FLOOR;
        if (span <= 0) {
            return 1.0f;
        }
        float m = 1.0f - (float) (received - SoundConfig.FLOOR) / span;
        return m < 0 ? 0 : (m > 1 ? 1 : m);
    }

    /** Compass word for the direction it arrived from, for player-facing messages. */
    public String bearing() {
        return Acoustics.bearing(fromDir);
    }
}
