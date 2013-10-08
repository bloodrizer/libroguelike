/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.negame.server.game.world;

import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.game.world.WorldChunk;
import com.nuclearunicorn.libroguelike.game.world.layers.WorldLayer;
import com.nuclearunicorn.negame.server.cache.INECache;
import org.lwjgl.util.Point;

import java.util.List;


/**
 *
 * @author Administrator
 */
public class ServerWorldLayer extends WorldLayer {

    INECache<Point, WorldChunk> neCache = null;

    @Override
    public synchronized WorldChunk get_cached_chunk(int chunk_x, int chunk_y){
        Point chunkOrigin = getLightweightPoint(chunk_x, chunk_y);

        //WorldChunk cachedChunk = neCache.get(util_point);
        
        WorldChunk cachedChunk;

        //try to fast-access to the local storage
        cachedChunk = chunk_data.get(chunkOrigin);
        if (cachedChunk != null){
            return cachedChunk;
        }else{
            cachedChunk = neCache.get(chunkOrigin);  //if no chunk generated, retrieve it from the cache
        }

        //if no chunk in cache, precache it
        if (cachedChunk == null) {
            return precache_chunk(chunk_x, chunk_y);   //this will aslo put chunk into local storage
        }else{
            //TODO: this part is confusing, move to the retrieveCachedChunk
            List<Entity> entList = cachedChunk.getEntList();
            for (Entity ent: entList){
                model.getEnvironment().getEntityManager().add(ent, z_index);
            }
        }


        returnLightweightPoint(chunkOrigin);

        return cachedChunk;
    }

    @Override
    public synchronized void chunk_gc() {
        //check if this chunk was not used for a long amt of time,
        //far from any player,  or we running low on memory
    }

    public void unloadChunk() {
        //save chunk data (region standings or smth. like this)
        //1. save chunk entities
        //2. unload them
        //3. unload the chunk
    }

    protected WorldChunk precache_chunk(int x, int y){
        WorldChunk chunk = new WorldChunk(x, y);

        chunk_data.put(new Point(x,y), chunk);
        process_chunk(chunk, z_index);

        return chunk;
    }

    public INECache<Point, WorldChunk> getNeCache() {
        return neCache;
    }

    public void setNeCache(INECache<Point, WorldChunk> neCache) {
        this.neCache = neCache;
    }

}
