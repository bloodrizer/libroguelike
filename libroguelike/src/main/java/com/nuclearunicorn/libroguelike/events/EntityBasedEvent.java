package com.nuclearunicorn.libroguelike.events;

import com.nuclearunicorn.libroguelike.game.ent.Entity;

/**

 */
public class EntityBasedEvent extends Event{
        protected Entity entity;

        public EntityBasedEvent(Entity entity){
            this.entity = entity;
        }

        public Entity getEntity(){
            return entity;
        }
}
