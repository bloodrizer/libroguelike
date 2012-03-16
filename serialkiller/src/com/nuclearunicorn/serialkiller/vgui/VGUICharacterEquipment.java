package com.nuclearunicorn.serialkiller.vgui;

import com.nuclearunicorn.libroguelike.game.items.BaseItem;
import com.nuclearunicorn.libroguelike.game.player.Player;
import com.nuclearunicorn.libroguelike.vgui.NE_GUI_FrameModern;
import com.nuclearunicorn.libroguelike.vgui.NE_GUI_Text;
import com.nuclearunicorn.serialkiller.game.combat.NPCStats;
import com.nuclearunicorn.serialkiller.game.combat.RLCombat;
import org.newdawn.slick.Color;

import java.util.ArrayList;
import java.util.List;

public class VGUICharacterEquipment extends NE_GUI_FrameModern {
    NE_GUI_Text items;

    public VGUICharacterEquipment(){

        super(false);    //no close button

        title = "Equipment";

        items = new NE_GUI_Text(){
            @Override
            protected void e_on_line_click(int lineId) {

                super.e_on_line_click(lineId);

                List<BaseItem> eqItems = new ArrayList<BaseItem>(Player.get_player_ent().equipment.slots.values());
                if (lineId < 0 || eqItems.size() <= lineId){
                    return;
                }

                BaseItem item = eqItems.get(lineId);

                if (Player.get_player_ent().equipment.hasItem(item)){
                    Player.get_player_ent().equipment.unequip(item);
                }
            }
        };
        items.max_lines = 10;
        items.set_size(20, 25, 200, 200);
        items.dragable = false;

        add(items);

    }

    public void updateInfo(){
        items.clearLines();
        RLCombat combat = (RLCombat) Player.get_ent().get_combat();
        NPCStats npcStats = combat.getStats();

        Color color;
        for(BaseItem item: Player.get_player_ent().equipment.slots.values()){
            if (item != null){
                items.add_line(item.get_type(), Color.lightGray);
            }
        }
    }

    @Override
    public void render() {
        updateInfo();
        super.render();
    }
}
