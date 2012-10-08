/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.events;

import com.nuclearunicorn.libroguelike.core.client.ClientEventManager;

/**
 *
 * @author Administrator
 */
public class Event {

    private int eventid = 0;
    private long timestamp = 0;

    EventManager manager = null;

    private boolean dispatched = false;
    public boolean is_dispatched(){
        return dispatched;
    }

    public void dispatch(){
        dispatched = true;
    }

    //todo: move to network event?
    public void set_timestamp(long timestamp){
        this.timestamp = timestamp;
    }

    public long get_timestamp(){
        return timestamp;
    }

    public long get_age(long timestamp){
        return timestamp - this.timestamp;
    }
    //todo end
    
    public void set_eventid(int eventid){
        this.eventid = eventid;
    }
    public int get_eventid(){
        return eventid;
    }

    public void setManager(EventManager manager){
        this.manager = manager;
    }

    public void post(){
        if (manager==null){
            //hack for easy migration
            manager = ClientEventManager.getEventManager();
            //throw new RuntimeException("No event manager to dispatch posted event");
        }
        
        manager.notify_event(this);
    }

    public void rollback(){
        //override me
    }

    public String classname(){
        return this.getClass().getName();
    }
    //todo: move to local event?
    public boolean is_local(){
        return true;
    }

}
