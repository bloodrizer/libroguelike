package com.nuclearunicorn.negame.client.generators;

import com.nuclearunicorn.libroguelike.game.GameEnvironment;
import com.nuclearunicorn.libroguelike.game.world.Terrain;
import com.nuclearunicorn.libroguelike.game.world.WorldChunk;
import com.nuclearunicorn.libroguelike.game.world.WorldTile;
import com.nuclearunicorn.libroguelike.game.world.WorldView;
import com.nuclearunicorn.libroguelike.game.world.generators.ChunkGenerator;
import com.nuclearunicorn.libroguelike.game.world.layers.WorldLayer;
import com.nuclearunicorn.libroguelike.utils.NLTimer;
import com.nuclearunicorn.negame.client.game.world.NEVoxelTile;
import com.nuclearunicorn.negame.common.generators.GenericGroundChunkGenerator;
import com.nuclearunicorn.negame.common.world.GenericNETile;
import org.lwjgl.util.Point;

import java.util.Random;

/**
 */
public class NEGroundChunkGenerator extends GenericGroundChunkGenerator {

    @Override
    protected void generate_objects(int i, int j, WorldTile tile, Random chunk_random) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
        protected GenericNETile build_chunk_tile(int i, int j, Random chunk_random){

            int tile_id = 0;
            int height = Terrain.get_height(i,j);
            //System.out.println("HEIGHT@" + i + "," + j + ":" + height);

            if (height > 120){
                tile_id = 25;
            }

            NEVoxelTile tile = new NEVoxelTile();
            Point origin = new Point(i,j);
            tile.origin = origin;

            //important!
            //tile should be registered before any action is performed on it
            getLayer().set_tile(origin, tile);
            //isn't it already registered?



            tile.set_height(height);

            if (Terrain.is_lake(tile)){
                tile.set_tile_id(1);
                tile.terrain_type = WorldTile.TerrainType.TERRAIN_WATER;
            }

            //Voxel side visibility calculation

            //todo: fix invisible parts of the tile of leftTile offset>tile offset

            NEVoxelTile leftTile = (NEVoxelTile) getLayer().get_tile(i-1, j);
            if (leftTile != null && WorldView.getYOffset(leftTile) == WorldView.getYOffset(tile)){
                leftTile.rv = false;
                tile.lv = false;
            }

            NEVoxelTile topTile = (NEVoxelTile) getLayer().get_tile(i, j-1);
            if (topTile != null && WorldView.getYOffset(topTile) == WorldView.getYOffset(tile)){
                topTile.fv = false;
                tile.kv = false;
            }

            return tile;
        }
}
