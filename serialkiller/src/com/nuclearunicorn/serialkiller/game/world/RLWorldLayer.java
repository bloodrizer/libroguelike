package com.nuclearunicorn.serialkiller.game.world;

import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.game.ent.controller.BaseController;
import com.nuclearunicorn.libroguelike.game.world.WorldChunk;
import com.nuclearunicorn.libroguelike.game.world.WorldCluster;
import com.nuclearunicorn.libroguelike.game.world.WorldTile;
import com.nuclearunicorn.libroguelike.game.world.layers.WorldLayer;
import com.nuclearunicorn.libroguelike.utils.pathfinder.astar.Mover;
import com.nuclearunicorn.serialkiller.game.world.entities.EntDoor;
import com.nuclearunicorn.serialkiller.game.world.entities.EntFurniture;
import com.nuclearunicorn.serialkiller.game.world.entities.EntRLActor;
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


    public RLWorldLayer(){
        tile_map = new RLWorldModelTileMap(this);   //inject rl-aware tilemap; todo: move to constructor in WorldLayer
    }

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

    @Override
    public void update() {
        super.update();

        int x = WorldCluster.origin.getX()*WorldChunk.CHUNK_SIZE;
        int y = WorldCluster.origin.getY()*WorldChunk.CHUNK_SIZE;
        int size = WorldCluster.CLUSTER_SIZE*WorldChunk.CHUNK_SIZE;

        for (int i = x; i<x+size; i++)
            for (int j = y; j<y+size; j++){
                int chunk_x = (int)Math.floor((float)i / WorldChunk.CHUNK_SIZE);
                int chunk_y = (int)Math.floor((float)j / WorldChunk.CHUNK_SIZE);

                if (get_cached_chunk(
                        chunk_x,
                        chunk_y) != null)
                {
                    WorldTile tile = get_tile(i,j);
                    tile.update();
                }
            }
    }

    //------------------------- pathfinding -----------------------------

    public static class RLWorldModelTileMap extends WorldLayer.WorldModelTileMap{

        public RLWorldModelTileMap(WorldLayer layer){
            super(layer);
        }

        @Override
        public boolean blocked(Mover mover, int x, int y) {

            RLTile tile = getTile(x, y);
            if (tile == null){
                return true;
            }

            return tile.isWall();   //tile.isBlocked() ||

        }

        @Override
        public int getScaleFactor() {
            return 2;
        }
        
        @Override
        public float getCost(Mover mover, int sx, int sy, int tx, int ty) {
            
            Entity owner = ((BaseController)mover).getOwner();
            RLTile tile = getTile(sx, sy);

            EntRLActor actor = (EntRLActor)tile.get_actor();
            if (actor == null){
                if (tile.owners.contains(owner)){
                    return 1/getScaleFactor();
                }else{
                    return 4/getScaleFactor();
                }
            }
            if ( actor instanceof EntDoor ){
                return 1/getScaleFactor();
            }
            if ( actor instanceof EntFurniture){    //TODO: check EntWindow there
                return 30/getScaleFactor();
            }

            return 60/getScaleFactor(); //some unknown actor type, better not touch

            //TODO: calculate different terrain types there
        }

        @Override
        public int getWidthInTiles() {
            return 128;
        }

        @Override
        public int getHeightInTiles() {
            return 128;
        }

        private RLTile getTile(int x, int y){
            temp.setLocation(x,y);
            temp = local2world(temp);

            RLTile tile = (RLTile)layer.get_tile(temp.getX(), temp.getY());
            return tile;
        }

    }
}
