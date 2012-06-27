package com.nuclearunicorn.negame.client.game.world;

import com.nuclearunicorn.libroguelike.game.world.layers.WorldLayer;
import com.nuclearunicorn.negame.client.render.TilesetVoxelRenderer;

/**
 * Created by IntelliJ IDEA.
 * User: bloodrizer
 * Date: 27.06.12
 * Time: 17:16
 * To change this template use File | Settings | File Templates.
 */
public class NEWorldLayer extends WorldLayer {

    @Override
    public void update_terrain() {
        super.update_terrain();

        System.out.println("invalidating layer geometry...");

        //terrain data invaludated, we need to rebuild voxel data
        //probably use static variable is wrong decision
        TilesetVoxelRenderer.invalidateGeometry();
    }
}
