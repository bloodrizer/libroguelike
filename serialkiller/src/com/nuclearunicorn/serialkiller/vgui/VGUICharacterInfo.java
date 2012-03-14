package com.nuclearunicorn.serialkiller.vgui;

import com.nuclearunicorn.libroguelike.game.player.Player;
import com.nuclearunicorn.libroguelike.vgui.NE_GUI_FrameModern;
import com.nuclearunicorn.libroguelike.vgui.NE_GUI_Text;
import com.nuclearunicorn.serialkiller.game.combat.NPCStats;
import com.nuclearunicorn.serialkiller.game.combat.RLCombat;
import org.newdawn.slick.Color;

public class VGUICharacterInfo extends NE_GUI_FrameModern{

    NE_GUI_Text stats;

    public VGUICharacterInfo(){
        super(true);    //close button

        stats = new NE_GUI_Text();
        stats.max_lines = 10;
        stats.x = 10;
        stats.y = 10;
        
        add(stats);
    }

    public void updateInfo(){
        stats.clearLines();
        RLCombat combat = (RLCombat) Player.get_ent().get_combat();
        NPCStats npcStats = combat.getStats();

        stats.add_line("HP: " + combat.get_hp() + "/" + combat.get_max_hp() , Color.lightGray);
        stats.add_line("Damage: " + combat.get_damage_amt() , Color.lightGray);
        stats.add_line("");
        stats.add_line("Str:" + npcStats.str, Color.lightGray);
        stats.add_line("Per:" + npcStats.per, Color.lightGray);
        stats.add_line("End:" + npcStats.end, Color.lightGray);
        stats.add_line("Chr:" + npcStats.chr, Color.lightGray);
        stats.add_line("Int:" + npcStats.intl, Color.lightGray);
        stats.add_line("Agi:" + npcStats.agi, Color.lightGray);
        stats.add_line("Luk:" + npcStats.luk    , Color.lightGray);
    }

    @Override
    public void render() {
        updateInfo();
        super.render();
    }
}
