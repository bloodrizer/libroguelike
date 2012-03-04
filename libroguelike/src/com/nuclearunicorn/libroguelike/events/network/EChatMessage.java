/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.events.network;

/**
 *
 * @author Administrator
 */
public class EChatMessage extends NetworkEvent{
    public int uid;
    public String message;
    public EChatMessage(int uid, String message){
        this.uid = uid;
        this.message = message;
    }

    @Override
    public String get_id(){
        return "0x02A0";

    }

    @Override
    public String[] serialize(){
        return new String[] {
            get_id(),
            message
        };
    }
}
