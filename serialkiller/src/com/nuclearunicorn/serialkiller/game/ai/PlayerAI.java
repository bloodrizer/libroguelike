package com.nuclearunicorn.serialkiller.game.ai;

import com.nuclearunicorn.libroguelike.game.ai.IAIAction;
import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.game.ent.controller.NpcController;
import com.nuclearunicorn.serialkiller.game.controllers.RLController;
import com.nuclearunicorn.serialkiller.game.world.entities.EntityRLHuman;
import com.nuclearunicorn.serialkiller.render.RLMessages;
import com.nuclearunicorn.serialkiller.utils.RLMath;
import org.newdawn.slick.Color;

import java.util.ArrayList;
import java.util.List;

/**
    Player AI is tricky transient layer, that normaly pass player comands to the controller,
    UNLESS player is sleepy or OUT OF CONTROL
 */

public class PlayerAI extends PedestrianAI {

    public static final String AI_STATE_CONTROLLABLE = "ai_state_CONTROLLABLE";
    public static final String AI_STATE_OUT_OF_CONTROL = "ai_state_OUT_OF_CONTROL";

    public PlayerAI(){
        super();

        registerState(AI_STATE_CONTROLLABLE, new IAIAction() {
            @Override
            public void act(NpcController npcController) {
                actionFollowCommand(npcController);
            }
        });

        registerState(AI_STATE_OUT_OF_CONTROL, new IAIAction() {
            @Override
            public void act(NpcController npcController) {
                actionOutOfControl(npcController);
            }
        });

        registerState(AI_STATE_SLEEPING, new IAIAction() {
            @Override
            public void act(NpcController npcController) {
                actionSleep(npcController);
            }
        });
    }

    private void actionSleep(NpcController npcController) {
        ((EntityRLHuman)owner).getBodysim().adjustAttribute("stamina",5f);
    }

    /*
        Out of control action
     */
    private void actionOutOfControl(NpcController npcController) {

        if (!npcController.hasPath()){
            engageNearestVictim(npcController);
        }

        if (npcController.hasPath()){
            npcController.follow_path();
        }
        //enter combat mode
        //kill NPC

    }

    private void engageNearestVictim(NpcController npcController){
        List<EntityRLHuman> entNPC = new ArrayList<EntityRLHuman>();
        List<Entity> entList = owner.getEnvironment().getEntityManager().getList(owner.getLayer().get_zindex());
        for(Entity ent: entList){
            if (ent instanceof EntityRLHuman && !ent.isPlayerEnt()){
                entNPC.add((EntityRLHuman) ent);
            }
        }

        EntityRLHuman nearestNpc = RLMath.getNearestEntity(entNPC, owner.origin);
        //if we have nearest NPC, change route
        if (nearestNpc != null && (!nearestNpc.origin.equals(npcController.destination)) ){
            ((RLController)npcController).calculateAdaptivePath(owner.origin, nearestNpc.origin);
        }
        this.nearestEnemy = nearestNpc;
    }

    private void actionFollowCommand(NpcController npcController) {
        npcController.follow_path();
    }

    @Override
    public void update() {
        //super.update();
        //state = AI_STATE_CONTROLLABLE;
        if (state != null && state.equals(AI_STATE_SLEEPING)){
            if(((EntityRLHuman)owner).getBodysim().getStamina() >= 90f){
                state = AI_STATE_CONTROLLABLE;
                RLMessages.message("You feel refreshed", Color.cyan);
            }
            return;
        }


        if (((EntityRLHuman)owner).getBodysim().getBloodlust() > 80f){
            state = AI_STATE_OUT_OF_CONTROL;
        }else{
            state = AI_STATE_CONTROLLABLE;
        }

        if (state.equals(AI_STATE_OUT_OF_CONTROL)){
            //TODO replace with canSeeEnt(ent)
            if (nearestEnemy !=null && RLMath.pointInLOS(owner.origin, nearestEnemy.origin, 10)){
                state = AI_STATE_CHASING;   //chase entity
            }
        }

    }

    public boolean isOutOfControl() {
        if (state == null){
            return true;
        }
        if (state.equals(AI_STATE_OUT_OF_CONTROL)){
            return true;
        }
        if (state.equals(AI_STATE_CHASING)){
            return true;
        }
        if (state.equals(AI_STATE_SLEEPING)){
            return true;
        }
        //todo: sleeping
        return false;
    }
}
