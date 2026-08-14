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
    
    public String name;
    
    public GameEnvironment(String name){
        this.name = name;
    }

    public String getName(){
        return name;
    }

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

    /**
     * Tear the world down for a "New game".
     *
     * <p>Note what {@code getEventManager().reset()} does: it clears the <i>listener list</i>,
     * so everything subscribed during startup is silently unsubscribed. The entity manager
     * subscribes exactly once, when it is lazily built, so it has to be re-attached here or
     * it stops seeing spawns and moves and never re-sorts for render order again. Services
     * outside the environment are their own owner's problem — they must re-subscribe on the
     * way in rather than guard on "have I ever been created", which is what left the second
     * town of a session with no working senses.
     */
    public void reset() {
        getEventManager().reset();
        getEntityManager().reset();
        getWorld().reset();
        getEntityManager().setEnviroment(this);
    }
}
