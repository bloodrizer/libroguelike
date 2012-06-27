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
        push_point(util_point);
        util_point.setLocation(chunk_x, chunk_y);

        WorldChunk cachedChunk = neCache.get(util_point);
        
        if (cachedChunk == null){
            return precache_chunk(chunk_x, chunk_y);
        }

        if (!chunk_data.containsKey(util_point)){
            //register chunk objects into the global entity pool
            List<Entity> entList = cachedChunk.getEntList();
            
            for (Entity ent: entList){
                model.getEnvironment().getEntityManager().add(ent, z_index);
            }
        }


        pop_point(util_point);

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
        neCache.put(new Point(x,y), chunk);

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
