/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.game.world;

import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.game.ent.EntityActor;
import org.lwjgl.util.Point;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Administrator
 */
public class WorldTile implements Serializable {
    public float light_level = 0.0f;
    public Point origin = null;

    public Entity get_active_object() {

        Object[] list = ent_list.toArray();
        for(int i=ent_list.size()-1; i>=0; i--){
            Entity ent = (Entity)list[i];

            if (ent.get_action_list().size() > 0){
                return ent;
            }
        }
        return null;
    }

    public enum TerrainType {
        TERRAIN_FORREST,
        TERRAIN_PLAIN,
        TERRAIN_WATER,
        TERRAIN_ROCK    //Valid for undeground areas only
    }

    public float moisture = 0.0f;

    public enum BiomeType {

        BIOME_SNOW,
        BIOME_TUNDRA(0),
        BIOME_BARE,
        BIOME_SCORCHED,

        BIOME_TAIGA(9),
        BIOME_SHRUBLAND,
        BIOME_TEMP_DESERT(2),

        BIOME_TEMP_RAINFOREST(25),
        BIOME_DECIDUOS_FOREST(25),
        BIOME_GRASSLAND(0),

        BIOME_TROPICAL_RAINFOREST(25),
        BIOME_SEASONAL_FOREST(25),
        BIOME_SUBTROPICAL_DESERT(2),
        
        BIOME_SHORE(2);     //shore is small sand one-tile area near the water source

        private final int tile_id;
        BiomeType(int tile_id){
            this.tile_id = tile_id;
        }
        BiomeType(){
            this.tile_id = 0;
        }

        public int tile_id(){
            return tile_id;
        }

        public int get_zindex(){
            return tile_id; //temp shit, change later
        }
    }

    private int tile_id = 0;
    private int height = 0;

    public TerrainType terrain_type = TerrainType.TERRAIN_PLAIN;
    public BiomeType biome_type = BiomeType.BIOME_GRASSLAND;


    public int get_moisture_zone(){
        int moisture_zone = (int) (moisture / (5.0f / 4)) + 1;

        return moisture_zone;
    }

    public int get_elevation_zone(){
        return height / (255/4) + 1;
    }

    public void update_biome_type(){
        return;
    }

    public float get_speed_modifier(){
        float speed = 1.0f;
        return speed;
    }

    //allocate sum data for entity storing
    public List<Entity> ent_list = new ArrayList<Entity>(10);

    public void add_entity(Entity ent){
        ent_list.add(ent);
        ent.tile = this;
    }
    public void remove_entity(Entity ent){
        ent_list.remove(ent);
    }

    /*
     * Check if tile has entity assigned to it
     *
     */
    public boolean has_ent(Class ent_class){
        Object[] list = ent_list.toArray();
        for(int i=ent_list.size()-1; i>=0; i--){
            Entity ent = (Entity)list[i];

            if (ent_class.isInstance(ent)){
                return true;
            }
        }
        return false;
    }

    Entity getEntity(Class/*<EntDecalBlood>*/ ent_class) {          //wtf?
        Object[] list = ent_list.toArray();
        for(int i=ent_list.size()-1; i>=0; i--){
            Entity ent = (Entity)list[i];

            if (ent_class.isInstance(ent)){
                return ent;
            }
        }
        return null;
    }

    public boolean isBlocked(){
        Object[] list = ent_list.toArray();
        for(int i=ent_list.size()-1; i>=0; i--){
            Entity entity = (Entity)list[i];
            if (entity.is_blocking()){
                  return true;
            }
        }
        return false;
    }

    //TODO: change to get_world_actor

    //this is more safe and more useful version of get_obstacle
    public Entity get_actor(){
        Object[] list = ent_list.toArray();
        for(int i=ent_list.size()-1; i>=0; i--){
            Entity entity = (Entity)list[i];
            if (entity instanceof EntityActor){
                return entity;
            }
        }
        return null;
    }

    public Entity get_obstacle(){
        Object[] list = ent_list.toArray();
        for(int i=ent_list.size()-1; i>=0; i--){
            Entity entity = (Entity)list[i];
            if (entity.is_blocking()){
                return entity;
            }
        }
        return null;
    }

    public int get_tile_id(){
        return tile_id;
    }

    public void set_tile_id( int tile_id){
        this.tile_id = tile_id;
    }

    public void set_height(int height){
        this.height = height;
    }

    public int get_height(){
        return height;
    }


}