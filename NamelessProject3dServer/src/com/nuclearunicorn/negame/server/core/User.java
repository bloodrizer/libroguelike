/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nuclearunicorn.negame.server.core;

import com.nuclearunicorn.libroguelike.game.ent.Entity;
import org.jboss.netty.channel.Channel;

/**
 *
 * @author bloodrizer
 */
public class User {

    Entity playerEnt;
    int userId = 123456;

    Channel charChannel;
    Channel gameChannel;

    public int getId() {
        return userId;
    }

    public void setEntity(Entity ent){
        this.playerEnt = ent;
    }

    public Entity getEntity(){
        return playerEnt;
    }

    public Channel getCharChannel() {
        return charChannel;
    }

    public void setCharChannel(Channel charChannel) {
        this.charChannel = charChannel;
    }

    public Channel getGameChannel() {
        return gameChannel;
    }

    public void setGameChannel(Channel gameChannel) {
        this.gameChannel = gameChannel;
    }

}
