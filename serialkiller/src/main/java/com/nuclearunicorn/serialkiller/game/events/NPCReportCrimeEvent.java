package com.nuclearunicorn.serialkiller.game.events;

import com.nuclearunicorn.libroguelike.events.Event;
import com.nuclearunicorn.libroguelike.events.PointBasedEvent;
import org.lwjgl.util.Point;

/**

 */
public class NPCReportCrimeEvent extends PointBasedEvent {
    public NPCReportCrimeEvent(Point origin) {
        super(origin);
    }
}
