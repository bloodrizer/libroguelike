/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.game.ent;


import com.nuclearunicorn.libroguelike.events.EEntitySpawn;
import com.nuclearunicorn.libroguelike.events.network.EEntityMove;
import com.nuclearunicorn.libroguelike.game.GameEnvironment;
import com.nuclearunicorn.libroguelike.game.ai.AI;
import com.nuclearunicorn.libroguelike.game.combat.Combat;
import com.nuclearunicorn.libroguelike.game.ent.controller.IEntityController;
import com.nuclearunicorn.libroguelike.game.items.ItemContainer;
import com.nuclearunicorn.libroguelike.game.world.WorldChunk;
import com.nuclearunicorn.libroguelike.game.world.WorldTile;
import com.nuclearunicorn.libroguelike.game.world.layers.WorldLayer;
import com.nuclearunicorn.libroguelike.render.DebugRenderer;
import com.nuclearunicorn.libroguelike.render.EntityRenderer;
import com.nuclearunicorn.libroguelike.utils.Timer;
import org.lwjgl.util.Point;

import java.io.Serializable;
import java.util.ArrayList;


/**
 *
 * @author Administrator
 */
public class Entity implements Comparable, Serializable {

    public Point origin;
    public WorldTile tile;  //the tile entity is currently assigned to
    /*
     * Combat handles all in-game combat mechanic, as stats, damage infliction and damage taking
     */
    protected Combat combat;
    protected AI ai;

    /*
     * This is an entity offset in tile coord system
     * It's used to allow smooth entity movement from tile to tile - for a player using NPC controller or for a npc using lerp
     */
    public float dx = 0.0f;
    public float dy = 0.0f;
    
    public float light_amt = 0.0f;
    private int uid = 0;
    private long next_think;

    transient public IEntityController controller;
    private WorldChunk chunk = null;
    
    private boolean blocking = true;
    protected String name = "undefined";

    protected EntityRenderer render = null;


    /*
     * This flag indicates that this entity is no longer required by WorldModel
     * This entity will be cleared with next gc cycle.
     */
    protected boolean garbage = false;

    //TODO: FIX ME FIX ME FIX ME
    private int layer_id = -1;

    transient protected GameEnvironment env = null;

    //--------------------------------------------------------------------------
    //  inventory
    //--------------------------------------------------------------------------
    public ItemContainer container = new ItemContainer();

    public enum Orientation {
        ORIENT_N,
        ORIENT_W,
        ORIENT_S,
        ORIENT_E
    }
    public Orientation orientation = Orientation.ORIENT_N;


    public boolean is_garbage(){
        return garbage;
    }
    public void trash(){
        garbage = true;
    }

    public boolean is_blocking(){
        return blocking;
    }
    public void set_blocking(boolean blocking){
        this.blocking = blocking;
    }

    //helper functions for code beautification
    public int x(){
        return origin.getX();
    }

    public int y(){
        return origin.getY();
    }

    public void set_combat(Combat combat){
        this.combat = combat;
        combat.set_owner(this);
    }
    public void set_ai(AI ai){
        this.ai = ai;
        ai.set_owner(this);
    }

    public Combat get_combat(){
        return this.combat;
    }

    public int compareTo(Object ent) {
        if (!(ent instanceof Entity)){
            throw new ClassCastException("Entity instance expected.");
        }

        int hc_this = this.origin.getY()*10000 + this.origin.getX();

        if(ent == null || ((Entity)ent).origin == null){
            return hc_this;
        }

        //hashcode generation allows up to 32 entities stored in the tile

        int hc_ent = ((Entity)ent).origin.getY()*(10000) + ((Entity)ent).origin.getX();

        //TODO: implement z-ordering algorythm for multiple decals if any presents
        //like floor tiles renders prior to item ents

        return hc_this - hc_ent;
    }
    
    public int get_uid(){
        return this.uid;
    }

    public void set_uid(int uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }


    public void setLayerId(int layer_id) {
        this.layer_id = layer_id;
    }
    
    public int getLayerId(){
        return layer_id;
    }

    public WorldLayer getLayer() {
        return env.getWorldLayer(layer_id);
        //return ClientGameEnvironment.getWorldLayer(layer_id);
    }

    public void setEnvironment(GameEnvironment env){
        this.env = env;
    }
    
    public GameEnvironment getEnvironment(){
        return env;
    }

    //--------------------------------------------------------------------------
    public void spawn(int uid, Point origin){
        if (layer_id < 0){
            throw new RuntimeException("Spawning entity without correct layerID");
        }
        
        if (env == null){
            throw new RuntimeException("Spawning entity without enviroment");
        }

        env.getEntityManager().add(this, layer_id);

        this.uid = uid;
        this.origin = origin;


        EEntitySpawn spawnEvent = new EEntitySpawn(this,origin);
        spawnEvent.setManager(env.getEventManager());
        spawnEvent.post();

        set_next_think(Timer.get_time());
    }

    //this is a debug version of spawn method, that use temporary timestamp-based uid
    public void spawn(Point origin){
        int __uid = (int)System.currentTimeMillis();
        spawn(__uid, origin);
    }

    //--------------------------------------------------------------------------
    public void set_chunk(WorldChunk chunk){
        this.chunk = chunk;
    }
    
    public WorldChunk get_chunk(){
        return this.chunk;
    }

    public boolean in_chunk(WorldChunk chunk){
       if (chunk == null) {
           System.out.println("Entity::in_chunk() - ent chunk is null");
           return false;
       }
       //note: for some strange reason there are different object pointers that are compared
       return (this.chunk.origin.equals(chunk.origin));
    }

    public void think(){
        if (ai != null){

            //dead entities do not think >:3
            if (combat != null && !combat.is_alive()){
                return;
            }

            ai.think();
        }
    }

    public void update(){
        if (controller != null){
            controller.think();
        }
        if (ai != null){
            ai.update();
        }
    }

    public long frame_time_ms = 0;
    public long next_frame;

    public boolean is_next_frame(long current_time_ms){
        if (current_time_ms > next_frame){
            next_frame = current_time_ms;
            return true;
        }
        return false;
    }
        
    public void next_frame(){
        next_frame = next_frame+frame_time_ms;

        EntityRenderer renderer = get_render();
        if (renderer!=null){
            renderer.next_frame();
        }
    }

    public void set_next_frame(long frame_time_ms) {
        this.frame_time_ms = frame_time_ms;
        
    }

    public void set_controller(IEntityController controller){
        this.controller = controller;
        controller.attach(this);
    }


    public void move_to(Point dest){
        EEntityMove event = new EEntityMove(this, dest);
        event.setManager(env.getEventManager());
        event.post();
    }


    public void sleep(long sleep_time_ms){
        set_next_think(next_think+sleep_time_ms);
    }

    public void set_next_think(long time_ms){
        next_think = time_ms;
    }

    public boolean is_awake(long current_time_ms){
        if (current_time_ms > next_think){
            next_think = current_time_ms;   //synchronize think time for correct sleep work
            return true;
        }
        return false;
    }
   

    public static String toString(Entity ent){
        return "[uid:'"+ent.uid+"' @("+
                Integer.toString(ent.origin.getX())+
                ","+
                Integer.toString(ent.origin.getY())+
                ")]";
    }

    @Override
    public String toString(){
        return toString(this);
    }

    public boolean isPlayerEnt(){
        return false;
    }

    //--------------------------------------------------------------------------
    //                      A bit of rendering shit
    //--------------------------------------------------------------------------

    public EntityRenderer build_render(){
        return new DebugRenderer();
    }

    public final EntityRenderer get_render(){
        if (render == null ){
            render = build_render();
            render.set_entity(this);    //inject entity data
        }

        return render;
    }

    public void setRenderer(EntityRenderer renderer){
        this.render = renderer;
        renderer.set_entity(this);
    }

    //--------------------------------------------------------------------------
    //                      A bit of actions shit
    //--------------------------------------------------------------------------
    public ArrayList get_action_list(){
        return new ArrayList<Entity>(0);
    }
}
