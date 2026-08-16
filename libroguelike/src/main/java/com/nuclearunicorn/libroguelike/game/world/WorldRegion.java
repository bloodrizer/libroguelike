/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nuclearunicorn.libroguelike.game.world;

import org.lwjgl.util.Point;

/**
 *
 * @author bloodrizer
 */

/*
 * Simple static helper class, that handles region mechanics
 * that includes village ownership, totem status, etc.
 * 
 */
public class WorldRegion {
    public static int REGION_SIZE = 5;
    
    public Point origin = new Point(0,0);

    
    public static Point get_region_coord(Point tile_coord){
        Point chunk_coord = WorldChunk.get_chunk_coord(tile_coord);
        int rx = chunk_coord.getX()/REGION_SIZE;
        int ry = chunk_coord.getY()/REGION_SIZE;
        
        chunk_coord.setLocation(rx, ry);    
        /*
         * we probably do not need to use defensive copyng there since
         * get_chunk_coord creates safe object anyway
         */
        return chunk_coord;
    }
    
    //load Region data from server side
    //for now just generate it procedural way
    public void load_data(){
    }
}
