/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.core.client;

import com.nuclearunicorn.libroguelike.events.EventManager;
import com.nuclearunicorn.libroguelike.game.GameEnvironment;
import com.nuclearunicorn.libroguelike.game.ent.EntityManager;
import com.nuclearunicorn.libroguelike.game.world.WorldModel;
import com.nuclearunicorn.libroguelike.game.world.layers.WorldLayer;

/**
 *
 * @author Administrator
 */
public class ClientGameEnvironment {
    static GameEnvironment env = null;

    public static void reset(){
        getEnvironment().reset();
    }

    public static GameEnvironment getEnvironment(){
        if (env == null){
            env = new GameEnvironment(){
                @Override
                public EventManager getEventManager(){
                    return ClientEventManager.eventManager;
                }
            };
        }
        return env;
    }

    public static void setEnvironment(GameEnvironment clientGameEnvironment) {
        env = clientGameEnvironment;
    }

    public static WorldModel getWorldModel(){
        return getEnvironment().getWorld();
    }

    public static WorldLayer getWorldLayer(int layerID){
        return getWorldModel().getWorldLayer(layerID);
    }

    public static EntityManager getEntityManager() {
        return getEnvironment().getEntityManager();
    }

}
