/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.game.ai;

import com.nuclearunicorn.libroguelike.game.ent.controller.NpcController;
import com.nuclearunicorn.libroguelike.game.player.Player;
import org.lwjgl.util.Point;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Administrator
 */
public class BasicMobAI extends AI{

    public static final String AI_STATE_ROAMING = "ai_state_ROAMING";
    public static final String AI_STATE_CHASING = "ai_state_CHASING";

    private Map<String,IAIAction> stateMap = new HashMap<String, IAIAction>();
    protected String state;

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
                actionRoaming(npcController);
            }
        });
    }
    
    protected void registerState(String name, IAIAction action){
        stateMap.put(name, action);
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

        NpcController npc_ctrl = (NpcController)(owner.controller);
        
        IAIAction action = stateMap.get(state);
        if (action != null){
            action.act(npc_ctrl);
        }

    }

    protected void actionRoaming(NpcController npc_ctrl){
        if (npc_ctrl.path == null && (int)(Math.random() * 20) < 25 ){
            int x = owner.origin.getX() + 5 - (int)(Math.random() * 10);
            int y = owner.origin.getY() + 5 - (int)(Math.random() * 10);

            npc_ctrl.set_destination(new Point(x,y));
        }
    }

    protected void actionChase(NpcController npc_ctrl){
        npc_ctrl.set_destination(Player.get_origin());
    }

}
