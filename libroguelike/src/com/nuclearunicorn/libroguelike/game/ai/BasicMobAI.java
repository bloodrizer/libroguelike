/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.game.ai;

import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.game.ent.controller.NpcController;
import com.nuclearunicorn.libroguelike.game.player.Player;
import com.nuclearunicorn.libroguelike.utils.Fov;
import org.lwjgl.util.Point;

/**
 *
 * @author Administrator
 */
public class BasicMobAI extends AI{

    enum AIState{
        STATE_IDLE,
        STATE_ROAMING,
        STATE_CHASE,
        STATE_AVOIDE
    }

    AIState state = AIState.STATE_IDLE;

    @Override
    public void update(){
        state = AIState.STATE_ROAMING;

        //here is perceptron polling

        Entity[] ents = Fov.get_entity_in_radius(owner.getEnvironment().getEntityManager(), owner.origin, 3, owner.getLayerId());

        for (int i=0; i<ents.length; i++){

            //Replace it with fraction system

            if (ents[i].isPlayerEnt()){
                state = AIState.STATE_CHASE;
            }
        }


        
        /*if (entity_in_fov()){
            
        }*/
    }

    @Override
    public void think(){

        if(owner.controller == null || ! (owner.controller instanceof NpcController)){
            return;
        }

        NpcController npc_ctrl = (NpcController)(owner.controller);

        switch(state){
            case STATE_ROAMING:
                if (npc_ctrl.path == null && (int)(Math.random() * 20) < 25 ){
                    int x = owner.origin.getX() + 5 - (int)(Math.random() * 10);
                    int y = owner.origin.getY() + 5 - (int)(Math.random() * 10);

                    npc_ctrl.set_destination(new Point(x,y));
                }
            break;

            case STATE_CHASE:
                //Entity player_ent = Player.get_ent();
                npc_ctrl.set_destination(Player.get_origin());

            break;
        }
    }
}
