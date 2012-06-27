/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nuclearunicorn.negame.server.core;

import com.nuclearunicorn.libroguelike.game.ent.Entity;
/**
 *
 * @author bloodrizer
 */
public class User {

    Entity playerEnt;
    int userId = 123456;

    public int getId() {
        return userId;
    }

    public void setEntity(Entity ent){
        this.playerEnt = ent;
    }

    public Entity getEntity(){
        return playerEnt;
    }
}
