/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.negame.server.game.world;

import com.nuclearunicorn.libroguelike.game.world.WorldChunk;
import com.nuclearunicorn.libroguelike.game.world.WorldModel;
import com.nuclearunicorn.libroguelike.game.world.layers.WorldLayer;
import com.nuclearunicorn.negame.server.cache.BaseNECache;
import com.nuclearunicorn.negame.server.cache.INECache;
import org.lwjgl.util.Point;


/**
 *
 * @author Administrator
 *
 *
 * Server's WorldModel. Can save and load in-game data
 *
 */

public class ServerWorldModel extends WorldModel {

    INECache<Point, WorldChunk> neCache = null;

    public ServerWorldModel(){

        System.out.println("Creating chunk cache...");

        neCache = new BaseNECache<Point, WorldChunk>();

        //do some sql lite initialization there
        for (int i = 0; i< LAYER_COUNT; i++ ){
            WorldLayer layer = new ServerWorldLayer();
            ((ServerWorldLayer)layer).setModel(ServerWorldModel.this);
            ((ServerWorldLayer) layer).setNeCache(neCache);

            layer.set_zindex(i);
            worldLayers.put(i, layer);
        }
    }
}

//TODO: to be investigated - ehcache, Berkeley DB, DirectMemory/Redis