package com.nuclearunicorn.serialkiller.game.world.entities;

import com.nuclearunicorn.libroguelike.core.Game;
import com.nuclearunicorn.libroguelike.events.IEventListener;
import com.nuclearunicorn.libroguelike.game.combat.Combat;
import com.nuclearunicorn.libroguelike.game.ent.ActionList;
import com.nuclearunicorn.libroguelike.game.ent.BaseEntityAction;
import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.serialkiller.game.combat.RLCombat;
import com.nuclearunicorn.serialkiller.game.events.ShowContainerViewEvent;
import com.nuclearunicorn.serialkiller.render.AsciiEntRenderer;
import com.nuclearunicorn.serialkiller.render.RLMessages;
import org.newdawn.slick.Color;

import java.util.ArrayList;

/**
 * Basic furniture entity for tables, doors, etc
 */
public class EntityFurniture extends EntityRLActor {
    
    public EntityFurniture(){
        Combat combat = new RLCombat();
        set_combat(combat);    
    }

    @Override
    public void die(Entity killer) {
        super.die(killer);

        ((AsciiEntRenderer)this.render).symbol = "X";
        RLMessages.message(name + " is broken", Color.lightGray);

        set_blocking(false);
    }

    @Override
    public ArrayList get_action_list() {
        class ActionOpen extends BaseEntityAction {
            @Override
            public void execute() {
                ((IEventListener) Game.get_game_mode().get_ui()).e_on_event(new ShowContainerViewEvent(owner));
            }
        }


        ActionList<Entity> list = new ActionList();
        list.set_owner(this);
        list.add_action(new ActionOpen(),"Open");

        return list.get_action_list();
    }
}
