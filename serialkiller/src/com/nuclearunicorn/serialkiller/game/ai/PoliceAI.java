package com.nuclearunicorn.serialkiller.game.ai;

import com.nuclearunicorn.libroguelike.core.client.ClientGameEnvironment;
import com.nuclearunicorn.libroguelike.events.Event;
import com.nuclearunicorn.libroguelike.game.ai.IAIAction;
import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.game.ent.EntityActor;
import com.nuclearunicorn.libroguelike.game.ent.controller.NpcController;
import com.nuclearunicorn.libroguelike.game.player.Player;
import com.nuclearunicorn.serialkiller.game.controllers.RLController;
import com.nuclearunicorn.serialkiller.game.events.NPCReportCrimeEvent;
import com.nuclearunicorn.serialkiller.game.social.SocialController;
import com.nuclearunicorn.serialkiller.game.world.RLTile;
import com.nuclearunicorn.serialkiller.utils.RLMath;
import org.lwjgl.util.Point;

import java.util.List;

/**
 */
public class PoliceAI extends PedestrianAI {

    public static final String AI_STATE_INVESTIGATING = "ai_state_INVESTIGATING";

    public PoliceAI(){
        super();

        registerState(AI_STATE_INVESTIGATING, new IAIAction() {
            @Override
            public void act(NpcController npcController) {
                actionInvestigate(npcController);
            }
        });
    }

    private void actionInvestigate(NpcController npcController) {
        RLController controller = (RLController)npcController;

        List<Point> crimeplaces = SocialController.getCrimeplaces();
        Point nearestCrimeplace = RLMath.getNearestPoint(crimeplaces, owner.origin);

        controller.calculateAdaptivePath(owner.origin, nearestCrimeplace);
        controller.follow_path();
    }

    @Override
    public void think() {
        super.think();
    }

    @Override
    public void update() {
        //super.update();

        //police never sleeps

        //if (state != AI_STATE_INVESTIGATING) {
        //    state = AI_STATE_PATROLLING;
        //}

        //rule 1. override behavior to chase if see criminal (max priority)

        nearestEnemy = null;
        //just get some criminal in fov
        for (EntityActor enemy : knowCriminals){
            if (enemy.get_combat() != null && !enemy.get_combat().is_alive()){
                continue;
            }
            if (((RLTile)enemy.tile).isVisible()){
                nearestEnemy = enemy;

                ((NpcController) this.owner.controller).clearPath();

                state = AI_STATE_CHASING;
                return;
            }
        }

        //rule 2. if investigating, do no override

        if (state != AI_STATE_INVESTIGATING) {

            //rule 3. patrol if no investigation or criminal in fov
            state = AI_STATE_PATROLLING;
        }

    }

    @Override
    public void e_on_obstacle(int x, int y) {
        Entity actor = ClientGameEnvironment.getWorldLayer(Player.get_zindex()).get_tile(x, y).get_actor();
        
        if(actor!=null && owner.get_combat() !=null){
            if (knowCriminals.contains(actor)){
                owner.get_combat().inflict_damage(actor);
            }
        }
    }

    @Override
    public void e_on_event(Event event) {
        super.e_on_event(event);

        if (event instanceof NPCReportCrimeEvent){
            System.out.println("Police AI: started investigation");
            
            state = AI_STATE_INVESTIGATING;
        }
    }
}
