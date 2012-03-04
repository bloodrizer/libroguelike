package com.nuclearunicorn.serialkiller.generators;

import com.nuclearunicorn.libroguelike.game.world.WorldChunk;
import com.nuclearunicorn.libroguelike.game.world.WorldTile;
import com.nuclearunicorn.libroguelike.game.world.generators.ChunkGenerator;
import org.lwjgl.util.Point;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by IntelliJ IDEA.
 * User: Administrator
 * Date: 03.03.12
 * Time: 19:10
 * To change this template use File | Settings | File Templates.
 */
public class TownChunkGenerator extends ChunkGenerator {

    public void generate(Point origin){

        Random chunk_random = new Random();
        chunk_random.setSeed(origin.getX()*10000+origin.getY());

        int x = origin.getX()* WorldChunk.CHUNK_SIZE;
        int y = origin.getY()*WorldChunk.CHUNK_SIZE;
        int size = WorldChunk.CHUNK_SIZE;

        final int OFFSET = WorldChunk.CHUNK_SIZE;

        for (int i = x - OFFSET; i<x+size+OFFSET; i++ ){
            for (int j = y - OFFSET; j<y+size+OFFSET; j++){
                if ( i>= x && i<x+size && j >=y && j < y+size){
                    addTile(i,j, chunk_random);
                }
            }
        }

        //Now, time to generate sum town

        Block gameBlock = new Block(
                x + 5,
                y + 5,
                WorldChunk.CHUNK_SIZE - 10 ,
                WorldChunk.CHUNK_SIZE - 10
        );
        MapGenerator mapgen = new MapGenerator(gameBlock);
        List<Block> blocks = new ArrayList<Block>();
        blocks.add(gameBlock);

        List<Block> districts = mapgen.process(blocks);
       
        System.out.println(districts);
        
        for(Block district: districts){
            district.scale(-2,-2);
            for (int i = district.getX(); i< district.getX()+district.getW(); i++){
                for (int j = district.getY(); j< district.getY()+district.getH(); j++){
                    getLayer().get_tile(i,j).set_height(100);

                    //TODO: some protection from recursive chunk bloating
                    //WorldTile tile = getLayer().get_cached_chunk(origin).tile_data.get(new Point(i,j));
                }
            }
        }

    }

    private WorldTile addTile(int i, int j, Random chunk_random) {
        WorldTile tile = new WorldTile();
        Point origin = new Point(i,j);
        tile.origin = origin;

        getLayer().set_tile(origin, tile);

        return tile;
    }
}