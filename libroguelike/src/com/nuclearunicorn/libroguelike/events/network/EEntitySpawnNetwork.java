package com.nuclearunicorn.libroguelike.events.network;

import com.nuclearunicorn.libroguelike.game.ent.Entity;
import org.lwjgl.util.Point;


/*
    This event is used by Server to notify client of chunk entities.
    Do not use this event once good method of chunk data streaming will be implemented
 */
public class EEntitySpawnNetwork extends NetworkEvent {

    public Entity ent = null;
    public Point origin = null;

    public EEntitySpawnNetwork(Entity ent, Point origin){
        this.ent    = ent;
        this.origin = origin;
    }

    @Override
    public String[] serialize(){
        return new String[] {
                ent.get_uid(),
                Integer.toString(origin.getX()),
                Integer.toString(origin.getY())
        };
    }
}
