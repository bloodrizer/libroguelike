/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.events;

import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.game.world.WorldChunk;

/**
 *
 * @author Administrator
 */
public class EEntityChangeChunk extends Event{
    public WorldChunk from = null;
    public WorldChunk to = null;
    public Entity ent;

    public EEntityChangeChunk(Entity ent, WorldChunk from, WorldChunk to){
        this.ent = ent;
        this.from = from;
        this.to = to;
    }
}
