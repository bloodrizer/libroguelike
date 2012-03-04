/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.game.ent;

import com.nuclearunicorn.libroguelike.game.actions.IAction;

import java.util.ArrayList;

/**
 *
 * @author Administrator
 */
public class EntActionList {
    ArrayList<IAction<Entity>> action_list = new ArrayList<IAction<Entity>>(5);

    Entity owner = null;
    public void set_owner( Entity owner){
        this.owner = owner;
    }

    public void add_action(IAction<Entity> action, String name){
        action_list.add(action);
        action.set_name(name);
        action.set_owner(owner);
    }

    public ArrayList get_action_list(){
        //wtf there
        return action_list;
    }
}
