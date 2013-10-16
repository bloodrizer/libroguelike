package com.nuclearunicorn.negame.client.game.world;

import com.nuclearunicorn.libroguelike.game.world.layers.WorldLayer;
import com.nuclearunicorn.negame.client.render.TilesetVoxelRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NEWorldLayer extends WorldLayer {

    final static Logger logger = LoggerFactory.getLogger(NEWorldLayer.class);

    @Override
    public void update_terrain() {
        super.update_terrain();

        logger.debug("invalidating layer geometry...");
        /*
            terrain data invaludated, we need to rebuild voxel data
            probably use static variable is wrong decision
         */
        TilesetVoxelRenderer.invalidateGeometry();
    }
}
