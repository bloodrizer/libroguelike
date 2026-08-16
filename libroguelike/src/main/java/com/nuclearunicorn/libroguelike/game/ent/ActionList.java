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
public class ActionList<T> {
    ArrayList<IAction<T>> action_list = new ArrayList<IAction<T>>(5);

    T owner = null;
    public void set_owner( T owner){
        this.owner = owner;
    }

    public void add_action(IAction<T> action, String name){
        action_list.add(action);
        action.set_name(name);
        action.set_owner(owner);
    }

    public ArrayList get_action_list(){
        //wtf there
        return action_list;
    }

    public void addAll(ArrayList superList) {
        action_list.addAll(superList);
    }
}
