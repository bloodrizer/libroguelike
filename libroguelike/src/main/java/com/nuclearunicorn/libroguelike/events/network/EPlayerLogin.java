/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.events.network;

/**
 *
 * @author Administrator
 */
public class EPlayerLogin extends NetworkEvent{
    @Override
    public String get_id(){
        return "0x0010";
    }
}
