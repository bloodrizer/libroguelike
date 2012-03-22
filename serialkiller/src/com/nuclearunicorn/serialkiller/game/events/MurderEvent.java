package com.nuclearunicorn.serialkiller.game.events;

import com.nuclearunicorn.libroguelike.events.Event;
import com.nuclearunicorn.serialkiller.game.world.entities.EntityRLHuman;
import org.lwjgl.util.Point;

/**
 */
public class MurderEvent extends Event {
    public EntityRLHuman criminal;
    public Point origin;
    
    public MurderEvent(Point origin, EntityRLHuman criminal){
        this.criminal = criminal;
        this.origin = origin;
    }
}
