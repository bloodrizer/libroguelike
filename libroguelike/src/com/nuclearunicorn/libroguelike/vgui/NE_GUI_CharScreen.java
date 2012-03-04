/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.vgui;

import com.nuclearunicorn.libroguelike.events.ETakeDamage;
import com.nuclearunicorn.libroguelike.events.Event;
import com.nuclearunicorn.libroguelike.game.combat.Combat;
import com.nuclearunicorn.libroguelike.game.combat.Stats;
import com.nuclearunicorn.libroguelike.game.player.Player;

import java.util.HashMap;

/**
 *
 * @author Administrator
 */
public class NE_GUI_CharScreen extends NE_GUI_FrameModern{

    static HashMap<String,NE_GUI_Label> stat_labels = new HashMap<String,NE_GUI_Label>(6);

    public NE_GUI_CharScreen(){
        super(true);
        set_title("Stats");

        create_labels();
    }

    static NE_GUI_Label hp_val;

    public final void create_labels(){
        String[] stats = Stats.stats;

        for (int i = 0; i< stats.length; i++){
            NE_GUI_Label stat_label = new NE_GUI_Label();
            stat_label.text = stats[i]+":";

            stat_label.x = 25;
            stat_label.y = 25 + i*20;

            add(stat_label);

            NE_GUI_Label stat_val = new NE_GUI_Label();
            stat_val.text = "5";

            stat_val.x = 60;
            stat_val.y = 25 + i*20;

            add(stat_val);

            stat_labels.put(stats[i], stat_val);
        }

        //------------------hp
            NE_GUI_Label hp_label = new NE_GUI_Label();
            hp_label.text = "hp:";
            hp_label.set_coord(75, 25);
            add(hp_label);

            hp_val = new NE_GUI_Label();
            hp_val.text = "0";
            hp_val.set_coord(100, 25);
            add(hp_val);
    }

    public static void update_stats(){

        Combat combat = Player.get_ent().get_combat();
        if(combat == null){
            return;
        }

        String[] stats = Stats.stats;
        for (int i = 0; i< stats.length; i++){
            int stat_value = combat.stats.get_stat(stats[i]);
            update_stat(stats[i],stat_value);
        }

        hp_val.text = combat.get_hp() + "/" + combat.get_max_hp();
    }

    public static void update_stat(String stat, int value){
        stat_labels.get(stat).text = Integer.toString(value);
    }

    /*@Override
    public void render(){
        
    }*/

    @Override
    public void notify_event(Event e){
        super.notify_event(e);

        if (e instanceof ETakeDamage){
            ETakeDamage dmg_event = (ETakeDamage) e;
            if(dmg_event.ent.equals(Player.get_ent())){
                update_stats();
            }
        }
    }
    
}
