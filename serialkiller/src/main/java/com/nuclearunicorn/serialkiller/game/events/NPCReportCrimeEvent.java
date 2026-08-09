package com.nuclearunicorn.serialkiller.game.events;

import com.nuclearunicorn.libroguelike.events.PointBasedEvent;
import com.nuclearunicorn.libroguelike.game.ent.EntityActor;
import org.lwjgl.util.Point;

/**
 * A crime called in: where it happened, and — when the caller got a look at them — who did it.
 *
 * <p>The suspect is the part that was missing. A report used to carry a bare location, so the
 * best a responding officer could do was walk to a street corner and stand there; the person
 * who had just committed the crime was, as far as the report was concerned, a stranger. Naming
 * them is what turns "go and look" into "go and arrest him", and it is the difference between
 * a police force that reacts to crime and one that tours crime scenes after the fact.
 */
public class NPCReportCrimeEvent extends PointBasedEvent {

    /** Who did it, or null when the caller only heard something. */
    public final EntityActor criminal;
    /** Who called it in, so a dispatcher can decline to route a report back to its source. */
    public final EntityActor reporter;

    public NPCReportCrimeEvent(Point origin) {
        this(origin, null, null);
    }

    public NPCReportCrimeEvent(Point origin, EntityActor criminal, EntityActor reporter) {
        super(origin);
        this.criminal = criminal;
        this.reporter = reporter;
    }
}
