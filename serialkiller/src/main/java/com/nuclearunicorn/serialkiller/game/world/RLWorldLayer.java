package com.nuclearunicorn.serialkiller.game.world;

import com.nuclearunicorn.libroguelike.core.client.ClientGameEnvironment;
import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.game.ent.controller.BaseController;
import com.nuclearunicorn.libroguelike.game.items.ItemEnt;
import com.nuclearunicorn.libroguelike.game.world.WorldChunk;
import com.nuclearunicorn.libroguelike.game.world.WorldCluster;
import com.nuclearunicorn.libroguelike.game.world.WorldTile;
import com.nuclearunicorn.libroguelike.game.world.layers.WorldLayer;
import com.nuclearunicorn.libroguelike.utils.pathfinder.astar.Mover;
import com.nuclearunicorn.serialkiller.game.world.entities.*;
import com.nuclearunicorn.serialkiller.game.world.entities.EntityFurniture;
import com.nuclearunicorn.serialkiller.game.world.entities.EntityRLActor;
import com.nuclearunicorn.serialkiller.render.RLMessages;
import org.lwjgl.util.Point;
import org.newdawn.slick.Color;

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

    @Override
    public void move_entity(Entity entity, Point coordDest) {
        super.move_entity(entity, coordDest);
        RLTile rlTile = (RLTile)getTile(coordDest);
        
        if (rlTile.has_ent(ItemEnt.class)){
            ItemEnt itemEnt = (ItemEnt)rlTile.getEntity(ItemEnt.class);

            entity.getContainer().add_item(itemEnt.get_item());
            rlTile.remove_entity(itemEnt);
            ClientGameEnvironment.getEntityManager().remove_entity(itemEnt);
            
            String countPostfix = "";
            if (itemEnt.get_item().get_count() > 1){
                countPostfix = "("+itemEnt.get_item().get_count()+")";
            }

            if (entity.isPlayerEnt()){
                RLMessages.message("Player has picked up " + itemEnt.get_item().get_type() + " " + countPostfix , Color.lightGray);
            }
        }
    }

    //------------------------- pathfinding -----------------------------

    public static class RLWorldModelTileMap extends WorldLayer.WorldModelTileMap{

        public RLWorldModelTileMap(WorldLayer layer){
            super(layer);
        }

        /**
         * What a route may not cross. Walls and anything solid that will not move on its own -
         * a tree, a crate, a window, a shut door.
         *
         * <p>This used to answer {@code tile.isWall()} alone, with solid furniture expressed as
         * mere <i>cost</i>. That made every window a legal one-tile shortcut into a house whose
         * door was on the far side, so A* deliberately routed residents through the glass; the
         * step then failed, the obstacle handler broke whatever was in the way, and the shortcut
         * became real. NPCs burgled their own homes nightly and the town lost its windows.
         *
         * <p>People stay passable on purpose - see {@link RLTile#isPathBlocked()}.
         */
        @Override
        public boolean blocked(Mover mover, int x, int y) {

            RLTile tile = getTile(x, y);
            if (tile == null){
                return true;
            }

            return tile.isPathBlocked();
        }

        @Override
        public int getScaleFactor() {
            return 2;
        }

        /**
         * Preference among the tiles that are passable at all. Your own floor is free, a
         * doorway is cheap, and someone standing in the way costs enough to walk around them
         * when there is room and not enough to call the route impossible when there is not.
         */
        @Override
        public float getCost(Mover mover, int sx, int sy, int tx, int ty) {

            Entity owner = ((BaseController)mover).getOwner();
            RLTile tile = getTile(tx, ty);
            if (tile == null){
                return COST_OPEN / getScaleFactor();
            }

            Entity actor = tile.get_actor();
            if (actor != null && actor.is_blocking() && actor.controller != null){
                return COST_OCCUPIED / getScaleFactor();   //a person: wait or go around, never a wall
            }
            if (actor instanceof EntityDoor){
                return COST_DOOR / getScaleFactor();
            }
            if (tile.owners.contains(owner)){
                return COST_HOME / getScaleFactor();
            }
            return COST_OPEN / getScaleFactor();
        }

        //Ratios, not absolutes: COST_OCCUPIED/COST_OPEN is how far an NPC detours around a
        //person before deciding to queue behind them instead.
        private static final float COST_HOME = 1f;
        private static final float COST_DOOR = 2f;
        private static final float COST_OPEN = 4f;
        private static final float COST_OCCUPIED = 16f;

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
