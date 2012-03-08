/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.events.network;

import com.nuclearunicorn.libroguelike.events.Event;
import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.game.world.WorldTile;
import com.nuclearunicorn.libroguelike.game.world.layers.WorldLayer;
import org.lwjgl.util.Point;


/**
 *
 * @author Administrator
 */
@NetID(id="")
public class EEntityMove extends Event {
    private Point from = new Point();
    private Point to   = new Point();
    public Entity entity = null;

    public Point getFrom(){
        return new Point(from);
    }

    public Point getTo(){
        return new Point(to);
    }

    public EEntityMove(Entity entity, Point target){
        this.entity = entity;
        this.from.setLocation(entity.origin);
        this.to.setLocation(target);
    }

    public static String toString(EEntityMove event){
        return "[#"+Integer.toString(event.get_eventid())+"[EEntityMove] from @"+event.from.toString()+" to @"+event.to.toString()+"]";
    }

    //safe switch - do not allow to move at the place that is blocked
    //TODO: recalculate astar path by posting new event

    @Override
    public void post(){

        WorldLayer layer = entity.getLayer();

        WorldTile tile = layer.get_tile(this.to.getX(), this.to.getY());
        if (tile!= null && !tile.isBlocked()){
            super.post();
        }
        //TODO: fix server bug when tile is null
    }

    @Override
    public String toString(){
        return toString(this);
    }

    /*@Override
    public String get_id(){
        if (this.entity == Player.get_ent()){
            return "0x0260";
        }
        return "0x0280";
        
    }*/

    /*@Override
    public String[] serialize(){
        return new String[] {
            get_id(),
            Integer.toString(this.to.getX()),
            Integer.toString(this.to.getY())
        };
    }*/
}
