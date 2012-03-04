/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.game.ai;

import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.utils.Fov;

import java.io.Serializable;

/**
 *
 * @author Administrator
 */
public class AI implements Serializable{
    
    protected Entity owner;
    public void set_owner(Entity owner){
        this.owner = owner;
    }

    public void update(){
        
    }

    public void think(){
        
    }

    public boolean entity_in_fov(Entity ent){
        //todo: implement combat.get_fov();
        if (Fov.in_range(owner.origin, ent.origin, 5)){
            return true;
        }

        return false;
    }
}
