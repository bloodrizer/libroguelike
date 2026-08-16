package com.nuclearunicorn.serialkiller.game.world.entities;

import com.nuclearunicorn.libroguelike.game.ent.ActionList;
import com.nuclearunicorn.libroguelike.game.ent.BaseEntityAction;
import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.game.player.Player;
import com.nuclearunicorn.serialkiller.game.ai.PlayerAI;

import java.util.ArrayList;

/**
 */
public class EntityBed extends EntityFurniture {
    @Override
    public ArrayList get_action_list() {

        class ActionSleep extends BaseEntityAction {
            @Override
            public void execute() {
                if (assert_range()){
                    Player.get_ent().getAI().setState(PlayerAI.AI_STATE_SLEEPING);
                }
            }
        }


        ActionList<Entity> list = new ActionList();
        list.set_owner(this);
        list.add_action(new ActionSleep(),"Sleep");

        return list.get_action_list();
    }
}
