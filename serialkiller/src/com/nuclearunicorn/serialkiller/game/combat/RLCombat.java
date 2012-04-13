package com.nuclearunicorn.serialkiller.game.combat;

import com.nuclearunicorn.libroguelike.game.combat.BasicCombat;
import com.nuclearunicorn.libroguelike.game.combat.Damage;
import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.game.ent.EntityActor;
import com.nuclearunicorn.libroguelike.game.items.BaseItem;
import com.nuclearunicorn.libroguelike.game.world.WorldTimer;
import com.nuclearunicorn.serialkiller.game.events.CriminalActionEvent;
import com.nuclearunicorn.serialkiller.game.events.SuspiciousSoundEvent;
import com.nuclearunicorn.serialkiller.game.world.entities.EntityRLHuman;
import com.nuclearunicorn.serialkiller.render.RLMessages;
import org.newdawn.slick.Color;

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
        int maxFov = (int)(5 + 1.2*stats.per);
        int minFov = (int)(maxFov * 0.7);

        int fov = (int)(minFov + (maxFov-minFov) * WorldTimer.get_light_amt());

        /*if (WorldTimer.is_night()){
            fov = (int)(fov * 0.7);
        } */

        return fov;
    }
    
    public int getHearRadius(){
        return  (int)(5 + 1.5*stats.per);   //slightly better than LOS, and date of time does not affect our senses
    }

    @Override
    public Damage.DamageType getDamageType() {



        String dmgTypeId = "dmg_generic";
        if (!(owner instanceof EntityRLHuman)){
            return super.getDamageType();
        }
        EntityRLHuman npc = (EntityRLHuman) owner;

        if (npc.equipment != null){
            for(BaseItem item : npc.equipment.slots.values()){

                if (item == null){
                    continue;
                }
                String effect = item.getEffect("damage_type");

                if (effect != null){
                    dmgTypeId = effect;
                }
            }
        }
        
        //System.out.println("RLCombat: calculating damage for id '" + dmgTypeId + "'");

        if (dmgTypeId == "dmg_cut"){
            return Damage.DamageType.DMG_CUT;
        }
        if (dmgTypeId == "dmg_blunt"){
            return Damage.DamageType.DMG_BLUNT;
        }

        return super.getDamageType();
    }

    @Override
    public int get_damage_amt() {
        return stats.str + getEquipBonus("damage");
    }

    public int getEquipBonus(String effectId) {
        if (owner instanceof EntityRLHuman){
            EntityRLHuman npc = (EntityRLHuman) owner;

            //System.out.println("getting effect '"+effectId+"' on ent"+npc.getName());

            int bonus = 0;
            if (npc.equipment != null){
                for(BaseItem item : npc.equipment.slots.values()){

                    if (item == null){
                        continue;
                    }
                    String effect = item.getEffect(effectId);

                    if (effect == null){
                        continue;
                    }
                    //System.out.println("effect '"+effectId+"':"+effect);
                    try {
                        bonus += Integer.parseInt(effect);
                    }catch (NumberFormatException ex){
                        System.err.println("Failed to get int value of item effect '"+effect+"'");
                    }
                }
            }

            return bonus;
        }
        return 0;
    }


    
    public int getDefence(){
        return getEquipBonus("defence");
    }

    @Override
    public void take_damage(Damage damage) {
        super.take_damage(damage);
        RLMessages.message(owner.getName() + " took " + damage.amt + " damage", new Color(231,4,231));

        if (!(owner instanceof EntityRLHuman)){
            return;
        }
        EntityRLHuman rlOwner = (EntityRLHuman)owner;
        if ( rlOwner.getBodysim() != null ){
            rlOwner.getBodysim().takeDamage(damage);
        }
        if (!rlOwner.get_combat().is_alive()){
            //System.out.println(owner.getName() + "is MURDERED");
            Entity inflictor = damage.inflictor;
            if (inflictor instanceof EntityRLHuman){
                ((EntityRLHuman)inflictor).kill(rlOwner);
            }

        }
    }



    @Override
    public void attack(Entity ent) {
        RLMessages.message(owner.getName() + " is attacking "+ent.getName(), new Color(253,126,126));

        CriminalActionEvent event = new CriminalActionEvent(ent.origin, (EntityActor)owner);
        event.post();

        //TODO: temporary hack for player ent. TODO: rewrite for support of multiple criminals in town
        if (owner.isPlayerEnt()){
            SuspiciousSoundEvent soundEvent = new SuspiciousSoundEvent(ent.origin, 10); //TODO: differend sound modifiers
            soundEvent.post();
        }
    }

    public NPCStats getStats() {
        return stats;
    }
}
