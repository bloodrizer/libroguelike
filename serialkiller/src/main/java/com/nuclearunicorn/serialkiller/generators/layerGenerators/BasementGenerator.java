package com.nuclearunicorn.serialkiller.generators.layerGenerators;

import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.game.world.WorldChunk;
import com.nuclearunicorn.libroguelike.game.world.WorldTile;
import com.nuclearunicorn.libroguelike.game.world.generators.ChunkGenerator;
import com.nuclearunicorn.serialkiller.game.world.RLTile;
import com.nuclearunicorn.serialkiller.game.world.RLWorldChunk;
import com.nuclearunicorn.serialkiller.game.world.entities.EntLadder;
import com.nuclearunicorn.serialkiller.game.world.entities.EntityRLActor;
import com.nuclearunicorn.serialkiller.render.AsciiEntRenderer;
import org.lwjgl.util.Point;
import org.newdawn.slick.Color;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 */
public class BasementGenerator extends ChunkGenerator {

    int seed;
    Random chunk_random;
    RLWorldChunk chunk;

    public static Map<Integer, ArrayList<Point>> ladderPositions = new HashMap<Integer, ArrayList<Point>>();

    public void generate(WorldChunk chunk){

        if (chunk instanceof RLWorldChunk){
            this.chunk = (RLWorldChunk)chunk;
        }else{
            throw new RuntimeException("trying to generate non-RLWorldChunk element");
        }

        seed = chunk.origin.getX()*10000 + chunk.origin.getY();
        chunk_random = new Random(seed);


        int x = chunk.origin.getX()* WorldChunk.CHUNK_SIZE;
        int y = chunk.origin.getY()*WorldChunk.CHUNK_SIZE;
        int size = WorldChunk.CHUNK_SIZE;

        final int OFFSET = WorldChunk.CHUNK_SIZE;

        for (int i = x - OFFSET; i<x+size+OFFSET; i++ ){
            for (int j = y - OFFSET; j<y+size+OFFSET; j++){
                if ( i>= x && i<x+size && j >=y && j < y+size){
                    WorldTile tile = addTile(i,j, chunk_random);
                    RLTile rlTile = (RLTile)tile;

                    rlTile.setWall(true);
                }
            }
        }
        if (ladderPositions.get(getLayer().get_zindex()-1) == null){
            System.err.println("Null ladder list at layer #" + (getLayer().get_zindex()-1));
            return;
        }

        for (Point ladder: ladderPositions.get(getLayer().get_zindex()-1)){
            for (int i = ladder.getX() - 3; i < ladder.getX() + 3; i++){
                for (int j = ladder.getY() - 3; j < ladder.getY() + 3; j++){
                    getRLTile(i,j).setWall(false);
                }
            }

            //place ascending ladder
            EntLadder ladderEnt = new EntLadder(); //desc ladder
            placeEntity(ladder.getX()+1, ladder.getY()+1, ladderEnt, "ladder", "<", Color.green);
            ladderEnt.setDescending(false);
        }
    }

    private WorldTile addTile(int i, int j, Random chunk_random) {
        WorldTile tile = new RLTile();
        Point origin = new Point(i,j);
        tile.origin = origin;

        getLayer().set_tile(origin, tile);

        return tile;
    }

    public static void addLadder(int zindex, Point coord) {
        if (ladderPositions.get(zindex) == null){
            ladderPositions.put(zindex, new ArrayList<Point>(32));
        }
        ladderPositions.get(zindex).add(coord);
    }

    public RLTile getRLTile(int i, int j){
        RLTile tile = (RLTile)(getLayer().get_tile(i, j));
        return tile;
    }

    public void placeEntity(int x, int y, EntityRLActor entity, String symbol, String name, Color color) {
        placeEntity(x, y, entity, symbol, name);
        ((AsciiEntRenderer) entity.get_render()).setColor(color);
    }

    private void placeEntity(int x, int y, Entity ent, String name, String symbol){
        ent.setName(name);
        ent.setEnvironment(environment);
        ent.setRenderer(new AsciiEntRenderer(symbol));
        ent.set_blocking(true);

        System.out.println("Spawning ladder ent @ layer #"+getLayer().get_zindex());
        ent.setLayerId(getLayer().get_zindex());
        ent.spawn(new Point(x,y));
    }

}
