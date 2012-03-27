package com.nuclearunicorn.serialkiller.game.events;

import com.nuclearunicorn.libroguelike.events.PointBasedEvent;
import org.lwjgl.util.Point;

/**

 */
public class SuspiciousSoundEvent extends PointBasedEvent{
    public int radius;
    public SuspiciousSoundEvent(Point origin, int radius) {
        super(origin);
        this.radius = radius;
    }
}
