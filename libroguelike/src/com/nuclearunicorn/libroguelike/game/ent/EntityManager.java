/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.game.ent;


import com.nuclearunicorn.libroguelike.events.EEntitySpawn;
import com.nuclearunicorn.libroguelike.events.Event;
import com.nuclearunicorn.libroguelike.events.IEventListener;
import com.nuclearunicorn.libroguelike.events.network.EEntityMove;
import com.nuclearunicorn.libroguelike.game.GameEnvironment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author Administrator
 */
public class EntityManager implements IEventListener {
    
    //TODO: assign to the world layer

    private GameEnvironment environment = null;

    /*static final ArrayList<Entity> ent_list = new ArrayList<Entity>();
    public static Collection ent_list_sync = Collections.synchronizedCollection(ent_list);*/

    public HashMap<Integer, ArrayList<Entity>> layer_ent_list = new HashMap<Integer, ArrayList<Entity>>(100);

    public void add(Entity ent, int layer_id){
        
        if (!isUniqueEntity(ent, layer_id)){
            throw new RuntimeException(environment.getName() + "> Trying to register entity with duplicate uid: " + ent.get_uid());
        }
        
        ent.setLayerId(layer_id);      
        ArrayList<Entity> entList = getList(layer_id);

        if (!entList.contains(ent)){
            entList.add(ent);
        }
    }

    //todo: replace with uid-entity hashmap for better performance?
    private boolean isUniqueEntity(Entity newEnt, int layerId) {
        for (Entity ent: getList(layerId)){
            if (ent.get_uid().equals(newEnt.get_uid())){
                return false;
            }
        }
        return true;
    }

    public ArrayList<Entity> getList(int layer_id){
        ArrayList<Entity> entList = layer_ent_list.get(layer_id);

        if (entList == null){
            entList = new ArrayList<Entity>();
            layer_ent_list.put(layer_id, entList);
        }
        
        return entList;
    }

    public void setEnviroment(GameEnvironment environment){
        this.environment = environment;

        environment.getEventManager().subscribe(this);
    }

    /* static final EntityManager instance = new EntityManager();
    static {
        ClientEventManager.eventManager.subscribe(instance);
    }*/

    /*
     * This method is called whether entity moves or spawns
     * to be sure that render order is correct
     */
    public synchronized void update(){
        //TODO: fix concurency issues there
        List<ArrayList<Entity>> entities = new ArrayList<ArrayList<Entity>>(layer_ent_list.values());

        for(ArrayList<Entity> list: layer_ent_list.values()){
            Collections.sort(list);
        }
    }

    public boolean has_ent(Entity ent, int layer_id){
        return getList(layer_id).contains(ent);
    }

    public void remove_entity(Entity ent, int layer_id){
        getList(layer_id).remove(ent);
    }
    
    public void remove_entity(Entity ent){
        for (int layer_id: layer_ent_list.keySet()){
            remove_entity(ent, layer_id);
        }
    }

    public Entity[] getEntities(int layer_id){
        return (Entity[]) getList(layer_id).toArray(new Entity[0]);
    }

    public Entity get_entity(String entity_id, int layer_id){
        for (Entity ent: getList(layer_id)){
            if (ent.get_uid() == entity_id){
                return ent;
            }
        }
        return null;
    }

    /*
     * Search for entity in whole list
     */
    public Entity get_entity(String uid) {
        for (int layer_id: layer_ent_list.keySet()){
            Entity ent = get_entity(uid, layer_id);
            if (ent!=null){
                return ent;
            }
        }
        return null;
    }

    public void e_on_event(Event event) {
        if (event instanceof EEntitySpawn){
            update();
        }
        if (event instanceof EEntityMove){
            update();
        }
    }

    public void reset() {
        layer_ent_list.clear();
    }
}