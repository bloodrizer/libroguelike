package com.nuclearunicorn.serialkiller.game.world.items;

import com.nuclearunicorn.libroguelike.game.ent.ActionList;
import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.game.items.BaseItem;
import com.nuclearunicorn.libroguelike.game.items.BaseItemAction;
import com.nuclearunicorn.libroguelike.game.items.ItemEnt;
import com.nuclearunicorn.serialkiller.game.world.entities.EntityRLHuman;
import com.nuclearunicorn.serialkiller.render.AsciiEntRenderer;
import org.newdawn.slick.Color;

import java.util.ArrayList;

/**
    Same item as BaseItem, but can spawn itemEnts with ASCII-Art renderer
 */
public class RLItem extends BaseItem{

    @Override
    public ArrayList get_action_list() {

        class DropItemAction extends BaseItemAction {

            @Override
            public void execute() {
                this.owner.drop();


                Entity entOwner = this.owner.get_container().getOwner();

                ItemEnt itemEnt = new ItemEnt();
                itemEnt.set_item( this.owner );
                itemEnt.setLayerId(entOwner.getLayerId());

                itemEnt.setRenderer( new AsciiEntRenderer("i", Color.lightGray) );
                itemEnt.setEnvironment( entOwner.getEnvironment());

                itemEnt.set_blocking(false);

                itemEnt.spawn( entOwner.origin );
            }

        }

        //eat food action

        class EatItemAction extends BaseItemAction {

            @Override
            public void execute() {
                EntityRLHuman humanOwner = ((EntityRLHuman)owner.get_container().getOwner());

                this.owner.del_count(1);
                if (this.owner.hasEffect("restore_hunger")){
                    int hungerAmt = Integer.parseInt(owner.getEffect("restore_hunger"));
                    humanOwner.getBodysim().restoreHunger(hungerAmt);
                }
            }

        }


        ActionList<BaseItem> list = new ActionList<BaseItem>();
        list.set_owner(this);

        if (hasEffect("restore_hunger") || hasEffect("restore_hp")){
            list.add_action(new EatItemAction(),"eat");
        }
        list.add_action(new DropItemAction(),"Drop");

        return list.get_action_list();
    }


    public static RLItem produce(String type, int count){
        RLItem item = new RLItem();

        item.set_type(type);
        item.set_count(count);

        return item;
    }

    public RLItem getItem() {
        RLItem item = RLItem.produce(type, count);

        item.set_container(container);
        item.set_slot(slot);

        item.setEffects(effects);

        return item;
    }

}
