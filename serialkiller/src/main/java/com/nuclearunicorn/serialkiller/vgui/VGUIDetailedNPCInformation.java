package com.nuclearunicorn.serialkiller.vgui;

import com.nuclearunicorn.libroguelike.core.Input;
import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.game.player.Player;
import com.nuclearunicorn.libroguelike.vgui.NE_GUI_FrameModern;
import com.nuclearunicorn.libroguelike.vgui.NE_GUI_Text;
import com.nuclearunicorn.serialkiller.game.ai.PedestrianAI;
import com.nuclearunicorn.serialkiller.game.bodysim.BodySimulation;
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
       info.max_lines = 20;
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
        info.add_line(prefix + " is " + ent.race.diplayName());


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
        info.add_line("");
        if (ent.getAI() != null){
            if (ent.getAI().getState() == PedestrianAI.AI_STATE_TIRED){
                info.add_line(prefix + " looks tired");
            }
            if (ent.getAI().getState() == PedestrianAI.AI_STATE_SLEEPING){
                info.add_line(prefix + " is sleeping with a happy smile on a face");    //todo: implement mood
            }

            if (Input.key_state_alt){
                info.add_line("AI state is '" + ent.getAI().getState()+"'");
            }
        }


        if (ent.getApartment() == null){
            info.add_line(prefix + " is homeless");
        }else{
            //Entity bed = ent.getApartment().beds.get((int) Math.random() * ent.getApartment().beds.size());
            if (ent.getSex() == EntityRLHuman.Sex.FEMALE){
                info.add_line(prefix + " has " + ent.getApartment().beds.size() + " beds at her apartment" );
            }else{
                info.add_line(prefix + " has " + ent.getApartment().beds.size() + " beds at his apartment" );
            }
        }

        if (ent.getMate() != null && ent.getMate() == Player.get_ent()){
            if (ent.getSex() == EntityRLHuman.Sex.FEMALE){
                info.add_line("She is your lovely wife" );
            }else{
                info.add_line("He is your husband" );
            }
        }

        if (ent.getParent() != null && ent.getParent() == Player.get_ent()){
            if (ent.getSex() == EntityRLHuman.Sex.FEMALE){
                info.add_line("She is your daughter" );
            }else{
                info.add_line("He is your son" );
            }
        }

        //bodysim part
        BodySimulation bodysim = ent.getBodysim();
        if (bodysim != null){
            if (bodysim.isInfected()){
                info.add_line(prefix + " looks sick" );
            }
        }

    }


}
