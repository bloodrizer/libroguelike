/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.game.world;

import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.game.world.layers.WorldLayer;
import org.lwjgl.util.Point;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Administrator
 */
public class WorldChunk implements Serializable{
    //todo: synch with server?
    public static int CHUNK_SIZE = 32;

    public Point origin = new Point(0,0);

    protected List<Entity> entList = new ArrayList<Entity>(100);
    public Map<Point,WorldTile> tile_data = new java.util.HashMap<Point,WorldTile>(1024);

    public boolean dirty = true;
    
    private WorldLayer layer = null;

    public WorldChunk(int chunk_x, int chunk_y){
        origin.setLocation(chunk_x, chunk_y);
    }
    
    public List<Entity> getEntList(){
        return entList;
    }

    public synchronized boolean add_entity(Entity ent){

        if (!entList.contains(ent)){

            entList.add(ent);
            ent.set_chunk(this);
            
            return true;

        }
        return false;
    }


    public void remove_entity(Entity ent){
            entList.remove(ent);
    }

    public void set_layer(WorldLayer layer) {
        this.layer = layer;
    }
    
    public static Point get_chunk_coord(Point position) {
        //TODO: use util point?
        int cx = (int)Math.floor((float)position.getX() / CHUNK_SIZE);
        int cy = (int)Math.floor((float)position.getY() / CHUNK_SIZE);

        return new Point(cx,cy);
    }
    
    public synchronized void unload(){
        throw new RuntimeException("Unloading method is not defined");
    }

}
