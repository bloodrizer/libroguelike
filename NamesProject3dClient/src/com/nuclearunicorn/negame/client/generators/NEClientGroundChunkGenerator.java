package com.nuclearunicorn.negame.client.generators;

import com.nuclearunicorn.libroguelike.core.client.ClientGameEnvironment;
import com.nuclearunicorn.libroguelike.game.world.Terrain;
import com.nuclearunicorn.libroguelike.game.world.WorldTile;
import com.nuclearunicorn.libroguelike.game.world.WorldView;
import com.nuclearunicorn.negame.common.events.EGetChunkData;
import com.nuclearunicorn.negame.client.game.world.NEVoxelTile;
import com.nuclearunicorn.negame.common.generators.GenericGroundChunkGenerator;
import com.nuclearunicorn.negame.common.world.GenericNETile;
import org.lwjgl.util.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

/**
 */
public class NEClientGroundChunkGenerator extends GenericGroundChunkGenerator {

    final static Logger logger = LoggerFactory.getLogger(NEClientGroundChunkGenerator.class);

    @Override
    protected void generate_objects(int i, int j, WorldTile tile, Random chunk_random) {

    }

    @Override
    protected GenericNETile build_chunk_tile(int i, int j, Random chunk_random){

        int tile_id = 0;
        int height = Terrain.get_height(i,j);

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

    @Override
    protected void afterGenerateChunk(int x, int y) {
        /**
         * After client-side chunk generation we must connect server and download a stream of entity data.
         *
         * EGetChunkData will trigger a chain of ENetworkEntitySpawn messages from server.
         * We must perform this action every time when chunk is generated, as we do not cache entities on client and unload them on any accasion
         */

        logger.info("Requesting entity data from server from generated chunk @{},{}", x, y);

        EGetChunkData getChunkData = new EGetChunkData(x, y);
        getChunkData.setManager(environment.getEventManager());
        //getChunkData.setManager(ClientGameEnvironment.getEnvironment().getEventManager());

        getChunkData.post();

    }
}
