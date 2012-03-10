/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.game.combat;

import com.nuclearunicorn.libroguelike.game.ent.Entity;
import java.io.Serializable;

/**
 *
 * This is a simple combat wrapper
 */
public abstract class Combat implements Serializable{

    public Stats stats = new Stats();
    protected int hp; // = get_max_hp()
    protected Entity owner;

    public void set_owner(Entity owner){
        this.owner = owner;
    }

    public void take_damage(Damage damage){
    }

    public int get_hp(){
        return hp;
    }

    public void set_hp(int hp){
        this.hp = hp;
        
        if (this.hp > get_max_hp()){
            this.hp = get_max_hp();
        }

    }

    public int get_max_hp(){
        return 0;
    }

    public boolean is_alive(){
        return hp>0;
    }


    /*
     * This method inflicts damage on given entity,
     * based on current combat mechanics of this entity
     */
    public void inflict_damage(Entity ent){

    }

}
