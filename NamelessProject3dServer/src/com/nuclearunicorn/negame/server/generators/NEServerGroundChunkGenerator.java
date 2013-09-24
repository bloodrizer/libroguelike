package com.nuclearunicorn.negame.server.generators;

import com.nuclearunicorn.libroguelike.game.world.Terrain;
import com.nuclearunicorn.libroguelike.game.world.WorldTile;
import com.nuclearunicorn.libroguelike.game.world.WorldView;
import com.nuclearunicorn.libroguelike.game.world.generators.ObjectGenerator;
import com.nuclearunicorn.negame.client.game.world.NEVoxelTile;
import com.nuclearunicorn.negame.common.generators.GenericGroundChunkGenerator;
import com.nuclearunicorn.negame.common.world.GenericNETile;
import com.nuclearunicorn.negame.server.generators.environment.FoliageGenerator;
import org.lwjgl.util.Point;

import java.util.Random;

/**
 */
public class NEServerGroundChunkGenerator extends GenericGroundChunkGenerator {

    public NEServerGroundChunkGenerator() {
        registerObjectGenerator(new FoliageGenerator());
    }

    @Override
    protected void generate_objects(int i, int j, WorldTile tile, Random chunk_random) {

        for(ObjectGenerator objGen: chunkObjectGenerators){
            //WRONG?
            objGen.generate_object(i, j, tile, chunk_random);
        }

        //FoliageGenerator testGen = new FoliageGenerator();
        //testGen.setEnvironment(environment);
       // testGen.add_tree(i, j);
    }

    @Override
    protected GenericNETile build_chunk_tile(int x, int y, Random chunk_random){

        int tile_id = 0;
        int height = Terrain.get_height(x, y);

        if (height > 120){
            tile_id = 25;
        }

        GenericNETile tile = new GenericNETile();
        Point origin = new Point(x,y);
        tile.origin = origin;
        //important!
        //tile should be registered before any action is performed on it
        getLayer().set_tile(origin, tile);
        tile.set_height(height);

        if (Terrain.is_lake(tile)){
            tile.set_tile_id(1);
            tile.terrain_type = WorldTile.TerrainType.TERRAIN_WATER;
        }

        return tile;
    }

    @Override
    protected void afterGenerateChunk(int i, int j) {

        FoliageGenerator testGen = new FoliageGenerator();
        testGen.setEnvironment(environment);
        testGen.add_tree(i*32 + 10, j*32 + 10);

    }


}
