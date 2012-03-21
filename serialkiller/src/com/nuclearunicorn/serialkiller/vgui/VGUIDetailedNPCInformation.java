package com.nuclearunicorn.serialkiller.vgui;

import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.vgui.NE_GUI_FrameModern;
import com.nuclearunicorn.libroguelike.vgui.NE_GUI_Text;
import com.nuclearunicorn.serialkiller.game.combat.NPCStats;
import com.nuclearunicorn.serialkiller.game.combat.RLCombat;
import com.nuclearunicorn.serialkiller.game.social.CrimeRecord;
import com.nuclearunicorn.serialkiller.game.world.entities.EntityRLHuman;
import org.newdawn.slick.Color;


public class VGUIDetailedNPCInformation extends NE_GUI_FrameModern{

    EntityRLHuman ent;
    NE_GUI_Text info;

    public VGUIDetailedNPCInformation(){
       super(true);
        
       title = "Detailed information"; 
        
       info = new NE_GUI_Text();
       info.max_lines = 10;
       info.x = 20;
       info.y = 20;
       info.solid = false;

       add(info);
   }

    public void setNPC(Entity ent) {
        this.ent = (EntityRLHuman)ent;
    }

    @Override
    public void render() {
        updateInfo();
        super.render();
    }

    private void updateInfo() {
        info.clearLines();

        if (ent == null){
            return;
        }

        RLCombat combat = (RLCombat) ent.get_combat();
        NPCStats npcStats = combat.getStats();
        
        String prefix = "He";
        if (ent.getSex() == EntityRLHuman.Sex.FEMALE){
            prefix = "She";
        }
        
        info.add_line(ent.getName() + " is "+ent.getSex().name() + ", age " + ent.age);
        info.add_line(prefix + " is " + ent.race);

        info.add_line("");
        if (!ent.crimeRecords.isEmpty()){
            Color lightRed = new Color(250,160,160);
            info.add_line(prefix + " committed following crimes:", lightRed);
            for (CrimeRecord record: ent.crimeRecords){
                if (record.count == 1){
                    info.add_line(record.crimeType.diplayName() + " - once", lightRed);
                }else{
                    info.add_line(record.crimeType.diplayName() + " - " + record.count + " times", lightRed);
                }
            }
        }

    }


}
