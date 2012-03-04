/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nuclearunicorn.libroguelike.game.world.generators;

import com.nuclearunicorn.libroguelike.game.GameEnvironment;
import com.nuclearunicorn.libroguelike.game.world.WorldChunk;
import com.nuclearunicorn.libroguelike.game.world.WorldTile;
import com.nuclearunicorn.libroguelike.game.world.layers.WorldLayer;
import com.nuclearunicorn.libroguelike.utils.NLTimer;
import org.lwjgl.util.Point;

import java.util.Random;

/**
 *
 * @author bloodrizer
 */
public class ChunkGroundGenerator extends ChunkGenerator {
    

    public ChunkGroundGenerator(){

    }

    @Override
    public void setEnvironment(GameEnvironment environment){
        super.setEnvironment(environment);
    }
    
    @Override
    public void generate(Point origin){

        NLTimer timer = new NLTimer();
        timer.push();

        Random chunk_random = new Random();
        chunk_random.setSeed(origin.getX()*10000+origin.getY());    //set chunk-specific seed

        //Thread.currentThread().dumpStack();
        //System.out.println("building data chunk @"+origin.toString());

        int x = origin.getX()* WorldChunk.CHUNK_SIZE;
        int y = origin.getY()*WorldChunk.CHUNK_SIZE;
        int size = WorldChunk.CHUNK_SIZE;
        
        final int OFFSET = WorldChunk.CHUNK_SIZE;

        for (int i = x - OFFSET; i<x+size+OFFSET; i++ ){
            for (int j = y - OFFSET; j<y+size+OFFSET; j++){
                if ( i>= x && i<x+size && j >=y && j < y+size){
                    build_chunk_tile(i,j, chunk_random);
                }
            }
        }

        //todo:generate objects

        timer.pop("chunk @"+origin.getX()+","+origin.getY());
    }

    //TODO: use environment.getWorldLayer there
    protected WorldLayer getLayer() {
        return environment.getWorldLayer(z_index);
    }
    
    //--------------------------------------------------------------------------
    private WorldTile build_chunk_tile(int i, int j, Random chunk_random){

        WorldTile tile = new WorldTile();
        Point origin = new Point(i,j);
        tile.origin = origin;
                //important!
                //tile should be registered before any action is performed on it
        getLayer().set_tile(origin, tile);
        //tile.set_height(height);

        return tile;
    }
}
