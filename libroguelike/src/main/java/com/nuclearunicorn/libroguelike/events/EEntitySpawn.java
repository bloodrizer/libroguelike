/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.events;

import com.nuclearunicorn.libroguelike.game.ent.Entity;
import org.lwjgl.util.Point;

/**
 *
 * @author Administrator
 */
public class EEntitySpawn extends Event{
    
     public Entity ent = null;
     public Point origin = null;

     public EEntitySpawn(Entity ent, Point origin){
        this.ent    = ent;
        this.origin = origin;
    }
}
