package com.nuclearunicorn.serialkiller.game.events;

import com.nuclearunicorn.libroguelike.events.Event;
import com.nuclearunicorn.libroguelike.game.ent.Entity;

/**
 * Show detailed information about npc entity
 */
public class ShowDetailedInformationEvent extends Event {
    public Entity ent;
    public ShowDetailedInformationEvent(Entity ent) {
        this.ent = ent;
    }
}
