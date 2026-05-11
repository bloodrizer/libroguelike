/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.game.combat;

import com.nuclearunicorn.libroguelike.events.ETakeDamage;
import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.game.ent.EntityActor;

/**
 *  This is generic combat engine, that is only inflicting damage based on hp
 */
public class BasicCombat extends Combat{


    @Override
    public void take_damage(Damage damage){
        
        ETakeDamage event = new ETakeDamage(this.owner, damage);
        event.post();

        hp = hp-damage.amt;

        if (!is_alive()){
            die(damage.inflictor);
        }
    }

    public void die(Entity killer){
        //TODO: post event there
        //owner.trash();

        if (owner instanceof EntityActor){
            ((EntityActor)owner).die(killer);
        }
    }

    public int get_damage_amt(){
        return 2;
    }


    public Damage.DamageType getDamageType(){
        return Damage.DamageType.DMG_GENERIC;
    }

    @Override
    public int get_max_hp(){
        return 50;
    }

    @Override
    public void inflict_damage(Entity ent){
        super.inflict_damage(ent);

        attack(ent);

        //that is very strange scenario when we try to damage ourselves.
        if (ent == this.owner){
            return;
        }

        Combat ent_combat = ent.get_combat();
        if (ent_combat != null){
            //do something there
            Damage.DamageType type = getDamageType();
            int amt = get_damage_amt();

            //non-lethal weapons ignore all damage including stat bonuses
            if (type.equals(Damage.DamageType.DMG_NONLETHAL)){
                amt = 0;
            }

            Damage dmg = new Damage(amt, type);
            dmg.set_inflictor(this.owner);


            ent_combat.take_damage(dmg);
        }
    }

    public void attack(Entity ent){

    }
        
}
