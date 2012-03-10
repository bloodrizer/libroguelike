package com.nuclearunicorn.serialkiller.game.combat;

import com.nuclearunicorn.libroguelike.game.combat.BasicCombat;
import com.nuclearunicorn.libroguelike.game.ent.EntityNPC;
import com.nuclearunicorn.libroguelike.game.items.BaseItem;
import com.nuclearunicorn.libroguelike.game.world.WorldTimer;

/**
 * Stats-aware combat engine
 */
public class RLCombat extends BasicCombat {

    NPCStats stats;

    public RLCombat(){
        super();

        stats = new NPCStats();
        hp = get_max_hp();
    }


    @Override
    public int get_max_hp() {
        return 20 + 4*stats.end;
    }
    
    public int getFovRadius(){
        int fov = (int)(5 + 1.2*stats.per);
        if (WorldTimer.is_night()){
            fov = (int)(fov * 0.7);
        }

        return fov;
    }

    @Override
    public int get_damage_amt() {
        return stats.str + getEquipBonus("damage");
    }

    private int getEquipBonus(String effectId) {
        if (owner instanceof EntityNPC){
            EntityNPC npc = (EntityNPC) owner;

            int bonus = 0;
            if (npc.equipment != null){
                for(BaseItem item : npc.equipment.slots.values()){
                    String effect = item.getEffect(effectId);
                    bonus += Integer.parseInt(effect);
                }
            }

            return bonus;
        }

        return 0;
    }
    
    public int getDefence(){
        return getEquipBonus("defence");
    }
}
