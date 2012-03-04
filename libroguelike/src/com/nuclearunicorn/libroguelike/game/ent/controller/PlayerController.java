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
public class PlayerController extends NpcController{

    public PlayerController(){
        super();

        //MOVE_SPEED = 0.5f;
    }

    @Override
    public void e_on_obstacle(int x, int y) {

        if(!Player.is_combat_mode()){
            return;
        }

        Entity obstacle = ClientGameEnvironment.getWorldLayer(Player.get_zindex()).get_tile(x, y).get_obstacle();
        if(obstacle!=null && owner.get_combat() !=null){
            owner.get_combat().inflict_damage(obstacle);
        }
    }
}
