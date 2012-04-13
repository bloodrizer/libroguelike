package com.nuclearunicorn.serialkiller.game.events;

import com.nuclearunicorn.libroguelike.events.EntityBasedEvent;
import com.nuclearunicorn.libroguelike.game.ent.Entity;

/**

 */
public class ShowContainerViewEvent extends EntityBasedEvent {
    public ShowContainerViewEvent(Entity entity) {
        super(entity);
    }
}
