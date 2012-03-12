package com.nuclearunicorn.serialkiller.game.ai;

import com.nuclearunicorn.libroguelike.game.ai.BasicMobAI;
import com.nuclearunicorn.libroguelike.game.ai.IAIAction;
import com.nuclearunicorn.libroguelike.game.ent.controller.NpcController;
import com.nuclearunicorn.serialkiller.game.world.RLWorldChunk;
import org.lwjgl.util.Point;

import java.util.List;

/*
    PEDESTRIAN AI - patroling from node to node like a policeman
*/
public class PedestrianAI extends BasicMobAI {

    public static final String AI_STATE_PATROLLING = "ai_state_PATROLLING";


    public PedestrianAI(){
        super();

        registerState(AI_STATE_PATROLLING, new IAIAction() {
            @Override
            public void act(NpcController npcController) {
                actionPatroll(npcController);
            }
        });
        
    }

    private void actionPatroll(NpcController npcController) {

        //System.out.println("patrolling!");

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

    }

}
