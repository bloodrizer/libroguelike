package com.nuclearunicorn.serialkiller.generators;

import com.nuclearunicorn.libroguelike.game.world.WorldChunk;
import com.nuclearunicorn.libroguelike.game.world.WorldTile;
import com.nuclearunicorn.libroguelike.game.world.generators.ChunkGenerator;
import com.nuclearunicorn.serialkiller.game.world.RLTile;
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

    int seed;
    Random chunk_random;

    public void generate(Point origin){

        seed = origin.getX()*10000+origin.getY();
        chunk_random = new Random(seed);


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

        //----------debug---------
        Block __block = new Block( 10, 10, 50, 50);
        //traceBlock(__block);

        MapGenerator gen = new MapGenerator(__block);
        gen.setSeed(123456);
        List<Block> __blocks = new ArrayList<Block>();
        __blocks.add(__block);

        List<Block> resultBlocks = gen.process(__blocks);
        for(Block result: resultBlocks){
            traceBlock(result);
        }
        //---------debug end-------


        //Now, time to generate sum town

        Block gameBlock = new Block(
                x + 5,
                y + 5,
                WorldChunk.CHUNK_SIZE - 10 ,
                WorldChunk.CHUNK_SIZE - 10
        );

        MapGenerator mapgen = new MapGenerator(gameBlock);
        mapgen.setSeed(seed);

        List<Block> blocks = new ArrayList<Block>();
        blocks.add(gameBlock);

        List<Block> districts = mapgen.process(blocks);

        for(Block district: districts){
            district.scale(-4,-4);
            fillBlock(district);
        }

    }

    private void fillBlock(Block district){
        int chance = chunk_random.nextInt(100);
        if (chance > 20){
            generateHousing(district);
        }else{
            //generate park
        }
    }

    private void generateHousing(Block block) {
        //todo: get object manager there
        //#bug there >>>>>
        //#commenting following lines will exploit incorrect room wall placement

        //traceBlock(block);

        //#<<< bug there

        int ROOM_COUNT = 4;

        MapGenerator gen = new MapGenerator(block);
        gen.setMinBlockSize( block.getArea() / ROOM_COUNT );

        List<Block> housePrefab = new ArrayList<Block>();
        housePrefab.add(block);

        List<Block> rooms = gen.roomProcess(housePrefab);

        //TODO: extract method traceBlock
        for(Block room: rooms){
            //traceBlock(room);
        }
    }
    /**
      Trace outer conture of block and mark every outer block as wall
     */
    private void traceBlock(Block block){
        for (int i = 0; i<= block.getH(); i++){
            placeWall(block.getX(), block.getY()+i);
            placeWall(block.getX()+block.getW(), block.getY()+i);
        }
        for (int j = 0; j<= block.getW(); j++){
            placeWall(block.getX()+j, block.getY());
            placeWall(block.getX()+j, block.getY()+block.getH());
        }
    }

    private void placeWall(int i, int j){
        RLTile tile = (RLTile)(getLayer().get_tile(i,j));
        tile.setWall(true);
        //TODO: add isBlockSight to RLTile

        //self.tiles[(x,y)].blocked = True
        //self.tiles[(x,y)].block_sight = True
    }


    private WorldTile addTile(int i, int j, Random chunk_random) {
        WorldTile tile = new RLTile();
        Point origin = new Point(i,j);
        tile.origin = origin;

        getLayer().set_tile(origin, tile);

        return tile;
    }
}