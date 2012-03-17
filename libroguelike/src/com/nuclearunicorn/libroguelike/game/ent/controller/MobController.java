/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.game.ent.controller;

import com.nuclearunicorn.libroguelike.core.client.ClientGameEnvironment;
import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.game.player.Player;

/**
 *
 * @author Administrator
 */
public class MobController extends NpcController{

    @Override
    public void e_on_obstacle(int x, int y) {
        Entity actor = ClientGameEnvironment.getWorldLayer(Player.get_zindex()).get_tile(x, y).get_actor();

        if (owner.getAI() != null ){
            owner.getAI().e_on_obstacle(x, y);
        }

        if(actor!=null && owner.get_combat() !=null){
            //owner.get_combat().inflict_damage(actor);
            //TODO: pass event to the ai manager and let it decide weither to attack target or not
        }
    }
}
