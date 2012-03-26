/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.game.ent.controller;

import com.nuclearunicorn.libroguelike.game.ent.Entity;

/**
 *
 * @author Administrator
 */
public class BaseController implements IEntityController {

    protected Entity owner = null;

    public void attach(Entity entity){
        this.owner = entity;
    }

    public Entity getOwner(){
        return owner;
    }

    public void think(){

    }
}
