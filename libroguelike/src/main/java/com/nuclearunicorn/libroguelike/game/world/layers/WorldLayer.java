/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.game.world.layers;

import com.nuclearunicorn.libroguelike.events.EEntityChangeChunk;
import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.game.ent.EntityManager;
import com.nuclearunicorn.libroguelike.game.world.*;
import com.nuclearunicorn.libroguelike.game.world.generators.ChunkGenerator;
import com.nuclearunicorn.libroguelike.game.world.generators.WorldGenerationException;
import com.nuclearunicorn.libroguelike.utils.Timer;
import com.nuclearunicorn.libroguelike.utils.pathfinder.astar.Mover;
import com.nuclearunicorn.libroguelike.utils.pathfinder.astar.TileBasedMap;
import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.StackObjectPool;
import org.lwjgl.util.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.*;

/**
 *
 * @author Administrator
 */

/*
 * This class handles world terrain data, including landscape, moisture, lightning model, object distribution, minerals, etc.
 */
public class WorldLayer implements Serializable {

    final static Logger logger = LoggerFactory.getLogger(WorldLayer.class);

    protected transient WorldModel model;

    public static final int GROUND_LAYER = 0;
    protected int z_index;

    static final int MAP_SIZE = WorldCluster.CLUSTER_SIZE*WorldChunk.CHUNK_SIZE;
    public transient WorldModelTileMap tile_map;

    protected StackObjectPool<Point> objectPool = null;

    private static boolean light_outdated = false;  //shows if model should rebuild terrain lightning
    protected static boolean terrain_outdated = false;  //shows if model should rebuild terrain lightning

    //--------------------------------------------------------------------------
    public Map<Point,WorldChunk> chunk_data = new java.util.HashMap<Point,WorldChunk>(100);
    private List<ChunkGenerator> generators = new ArrayList<ChunkGenerator>();

    public WorldLayer(){
        tile_map = new WorldModelTileMap(this);

        PoolableObjectFactory<Point> poolFactory = new BasePoolableObjectFactory<Point>(){
            public Point makeObject(){
                return new Point(0,0);
            }

            @Override
            public void passivateObject(Point point) throws Exception { point.setLocation(0, 0);}
        };
        objectPool = new StackObjectPool<Point>(poolFactory);
    }

    public void registerGenerator(ChunkGenerator generator){
        generators.add(generator);
    }
    
    public Point getLightweightPoint(){
        try {
            Point point = objectPool.borrowObject();
            return point;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to borrow lightweight object from the pool");
            //return new Point(0,0);
        }
    }
    
    public Point getLightweightPoint(int x, int y){
        Point point = getLightweightPoint();
        point.setLocation(x, y);
        
        return point;
    }
    
    public void returnLightweightPoint(Point point){
        try {
            objectPool.returnObject(point);
        } catch (Exception ex) {
            logger.error("Failed to return lightweight object from the pool", ex);
        }    
    }

    public Map<Point,WorldTile> getTileData(Point tileOrigin, boolean restrictOutOfBounds){

        Point chunkOrigin = WorldChunk.get_chunk_coord(tileOrigin);      

        WorldChunk chunk = get_cached_chunk(chunkOrigin, restrictOutOfBounds);

        if (chunk != null){
            return chunk.tile_data;
        }
        return null;
    }


    public void setModel(WorldModel model){
        this.model = model;
    }
    
    protected EntityManager getEntManager(){
        return model.getEnvironment().getEntityManager();
    }

    /**
     * WARNING, THIS METHOD WILL LAZY INITIALIZE WHOLE CHUNK ON ACCESS
     * BE EXEREMELY CAREFUL WHILE USING IT
     * @param origin
     * @param tile
     */
    public void set_tile(Point origin, WorldTile tile){
        setTile(origin, tile, true);
    }

    public void setTile(Point origin, WorldTile tile, boolean checkOOC){
        Map<Point,WorldTile> tileData = getTileData(origin, checkOOC);
        //if called OOC (out of cluster), this will return null chunk data
        if (tileData != null){
            tileData.put(origin, tile);
        }
        //TODO: show warning
    }

    public static void invalidate_light(){
        light_outdated = true;
    }

    //a bit more faster version of getTile - no tile calculation presents
    public WorldTile getTile(WorldChunk chunk, int x, int y){
        Point tileOrigin = getLightweightPoint(x, y);
        WorldTile tile = chunk.tile_data.get(tileOrigin);

        returnLightweightPoint(tileOrigin);
        return tile;
    }

    public WorldTile get_tile(int x, int y){
        return getTile(x, y, true);
    }

    public WorldTile getTile(int x, int y, boolean restrictOutOfBounds){
        Point tileOrigin = getLightweightPoint(x, y);

        Map<Point,WorldTile> tileData = getTileData(tileOrigin, restrictOutOfBounds);
        if (tileData == null){
            return null;
        }
        WorldTile tile = tileData.get(tileOrigin);
        returnLightweightPoint(tileOrigin);

        return tile;
    }

    public WorldTile getTile(Point tileOrigin){

        WorldTile tile = null;
        Map<Point,WorldTile> tileData = getTileData(tileOrigin, true);
        if (tileData!= null){
            tile = tileData.get(tileOrigin);
        }

        return tile;
    }


    public WorldChunk get_cached_chunk(int chunk_x, int chunk_y){
        return get_cached_chunk(chunk_x, chunk_y, true);
    }

    /**
     *
     * @param chunk_x
     * @param chunk_y
     * @param restrictOutOfBounds
     *  If true, basic bounds check will be performed on current game cluster.
     *  If chunk is not in the cluster, it will be not retrieved
     * @return
     */
    public WorldChunk get_cached_chunk(int chunk_x, int chunk_y, boolean restrictOutOfBounds){
        Point chunkOrigin = getLightweightPoint(chunk_x, chunk_y);
        WorldChunk chunk = chunk_data.get(chunkOrigin);

        if (chunk != null){
            return chunk;
        }else {

            if( WorldCluster.chunk_in_cluster(chunkOrigin) || !restrictOutOfBounds){
                return precache_chunk(chunk_x, chunk_y);
            }

            //this is not an actuall error
            //logger.error("Chunk origin @" + "[" + chunk_x + "," + chunk_y + "] is out of cluster bounds @" + WorldCluster.origin + "+/-" + WorldCluster.CLUSTER_SIZE);
            return null;
        }
    }

    public WorldChunk get_cached_chunk(Point location){
        return get_cached_chunk(location.getX(),location.getY(), true);
    }

    public WorldChunk get_cached_chunk(Point location, boolean restrictOutOfBounds){
        return get_cached_chunk(location.getX(),location.getY(), restrictOutOfBounds);
    }

    public void update(){
        
        ArrayList<Entity> entList = getEntManager().getList(z_index);
        Object[] entArray = entList.toArray();
        
        for(int i=entList.size()-1; i>=0; i--){
            Entity entity = (Entity)entArray[i];

            entity.update();

            if (entity.is_awake(Timer.get_time())){
                  entity.think();
            }
            if (entity.is_next_frame(Timer.get_time())){
                  entity.next_frame();
            }

            if (entity.is_garbage()){
                getEntManager().remove_entity(entity);
                entity.tile.remove_entity(entity);
            }
        }


        //here comes tricky part - recalculate light emission
        if (light_outdated){
            recalculate_light();
            light_outdated = false;
        }

        if (terrain_outdated){
            update_terrain();
            terrain_outdated = false;
        }
        
    }

    public void recalculate_light(){


        Object[] ent_list = getEntManager().getList(z_index).toArray();

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

                 if (tile != null){
                     tile.light_level = 0.0f;

                     for(int e_id=0; e_id<ent_list.length; e_id++){
                         Entity entity = (Entity)ent_list[e_id];
                         if (entity.light_amt > 0.0f){
                            tile.light_level += get_light_amt(i,j,entity);
                         }
                     }
                 }
             }
        }
    }

    /*
     * This function returns light ammount, casted by entity 'ent' at the tile
     */
    private float get_light_amt(int x, int y, Entity ent){
        int dx = ent.origin.getX()-x;
        int dy = ent.origin.getY()-y;
        //float disst = (float)Math.sqrt(dx*dx+dy*dy);
        float disst = dx*dx+dy*dy;

        return ent.light_amt / disst;

        //return 0.1f;
    }
    
    /*
     * Put new chunk with set (x,y) point index and fills it with terrain data
     * Use ground layer as default for compatibility reason
     * Note that we probably need to build every layer in this chunk(?)
     */

    protected WorldChunk precache_chunk(int x, int y){
        WorldChunk chunk = new WorldChunk(x, y){
            @Override
            public synchronized void unload(){
                //System.out.println("unloading chunk @"+origin.toString());
                //System.out.println("trying to remove " + Integer.toString( entList.size() ) +" entities");

                for (Iterator iter = entList.iterator(); iter.hasNext();) {
                        Entity ent = (Entity) iter.next();
                        getEntManager().remove_entity(ent);
                        iter.remove();
                }
                //System.out.println(Integer.toString( entList.size() ) +" entities left");
            }
        };

        chunk_data.put(new Point(x,y), chunk);
        process_chunk(chunk, z_index);

        return chunk;
    }

    public void process_chunk(WorldChunk chunk, int z_index){
        buildChunk(chunk, z_index);

        /*
         * TODO: We can not simply load one region player into one,
         * since the map will look dull and empty
         *
         * We should load large portion of regions, at least 3x3 blocks
         */

        int rx = chunk.origin.getX()/ WorldRegion.REGION_SIZE;
        int ry = chunk.origin.getY()/WorldRegion.REGION_SIZE;
        Point regionOrigin = new Point(rx,ry);
        WorldModel.worldRegions.get_cached(regionOrigin);

    }

    protected void buildChunk(WorldChunk chunk, int z_index){

        logger.debug("building chunk @{}", chunk.origin);

        if (model.getEnvironment() == null){
            throw new WorldGenerationException("model environment is null on WorldLayer");
        }

        if(generators.isEmpty()){
            throw new WorldGenerationException("failed to generate layer chunk - no generators registered");
        }

        for(ChunkGenerator gen: generators){
            gen.setEnvironment(model.getEnvironment());
            gen.set_zindex(z_index);
            gen.generate(chunk);
        }

        terrain_outdated = true;
    }
    


    //clean all unused chunks and data
    public synchronized void chunk_gc(){
        for (Iterator<Map.Entry<Point, WorldChunk>> iter = chunk_data.entrySet().iterator();
            iter.hasNext();) {
            Map.Entry<Point, WorldChunk> entry = iter.next();

            WorldChunk __chunk = (WorldChunk)entry.getValue();

            if (!WorldCluster.chunk_in_cluster(__chunk.origin)){
                __chunk.unload();
                iter.remove();
            }
        }
    }
    




    public void move_entity(Entity entity, Point coord_dest){
        //System.err.println(model.getName()+" is changing coordinates for entity "+entity.getName()+"("+entity.get_uid()+")");

        Point coord_from = new Point(entity.origin);  //defence copy
        
        //System.out.println("world model::on entity move to:"+coord_dest.toString());
        entity.origin.setLocation(coord_dest);

        if (entity.light_amt > 0.0f){
            invalidate_light();
        }

        //now with a chunk shit
        //----------------------------------------------------------------------
        WorldChunk new_chunk = get_cached_chunk(WorldChunk.get_chunk_coord(coord_dest));
        if (new_chunk != null && !entity.in_chunk(new_chunk)){
            logger.info("World model '{}' is changing chunk for entity {}", model.getName(), entity.getName() );

            WorldChunk ent_chunk = entity.get_chunk();
            //todo: move to event dispatcher?
            if(ent_chunk != null ){
                ent_chunk.remove_entity(entity);
            }

            new_chunk.add_entity(entity);
            //todo end

            //------------------------------------------------------------------
            EEntityChangeChunk e_change_chunk = new EEntityChangeChunk(entity,ent_chunk,new_chunk);
            e_change_chunk.setManager(model.getEnvironment().getEventManager());
            e_change_chunk.post();
            //----------------------------------------------------------------------
        }
        //now, after we successfully performed chunk routine,
        //set valid entity pointers in chunks
        WorldTile tile_from = get_tile(coord_from.getX(), coord_from.getY());
        WorldTile tile_to   = get_tile(coord_dest.getX(), coord_dest.getY());

        tile_from.remove_entity(entity);
        tile_to.add_entity(entity);

        //----------------------------------------------------------------------
    }

    /*
     * update_terrain is called whenever player_ent crosses a border of terrain
     and new portion of terrain generation is required
     *
     */

    public void update_terrain() {
        /*Terrain.heightmap_cached.clear();
        //System.out.println("clearing aquatic tiles data");
        //Terrain.aquatic_tiles.clear();

        GameUI ui = (GameUI)(Game.get_game_mode().get_ui());
        
        if(ui.minimap != null){
            ui.minimap.expired = true;
            ui.minimap.update_map();
        }*/
    }

    public void set_zindex(int z_index) {
        this.z_index = z_index;
    }

    public int get_zindex() {
        return z_index;
    }

    public void reset() {
        chunk_data.clear();
        generators.clear();   //kinda ok, but not sure
    }



    /*
     *  WorldModelTileMap is a mediator between WorldModel and AStarPathfinder
     *
     *  it's a relatively small tilemap, that is using 0,0 - MAP_SIZE,MAP_SIZE local coord
     *  for fast path calculation
     *
     *
     *
     */

    public static class WorldModelTileMap implements TileBasedMap {

        protected WorldLayer layer = null;
        protected Point temp = new Point(0,0);

        public WorldModelTileMap(WorldLayer layer){
            this.layer = layer;
        }

        public void pathFinderVisited(int x, int y) {
            //visited[x][y] = true;
        }


        public int getWidthInTiles() {
            return MAP_SIZE;
        }

        public int getHeightInTiles() {
            return MAP_SIZE;
        }

        public boolean blocked(Mover mover, int x, int y) {
            //todo: check the mover type
            temp.setLocation(x,y);
            temp = local2world(temp);

            WorldTile tile = layer.get_tile(temp.getX(), temp.getY());
            if (tile == null){
                return true;
            }

            return tile.isBlocked();

            //todo: check border collision
        }

        public float getCost(Mover mover, int sx, int sy, int tx, int ty) {
            return 1;
            //TODO: calculate different terrain types there
        }

        @Override
        public int getScaleFactor() {
            return 1;
        }

        static Point origin = new Point(0,0);
        public synchronized Point world2local(Point world){
            origin.setLocation(
                    WorldCluster.origin.getX()*WorldChunk.CHUNK_SIZE,
                    WorldCluster.origin.getY()*WorldChunk.CHUNK_SIZE);

            world.setLocation(
                    world.getX()-origin.getX(),
                    world.getY()-origin.getY()
            );
            return world;
        }

        public synchronized Point local2world(Point world){
            origin.setLocation(
                    WorldCluster.origin.getX()*WorldChunk.CHUNK_SIZE,
                    WorldCluster.origin.getY()*WorldChunk.CHUNK_SIZE);

            world.setLocation(
                    world.getX()+origin.getX(),
                    world.getY()+origin.getY()
            );
            return world;
        }
    }
}
