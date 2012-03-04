/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.game.world;

import com.nuclearunicorn.libroguelike.core.Input;
import com.nuclearunicorn.libroguelike.core.client.ClientEventManager;
import com.nuclearunicorn.libroguelike.core.client.ClientGameEnvironment;
import com.nuclearunicorn.libroguelike.events.EMouseDrag;
import com.nuclearunicorn.libroguelike.events.EMouseRelease;
import com.nuclearunicorn.libroguelike.events.Event;
import com.nuclearunicorn.libroguelike.events.IEventListener;
import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.game.world.layers.WorldLayer;
import com.nuclearunicorn.libroguelike.render.EntityRenderer;
import com.nuclearunicorn.libroguelike.render.TilesetRenderer;
import com.nuclearunicorn.libroguelike.render.WindowRender;
import com.nuclearunicorn.libroguelike.render.layers.GroundLayerRenderer;
import com.nuclearunicorn.libroguelike.render.layers.LayerRenderer;
import com.nuclearunicorn.libroguelike.utils.Noise;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.Point;
import org.lwjgl.util.vector.Vector3f;

import java.util.Iterator;

import static org.lwjgl.opengl.GL11.glLoadIdentity;

/**
 *
 * @author bloodrizer
 */
public class WorldView implements IEventListener {



    public class TextureTransition {
        public boolean[] nb = new boolean[8];  //n, w, e, s, nw, ns, ew, es
    }

    public static Point highlited_tile = null;
    private WorldModel model;
    public static boolean DRAW_GRID = false;

    public WorldView(WorldModel model) {
        ClientEventManager.eventManager.subscribe(this);
        this.model = model;
    }

    public static void highlight_tile(Point tile_coord) {
        highlited_tile = tile_coord;
    }

    
    //returns z-index of current terrain layer
    private static int view_z_index = WorldLayer.GROUND_LAYER;
    public static void set_zindex(int z_index){
        if (z_index<0){ view_z_index = 0; }
        else if(z_index > WorldModel.LAYER_COUNT) {
            view_z_index = WorldModel.LAYER_COUNT;
        } else {
            view_z_index = z_index;
        }
    }
    
    public static int get_zindex(){
        return view_z_index;
    }

    private WorldLayer getLayer() {
        return ClientGameEnvironment.getWorldLayer(view_z_index);
    }


    public LayerRenderer getLayerRenderer(){
        return null;
    }


    /**
       Get current loaded world cluster and iterate every chunk of this cluster
       Get associated tile renderer and perform tile rendering

       This method contains a lot of tricky optimisations in order to improve FPS
       on a large / infinite-sized maps
     */
    public void render_layer(){
        
        LayerRenderer renderer = null;

        renderer = getLayerRenderer();

        int x = WorldCluster.origin.getX()*WorldChunk.CHUNK_SIZE;
        int y = WorldCluster.origin.getY()*WorldChunk.CHUNK_SIZE;
        int size = WorldCluster.CLUSTER_SIZE*WorldChunk.CHUNK_SIZE;
        
        WorldChunk currentChunk = new WorldChunk(0, 0);

        for (int i = x; i<x+size; i++)
        for (int j = y; j<y+size; j++)
            {

                if (!WorldViewCamera.tile_in_fov(i,j)){
                    continue;
                }
                //NOTE: get_cached_chink is now deprecated function, as it can load random chunk data without checking, if it's inside
                //of world cluster
                //world cluster should cache it instead

                //serious debug problems othervise
                int chunk_x = (int)Math.floor((float)i / WorldChunk.CHUNK_SIZE);
                int chunk_y = (int)Math.floor((float)j / WorldChunk.CHUNK_SIZE);

                //sunce chunk retrival is heavy operation, we will request it only if coords are changed
                if (currentChunk.origin.getX() != chunk_x && currentChunk.origin.getY() != chunk_y){
                    currentChunk = getLayer().get_cached_chunk(
                        chunk_x,
                        chunk_y);   //<---slooow
                }

                if ( currentChunk != null){
                    //WorldTile tile = getLayer().getTile(currentChunk, i,j);
                    WorldTile tile = getLayer().get_tile(i,j);

                    //render tile
                    if (renderer != null){
                        renderer.render_tile(tile, i, j);
                    }
                }

            }
    }

    public void render_entities(){
        for (Iterator iter = ClientGameEnvironment.getEntityManager().getList(view_z_index).iterator(); iter.hasNext();) {
           Entity entity = (Entity) iter.next();
           render_entity(entity);
        }
    }

    public static int getYOffset(WorldTile tile){
        if (tile != null){
            float y_offset = (int)((float)tile.get_height()/32.0f);
            return (int)(y_offset*20);
        }else{
            return 0;
        }
    }

    public void render_entity(Entity entity){
        //todo: use factory render

        //IGenericRender render = Render.get_render(entity);
        //render.render(entity);
        GL11.glColor3f(1.0f,1.0f,1.0f);

        WorldTile tile = getLayer().get_tile(
            entity.origin.getX(),
            entity.origin.getY()
        );


        float r, g, b;

        Vector3f tile_color = GroundLayerRenderer.get_tile_color(tile);
        r = tile_color.getX();
        g = tile_color.getY();
        b = tile_color.getZ();

        GL11.glColor3f(
            r,
            g,
            b
        );

        final int y_offset = WorldView.getYOffset(tile);

        EntityRenderer renderer = entity.get_render();
        renderer.render();  //render, lol
    }

    //--------------------------------------------------------------------------

    public void render(){

        WorldViewCamera.update();

        glLoadIdentity();
    
        WorldViewCamera.setMatrix();
  
        render_layer();
        render_entities();
        
        glLoadIdentity();

        //update_cursor();
    }

    public void update_cursor(){
        
        //Introduce cursor class
        //We may change cursor, if user action is now active

        /*if (Player.is_combat_mode()){
            Render.set_cursor("/render/ico_sword.png");
            return;
        }*/

        //Render.set_cursor("/render/ico_default.png");


        //--------------------main code---------------------
        int x = Mouse.getX();
        int y = Mouse.getY();

        Point tile_coord = WorldView.getTileCoord(x,y);
        WorldTile tile = getLayer().get_tile(tile_coord.getX(), tile_coord.getY());
        if(tile==null){
            return;
        }
        Entity ent = tile.get_actor();
        //---------------------end---------------------------
        

        /*if (ent != null && ent instanceof EntMonster ){
            Render.set_cursor("/render/ico_sword.png");
        }else{
            Render.set_cursor("/render/ico_default.png");
        }*/

    }



    public static Point getTileCoord(Point window_coord){
        int x = window_coord.getX();
        int y = window_coord.getY();
        return getTileCoord(x,y);
    }

    
    public static boolean ISOMETRY_MODE = true;
    public static float ISOMETRY_ANGLE = 45.0f;

    //public static float ISOMETRY_Y_SCALE = 0.6f;
    //public static float ISOMETRY_TILE_SCALE = 1.2f;
    public static float ISOMETRY_Y_SCALE = 0.6f;
    public static float ISOMETRY_TILE_SCALE = 1.0f;
    /*
     * Sprites are rendering in 1:1 proportion, but
     * tiles are rendering in 1:1.2 proportion, so
     * we use this hack to avoid insane recalculations.
     * See Tileset.java
     */
    public static float TILE_UPSCALE = 1.2f;

    //perform reverse isometric transformation
    //transform screen point into the world representation in isometric space

    public static int local2world_x(float x, float y){

        x = (x / ISOMETRY_TILE_SCALE);
        y = (y / ISOMETRY_Y_SCALE / ISOMETRY_TILE_SCALE);

        float world_x = x*(float)Math.sin(ISOMETRY_ANGLE * Noise.DEG_TO_RAD)
                    +y*(float)Math.cos(ISOMETRY_ANGLE * Noise.DEG_TO_RAD);

        return (int)world_x;
    }
    public static int local2world_y(float x, float y){
        
        x = (x / ISOMETRY_TILE_SCALE);
        y = (y / ISOMETRY_Y_SCALE / ISOMETRY_TILE_SCALE);

        float world_y = -x*(float)Math.cos(ISOMETRY_ANGLE * Noise.DEG_TO_RAD)
                    +y*(float)Math.cos(ISOMETRY_ANGLE * Noise.DEG_TO_RAD);

        return (int)world_y;
    }

    public static Point local2world(Point point){
        float x = point.getX();
        float y = point.getY();
        
        int world_x = local2world_x(x,y);
        int world_y = local2world_y(x,y);
        
        return new Point(world_x, world_y);
    }

    //--------------------------------------------------------------------------
    //                           World 2 Local
    //--------------------------------------------------------------------------
    public static int world2local_x(float x, float y){
        float local_x = x*(float)Math.cos(ISOMETRY_ANGLE * Noise.DEG_TO_RAD)
                    -y*(float)Math.sin(ISOMETRY_ANGLE * Noise.DEG_TO_RAD);
        
         local_x = local_x * ISOMETRY_TILE_SCALE;

         return (int)local_x;
    }
    public static int world2local_y(float x, float y){
        float local_y = x*(float)Math.sin(ISOMETRY_ANGLE * Noise.DEG_TO_RAD)
                    +y*(float)Math.cos(ISOMETRY_ANGLE * Noise.DEG_TO_RAD);
        
         local_y = local_y * ISOMETRY_Y_SCALE * ISOMETRY_TILE_SCALE;

         return (int)local_y;
    }


    //transform world x,y point into the screen isometric representation (rotate to the 45 angle and scale)
    public static Point world2local(Point point){
        float x = point.getX();
        float y = point.getY();

        int local_x = world2local_x(x,y);
        int local_y = world2local_y(x,y);



        return new Point( local_x, local_y);
    }
    
    /**
     *  Cast (mx,my) screen coords, given in the NE coord system
     *  without checking tile height modifiers
     */
    private static Point getTileCoordPlain(int mx, int my){
            int local_x = local2world_x(mx,my);
            int local_y = local2world_y(mx,my);
            //point = local2world(point);

            //--------------------------------------------
            //there is actually a hack there, but it works
            //--------------------------------------------
            int tile_x = local_x/ TilesetRenderer.TILE_SIZE;
            if (local_x<0){ tile_x = tile_x-1; }
            int tile_y = local_y/ TilesetRenderer.TILE_SIZE;
            if (local_y<0){ tile_y = tile_y-1; }
            //-----------------end of hack----------------

            Point point = new Point(tile_x,tile_y);

            return point;
    }

    /**
     * Cast (mx,my) scren coords given in the GL coord system
     * to the world tile coord, assuming every tile has different height
     */
    public static Point getTileCoord(int x, int y) {

        /**Step 1. First aproximation.
         *
         * Get tile as if height was not set at all
         * That help us to get relative area of the tiles we clicked at
         */

        y = WindowRender.get_window_h()-y;  //invert it wtf

            /* NE use following coord system, while gl use reverse y axis
               (0,0) - - (x,0)
             * |
             * |
             * (0,y)
             */

            x = x + (int)WorldViewCamera.camera_x;
            y = y + (int)WorldViewCamera.camera_y;

            
            Point point = getTileCoordPlain(x,y);
            int tile_x = point.getX();
            int tile_y = point.getY();

            /**Step 2.
             * Iterate all tiles at that region
             * Check each tile if our point is in their top region
             */

            for (int i = tile_x-3; i<tile_x+3; i++){
                for(int j = tile_y-3; j<tile_y+3; j++){
                    if (!WorldCluster.tile_in_cluster(i, j)) { continue; }

                    WorldLayer layer = ClientGameEnvironment.getWorldLayer(view_z_index);
                    WorldTile tile = layer.get_tile(i, j);
                    int y_offset = getYOffset(tile);

                    //adjust y value with height midifier
                    //replace 32 with actual magic constant based on tile sprite height
                    int y2 = y - y_offset;

                    Point point2 = getTileCoordPlain(x,y2);
                    if (point2.equals(tile.origin)){
                        return point2;
                    }
                }
            }
            return point;
    }

    /**
     * This method checks first, that point is in the tile sprite area
     * It checks then, if the point is in the upper part of the sprite - isometric tile square
     *
     */
    private static boolean pointInTile(WorldTile tile, int mx, int my){
        return false;
    }


    //todo: refact me
    float camera_x = 0;
    float camera_y = 0;

    //----------------------------EVENTS SHIT-----------------------------------
    public void e_on_event(Event event){
       
       /*System.out.println("WorldView - camera @ "+Float.toString(camera_x)+
                   ","+Float.toString(camera_y));*/

       if (event instanceof EMouseDrag){

           EMouseDrag drag_event = (EMouseDrag)event;
           if (drag_event.type == Input.MouseInputType.RCLICK){

               WorldViewCamera.follow_target = false;
            //camera_x += drag_event.dx*1.5;
            //camera_y += drag_event.dy*1.5;
               WorldViewCamera.move(drag_event.dx*1.5f,-drag_event.dy*1.5f);
           }


           

       }else if(event instanceof EMouseRelease){
           EMouseRelease drag_event = (EMouseRelease)event;
           if (drag_event.type == Input.MouseInputType.RCLICK){
                //camera_x = 0.0f;
                //camera_y = 0.0f;
               //WorldViewCamera.set(0.0f,0.0f);
               WorldViewCamera.follow_target = true;
           }
       }
    }
    //--------------------------------------------------------------------------
    public void e_on_event_rollback(Event event){
      
    }


    /**
     * Recieves screen coord of the entity based on the tile coord and a screen tile size
     */

    public static int get_tile_x_screen(Point origin){
        return world2local_x(
                (origin.getX())*TilesetRenderer.TILE_SIZE,
                (origin.getY())*TilesetRenderer.TILE_SIZE
        ) - (int)WorldViewCamera.camera_x;
    }

    public static int get_tile_y_screen(Point origin){
        return world2local_y(
                (origin.getX())*TilesetRenderer.TILE_SIZE,
                (origin.getY())*TilesetRenderer.TILE_SIZE
        ) - (int)WorldViewCamera.camera_y;
    }

}
