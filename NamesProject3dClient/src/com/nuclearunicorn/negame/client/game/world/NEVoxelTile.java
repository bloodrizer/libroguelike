package com.nuclearunicorn.negame.client.game.world;

import com.nuclearunicorn.libroguelike.game.world.WorldTile;
import com.nuclearunicorn.negame.common.world.GenericNETile;

/**
 * Voxel utility data wraper for tile object
 * Contains various render information (edge visibility, texture id's etc)
 */
public class NEVoxelTile extends GenericNETile{
    public boolean lv, rv, tv, bv, fv, kv;

    public NEVoxelTile(){
        super();

        lv = rv = tv = bv = fv = kv = true;
    }
}
