/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.game.ent;

import com.nuclearunicorn.libroguelike.game.actions.IAction;
import com.nuclearunicorn.libroguelike.game.player.Player;

/**
 *
 * @author Administrator
*/

public class BaseEntityAction implements IAction<Entity> {
    protected Entity owner = null;
    protected String name = "undefined";

    public void set_name(String name){
        this.name = name;
    }

    public String get_name(){
        return this.name;
    }

    public void execute() {
        //do nothing
    }

    public void set_owner(Entity owner) {
        this.owner = owner;
    }

    public Entity get_owner() {
        return owner;
    }

    /*
     * This helper method asserts if player is in range of
     * action owner in order to take action
     *
     * return true if in range
     * return false if not AND sets entity as destination
     */

    public boolean assert_range(){
        if (Player.in_range(owner)){
            return true;
        }else{
            Player.move(owner.origin);
            return false;
        }
    }
}
