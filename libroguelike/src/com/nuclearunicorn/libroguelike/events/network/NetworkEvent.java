/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.events.network;

import com.nuclearunicorn.libroguelike.events.Event;

/**
 *
 * @author Administrator
 */
@interface NetID {
    String id();
}


public class NetworkEvent extends Event {

    @Override
    public boolean is_local(){
        return local;
    }

    private boolean local = false;
    public void set_local(boolean b) {
        this.local = true;
    }

    private boolean synchronised = false;
    public boolean is_synchronised(){
        return synchronised;
    }

    public void synchronise(){
        synchronised = true;
    }

    public String get_id(){
        return "0x0000";
    }

    public String[] serialize(){
        return new String[] {get_id()};
    }
}
