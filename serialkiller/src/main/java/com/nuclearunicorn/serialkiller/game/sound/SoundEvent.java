package com.nuclearunicorn.serialkiller.game.sound;

import com.nuclearunicorn.libroguelike.events.PointBasedEvent;
import com.nuclearunicorn.libroguelike.game.ent.EntityActor;
import org.lwjgl.util.Point;

/**
 * A noise happening at a place (SOUND_DESIGN.md 6). Replaces {@code SuspiciousSoundEvent},
 * whose entire model was a Euclidean radius.
 *
 * <p>Posting one of these does nothing on its own — {@link Acoustics#emit} is what turns it
 * into a field and delivers a {@link SoundHeard} to whoever the field says can hear it. The
 * event itself carries only what was true at the source: what happened, how loud, and who
 * did it.
 */
public class SoundEvent extends PointBasedEvent {

    public final SoundKind kind;

    /** Who made the noise, or null for a sourceless one (a window falling in). */
    public final EntityActor source;

    /** Source level in dB. Defaults to the kind's, but an emitter may bend it. */
    public final int loudness;

    public final int layerId;

    public SoundEvent(Point origin, SoundKind kind, EntityActor source, int layerId) {
        this(origin, kind, source, layerId, kind.db());
    }

    public SoundEvent(Point origin, SoundKind kind, EntityActor source, int layerId,
                      int loudness) {
        super(origin);
        this.kind = kind;
        this.source = source;
        this.layerId = layerId;
        this.loudness = loudness;
    }

    /** Compute the field and hand this to every listener that can hear it. */
    public void emit() {
        Acoustics.emit(this);
    }

    /**
     * A sound is delivered by the field, never broadcast.
     *
     * <p>Overridden so that {@code post()} — which every other event in the codebase uses,
     * and which hands the event to every subscriber regardless of where they are standing —
     * cannot silently reintroduce the unattenuated dispatch this class exists to replace.
     */
    @Override
    public void post() {
        emit();
    }

    @Override
    public String toString() {
        return kind + "(" + loudness + "dB) @" + origin.getX() + "," + origin.getY();
    }
}
