package com.nuclearunicorn.serialkiller.game.ai;

import com.nuclearunicorn.libroguelike.events.Event;
import com.nuclearunicorn.libroguelike.game.ai.BasicMobAI;
import com.nuclearunicorn.libroguelike.game.ai.IAIAction;
import com.nuclearunicorn.libroguelike.game.ent.EntityActor;
import com.nuclearunicorn.libroguelike.game.ent.controller.NpcController;
import com.nuclearunicorn.serialkiller.game.events.NPCWitnessCrimeEvent;
import com.nuclearunicorn.serialkiller.game.world.RLTile;
import com.nuclearunicorn.serialkiller.game.world.RLWorldChunk;
import com.nuclearunicorn.serialkiller.render.AsciiEntRenderer;
import com.nuclearunicorn.serialkiller.render.RLMessages;
import org.lwjgl.util.Point;
import org.newdawn.slick.Color;

import java.util.ArrayList;
import java.util.List;

/*
    PEDESTRIAN AI - patroling from node to node like a policeman
*/
public class PedestrianAI extends BasicMobAI {

    public static final String AI_STATE_PATROLLING = "ai_state_PATROLLING";
    private static final String AI_STATE_ESCAPING = "ai_state_ESCAPING";

    List<EntityActor> knowCriminals = new ArrayList<EntityActor>();
    EntityActor nearestEnemy = null;


    public PedestrianAI(){
        super();

        registerState(AI_STATE_PATROLLING, new IAIAction() {
            @Override
            public void act(NpcController npcController) {
                actionPatroll(npcController);
            }
        });

        registerState(AI_STATE_ESCAPING, new IAIAction() {
            @Override
            public void act(NpcController npcController) {
                actionEscape(npcController);
            }
        });

    }

    private void actionPatroll(NpcController npcController) {
        if (npcController == null){
            return;
        }

        if (!npcController.hasPath()){
            getRandomRoute(npcController);
        }

        //add 10% chance of standing still, making it more natural for player to chase target
        if ((int)(Math.random()*100) >= 15){
            npcController.follow_path();
        }
    }

    private void actionEscape(NpcController npcController) {

        ((AsciiEntRenderer)owner.get_render()).symbol = "!";

        if (npcController == null){
            return;
        }

        int disst = npcController.distanceToTarget(nearestEnemy.origin);

        if (disst > 0 && disst < 10){
            npcController.escapeTarget(nearestEnemy);
        } else {
            //System.out.println("Invalid escape distance :"+disst+" to target '" + nearestEnemy.getName() + "'@"+nearestEnemy.origin);
        }

        if (Math.random()*100 > 60){

            RLMessages.message(owner.getName() + " screams!", Color.red);

            ((EntityActor)owner).say_message("AAAAAAAA!");
        }

    }

    private void getRandomRoute(NpcController npcController) {
        RLWorldChunk chunk = (RLWorldChunk)(this.owner.get_chunk());
        List<Point> mst = chunk.getMilestones();
        
        Point rndMilestone = mst.get((int) (Math.random() * mst.size()));
        
        //get random point near the milestone node (more natural ai behavior)
        Point point = new Point(
                rndMilestone.getX() + (int)Math.random()*4-2,
                rndMilestone.getY() + (int)Math.random()*4-2
        );

        npcController.set_destination(point);
    }


    @Override
    public void update(){
        //state = AI_STATE_ROAMING;

        /*
        timer	= self.owner.em.services["sk_time"]
		if timer.is_night():
			if not self.state == ai_state_SLEEPING:
				self.state = ai_state_TIRED
		else:
			self.state = None
        */

        /*if self.player_in_fov():

        if self.state == ai_state_SLEEPING:
        #if hear loud sound, than awake

        #otherwise:
        return


        #TODO: check if crimeplace is near and catch player
                player = self.owner.em.services["player"]
        if player.bloody and player.is_alive():
        #RUN FOR YOUR LIFE
        self.state = ai_state_ESCAPING*/

        state = AI_STATE_PATROLLING;


        nearestEnemy = null;
        //just get some criminal in fov
        for (EntityActor enemy : knowCriminals){
            if (((RLTile)enemy.tile).isVisible()){
                nearestEnemy = enemy;

                state = AI_STATE_ESCAPING;
            }
        }

        //------------------------pseudocode time
        //IF NO PLAYER (OR CRIMINAL) IS IN FOV, THAN RESET TO PATROLLING
        //ELSE DO NOTHING


    }

    @Override
    public void e_on_event(Event event) {
        if (event instanceof NPCWitnessCrimeEvent){
            NPCWitnessCrimeEvent e = (NPCWitnessCrimeEvent)event;

            //TODO: pass criminal to the AI manager, so state will be reset only if no known criminal is in fov
            knowCriminals.add((EntityActor) e.criminal);
        }
    }
}
