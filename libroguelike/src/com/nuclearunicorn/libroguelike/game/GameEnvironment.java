/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.game;

import com.nuclearunicorn.libroguelike.events.EventManager;
import com.nuclearunicorn.libroguelike.game.ent.EntityManager;
import com.nuclearunicorn.libroguelike.game.world.WorldModel;
import com.nuclearunicorn.libroguelike.game.world.layers.WorldLayer;

/**
 *
 * @author Administrator
 */

/*
 * Container that binds former static EventManager, WorldModel and EntityManager into one entity
 *
 * Used to allow client and server on the same application to have own model of game processes
 *
 */

public abstract class GameEnvironment {

    protected EntityManager entManager = null;
    protected WorldModel clientWorld = null;


    public EventManager getEventManager(){
        throw new RuntimeException("requesting EventManager on abstract GameEnvironment");
    }

    public EntityManager getEntityManager(){
        if (entManager == null){
            
            entManager = new EntityManager();
            entManager.setEnviroment(this);
        }
        return entManager;
    }
    
    public void setWorld(WorldModel model){
        clientWorld = model;
        clientWorld.setEnvironment(this);
    }

    public WorldModel getWorld(){
        if (clientWorld == null){
            
            clientWorld = new WorldModel();
            clientWorld.setEnvironment(this);
        }
        return clientWorld;
    }

    public WorldLayer getWorldLayer(int layerId){
        return getWorld().getWorldLayer(layerId);
    }

    public void reset() {
        getEventManager().reset();
        entManager.reset();
        clientWorld.reset();
    }
}
