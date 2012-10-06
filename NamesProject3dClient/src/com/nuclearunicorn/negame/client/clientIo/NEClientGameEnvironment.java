/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.negame.client.clientIo;

import com.nuclearunicorn.libroguelike.core.client.ClientGameEnvironment;
import com.nuclearunicorn.libroguelike.game.GameEnvironment;
import com.nuclearunicorn.libroguelike.game.ent.EntityManager;
import com.nuclearunicorn.libroguelike.game.world.WorldModel;
import com.nuclearunicorn.libroguelike.game.world.layers.WorldLayer;

/**
 *
 * @author Administrator
 */
public class NEClientGameEnvironment extends ClientGameEnvironment {
    static GameEnvironment env = null;

    public static GameEnvironment getEnvironment(){
        /*if (env == null){
            env = new GameEnvironment(){
                
            };
        }*/
        return env;
    }

    public static void setEnvironment(GameEnvironment clientGameEnvironment) {
        env = clientGameEnvironment;
    }

    public static WorldModel getWorldModel(){
        return env.getWorld();
    }

    public static WorldLayer getWorldLayer(int layerID){
        return getWorldModel().getWorldLayer(layerID);
    }

    public static EntityManager getEntityManager() {
        return env.getEntityManager();
    }

}
