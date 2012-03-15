package com.nuclearunicorn.serialkiller.vgui;

import com.nuclearunicorn.libroguelike.game.items.BaseItem;
import com.nuclearunicorn.libroguelike.game.items.EquipContainer;
import com.nuclearunicorn.libroguelike.game.player.Player;
import com.nuclearunicorn.libroguelike.vgui.NE_GUI_FrameModern;
import com.nuclearunicorn.libroguelike.vgui.NE_GUI_Text;
import com.nuclearunicorn.serialkiller.game.combat.NPCStats;
import com.nuclearunicorn.serialkiller.game.combat.RLCombat;
import org.newdawn.slick.Color;

/**
 */
public class VGUICharacterInventory extends NE_GUI_FrameModern {

    NE_GUI_Text items;

    public VGUICharacterInventory(){

        super(false);    //no close button

        title = "Inventory";

        items = new NE_GUI_Text(){
            @Override
            protected void e_on_line_click(int lineId) {


                super.e_on_line_click(lineId);

                BaseItem item = Player.get_player_ent().container.items.get(lineId);
                if (Player.get_player_ent().equipment.hasItem(item)){
                    Player.get_player_ent().equipment.unequip(item);
                }else{
                    Player.get_player_ent().equipment.equip_item(item);
                }
            }
        };
        items.max_lines = 10;
        items.set_size(20, 25, 50, 200);
        items.dragable = false;

        add(items);

    }

    public void updateInfo(){
        items.clearLines();
        RLCombat combat = (RLCombat) Player.get_ent().get_combat();
        NPCStats npcStats = combat.getStats();

        Color color;
        for(BaseItem item: Player.get_player_ent().container.items){
            EquipContainer equipment = Player.get_player_ent().equipment;

            if (equipment != null && equipment.hasItem(item)){
                color = Color.white;
            }else{
                color = Color.lightGray;
            }
            items.add_line(item.get_type(), color);
        }
    }

    @Override
    public void render() {
        updateInfo();
        super.render();
    }

}
