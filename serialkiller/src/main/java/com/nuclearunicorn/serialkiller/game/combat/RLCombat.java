package com.nuclearunicorn.serialkiller.game.combat;

import com.nuclearunicorn.libroguelike.game.combat.BasicCombat;
import com.nuclearunicorn.libroguelike.game.combat.Damage;
import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.game.ent.EntityActor;
import com.nuclearunicorn.libroguelike.game.items.BaseItem;
import com.nuclearunicorn.libroguelike.game.world.WorldTimer;
import com.nuclearunicorn.serialkiller.game.events.CriminalActionEvent;
import com.nuclearunicorn.serialkiller.game.sound.SoundEvent;
import com.nuclearunicorn.serialkiller.game.sound.SoundKind;
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
        if (dmgTypeId == "dmg_nonlethal"){
            return Damage.DamageType.DMG_NONLETHAL;
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
            new SoundEvent(owner.origin, SoundKind.BODY_FALL, rlOwner,
                    owner.getLayerId()).emit();
        } else if (damage.amt > 0) {
            // The loudest thing in any murder is the victim, and at 70dB it is the one
            // noise that reliably gets out of a building through a window or an open door.
            new SoundEvent(owner.origin, SoundKind.SCREAM, rlOwner, owner.getLayerId()).emit();
        }
    }



    @Override
    public void attack(Entity ent) {
        RLMessages.message(owner.getName() + " is attacking "+ent.getName(), new Color(253,126,126));

        CriminalActionEvent event = new CriminalActionEvent(ent.origin, (EntityActor)owner, ent);
        event.post();

        // Every blow makes a noise, whoever throws it. This used to fire only for the player,
        // which meant a mugging two streets over was silent and the town's sensor net only
        // ever detected the one criminal it already knew about.
        new SoundEvent(ent.origin, attackSound(), (EntityActor)owner, owner.getLayerId()).emit();
    }

    /**
     * What the blow sounds like. A knife is quieter than a fist, which is quieter than
     * something breaking — so the weapon that makes a murder easy is also the one that makes
     * it quiet, and that is a choice worth having.
     */
    private SoundKind attackSound() {
        switch (getDamageType()) {
            case DMG_CUT:    return SoundKind.KNIFE;
            case DMG_BLUNT:  return SoundKind.BONE_BREAK;
            default:         return SoundKind.PUNCH;
        }
    }

    public NPCStats getStats() {
        return stats;
    }
}
