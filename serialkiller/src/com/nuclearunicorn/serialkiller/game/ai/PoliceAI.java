package com.nuclearunicorn.serialkiller.game.ai;

import com.nuclearunicorn.libroguelike.core.client.ClientGameEnvironment;
import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.game.ent.EntityActor;
import com.nuclearunicorn.libroguelike.game.ent.controller.NpcController;
import com.nuclearunicorn.libroguelike.game.player.Player;
import com.nuclearunicorn.serialkiller.game.world.RLTile;

/**
 */
public class PoliceAI extends PedestrianAI {

    public PoliceAI(){
        super();

    }

    @Override
    public void think() {
        super.think();
    }

    @Override
    public void update() {
        super.update();

        //police never sleeps
        state = AI_STATE_PATROLLING;

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
            }
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

}
