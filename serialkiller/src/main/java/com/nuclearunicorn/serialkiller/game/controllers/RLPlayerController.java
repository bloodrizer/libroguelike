package com.nuclearunicorn.serialkiller.game.controllers;

import com.nuclearunicorn.libroguelike.core.client.ClientGameEnvironment;
import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.game.player.Player;
import com.nuclearunicorn.serialkiller.game.ai.PlayerAI;

/**
   Placeholder for every tricky things player may have
 */
public class RLPlayerController extends RLController{

    @Override
    public void e_on_obstacle(int x, int y) {

        PlayerAI ai = (PlayerAI) Player.get_ent().getAI();

        if(!Player.is_combat_mode() && (ai.getState() != PlayerAI.AI_STATE_OUT_OF_CONTROL)){
            return;
        }

        Entity obstacle = ClientGameEnvironment.getWorldLayer(Player.get_zindex()).get_tile(x, y).get_obstacle();
        if(obstacle!=null && owner.get_combat() !=null){
            owner.get_combat().inflict_damage(obstacle);
        }
    }

}
