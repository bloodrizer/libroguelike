package com.nuclearunicorn.serialkiller.vgui;

import com.nuclearunicorn.libroguelike.events.EMouseClick;
import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.game.items.BaseItem;
import com.nuclearunicorn.libroguelike.game.player.Player;
import com.nuclearunicorn.libroguelike.vgui.NE_GUI_FrameModern;
import com.nuclearunicorn.serialkiller.game.world.entities.EntityRLHuman;

import java.util.List;

/**

 */
public class VGUIContainerView extends NE_GUI_FrameModern{

    VGUICharacterInventory playerInv;
    VGUICharacterInventory containerInv;

    Entity containerEnt = null;

    public VGUIContainerView(){
        super(true);    //close button


        playerInv = new VGUICharacterInventory(){
            @Override
            protected void onLeftClick(int lineId, EMouseClick clickEvent) {
                if (containerEnt == null){
                    throw new RuntimeException("trying to move item into a null container");
                }

                List<BaseItem> items = Player.get_ent().getContainer().getItems();
                if (lineId < 0 || items.size() <= lineId) {
                    return;
                }

                BaseItem item = items.get(lineId);

                EntityRLHuman ent = (EntityRLHuman) Player.get_ent();
                if (ent.equipment == null) {
                    System.err.println("Player's entity equipment is null!");
                    return;
                }

                if (ent.equipment.hasItem(item)) {
                    ent.equipment.unequip(item);
                }

                containerEnt.getContainer().add_item(item);
            }
        };

        playerInv.set_tw(7);
        playerInv.set_th(9);
        playerInv.x = 20;
        playerInv.y = 20;
        playerInv.dragable = false;

        add(playerInv);


        containerInv = new VGUICharacterInventory(){
            @Override
            protected void onLeftClick(int lineId, EMouseClick clickEvent) {
                if (containerEnt == null){
                    throw new RuntimeException("trying to move item from a null container");
                }

                List<BaseItem> items = containerEnt.getContainer().getItems();
                if (lineId < 0 || items.size() <= lineId) {
                    return;
                }

                BaseItem item = items.get(lineId);

                EntityRLHuman ent = (EntityRLHuman) Player.get_ent();

                if (ent.equipment == null) {
                    throw new RuntimeException("Player's entity equipment is null!");
                }

                ent.getContainer().add_item(item);
            }
        };

        containerInv.set_tw(7);
        containerInv.set_th(9);
        containerInv.x = 20;
        containerInv.y = 20;
        containerInv.dragable = false;

        add(containerInv);

    }

}
