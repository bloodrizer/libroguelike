/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.game.ai;

import com.nuclearunicorn.libroguelike.events.Event;
import com.nuclearunicorn.libroguelike.events.IEventListener;
import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.utils.Fov;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Administrator
 */
public class AI implements Serializable, IEventListener{

    protected String state;
    protected Map<String,IAIAction> stateMap = new HashMap<String, IAIAction>();

    protected void registerState(String name, IAIAction action){
        stateMap.put(name, action);
    }

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

    @Override
    public void e_on_event(Event event) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    //this event is triggered when Entity Controller hits obstacle
    //implement behavior logic there (e.g. attacking target, etc)
    public void e_on_obstacle(int x, int y) {
        //override me
    }


    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}
