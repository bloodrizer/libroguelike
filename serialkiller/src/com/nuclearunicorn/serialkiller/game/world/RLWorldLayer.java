package com.nuclearunicorn.serialkiller.game.world;

import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.game.world.WorldChunk;
import com.nuclearunicorn.libroguelike.game.world.layers.WorldLayer;
import org.lwjgl.util.Point;

import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: Administrator
 * Date: 09.03.12
 * Time: 14:55
 * To change this template use File | Settings | File Templates.
 */
public class RLWorldLayer extends WorldLayer {

    @Override
    protected WorldChunk precache_chunk(int x, int y){
        WorldChunk chunk = new RLWorldChunk(x, y){
            @Override
            public synchronized void unload(){
                System.out.println("unloading chunk @"+origin.toString());
                System.out.println("trying to remove " + Integer.toString( entList.size() ) +" entities");

                for (Iterator iter = entList.iterator(); iter.hasNext();) {
                    Entity ent = (Entity) iter.next();
                    getEntManager().remove_entity(ent);
                    iter.remove();
                }
                System.out.println(Integer.toString( entList.size() ) +" entities left");
            }
        };

        chunk_data.put(new Point(x,y), chunk);
        process_chunk(chunk, z_index);

        return chunk;
    }

}
