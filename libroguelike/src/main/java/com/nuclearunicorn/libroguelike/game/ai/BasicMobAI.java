/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.game.ai;

import com.nuclearunicorn.libroguelike.utils.Rng;

import com.nuclearunicorn.libroguelike.game.ent.controller.NpcController;
import com.nuclearunicorn.libroguelike.game.player.Player;
import org.lwjgl.util.Point;

/**
 *
 * @author Administrator
 */
public class BasicMobAI extends AI{

    public static final String AI_STATE_ROAMING = "ai_state_ROAMING";
    public static final String AI_STATE_CHASING = "ai_state_CHASING";

    public BasicMobAI(){

        registerState(AI_STATE_ROAMING, new IAIAction() {
            @Override
            public void act(NpcController npcController) {
                actionRoaming(npcController);
            }
        });

        registerState(AI_STATE_CHASING, new IAIAction() {
            @Override
            public void act(NpcController npcController) {
                actionChase(npcController);
            }
        });
    }
    

    @Override
    public void update(){
        state = AI_STATE_ROAMING;

        //here is perceptron polling
        /*Entity[] ents = Fov.get_entity_in_radius(owner.getEnvironment().getEntityManager(), owner.origin, 3, owner.getLayerId());

        for (int i=0; i<ents.length; i++){

            //Replace it with fraction system
            if (ents[i].isPlayerEnt()){
                state = AI_STATE_CHASING;
            }
        }*/
    }

    @Override
    public void think(){
        if(owner.controller == null || ! (owner.controller instanceof NpcController)){
            return;
        }

        act((NpcController)(owner.controller));
    }

    protected void actionRoaming(NpcController npc_ctrl){
        if (npc_ctrl.path == null && (int)(Rng.random(Rng.AI) * 20) < 25 ){
            int x = owner.origin.getX() + 5 - (int)(Rng.random(Rng.AI) * 10);
            int y = owner.origin.getY() + 5 - (int)(Rng.random(Rng.AI) * 10);

            npc_ctrl.set_destination(new Point(x,y));
        }
    }

    protected void actionChase(NpcController npcController){

        if (!npcController.hasPath()){
            npcController.set_destination(Player.get_origin());
        }

        npcController.follow_path();
    }

}
