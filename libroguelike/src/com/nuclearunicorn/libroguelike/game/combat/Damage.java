/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.game.combat;

import com.nuclearunicorn.libroguelike.game.ent.Entity;

/**
 *
 * @author Administrator
 */
public class Damage {
    public enum DamageType {
        DMG_GENERIC,    //generic damage
        DMG_CUT,        //inflicts bleading
        DMG_BLUNT,      //inflicts stun
        DMG_FIRE,       //inflicts continous burning
        DMG_MAGIC       //reserved
    };

    public DamageType type = DamageType.DMG_GENERIC;
    public int amt; //amount of damage
    public Entity inflictor;

    public void set_inflictor(Entity inflictor){
        this.inflictor = inflictor;
    }

    public Damage(int amt, DamageType type){
        this.amt = amt;
        this.type = type;
    }
    
    public Damage(int amt){
        this.amt = amt;
        this.type = DamageType.DMG_GENERIC;
    }
}
