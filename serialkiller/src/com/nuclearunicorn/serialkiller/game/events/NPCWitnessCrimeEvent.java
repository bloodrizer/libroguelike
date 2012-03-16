package com.nuclearunicorn.serialkiller.game.events;

import com.nuclearunicorn.libroguelike.events.Event;
import com.nuclearunicorn.libroguelike.game.ent.EntityActor;
import org.lwjgl.util.Point;

/**
 */
public class NPCWitnessCrimeEvent extends Event {
    public Point origin;
    public EntityActor criminal;

    public NPCWitnessCrimeEvent(Point origin, EntityActor criminal) {
        this.origin = origin;
        this.criminal = criminal;
    }
}
