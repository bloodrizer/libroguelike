/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nuclearunicorn.libroguelike.events;


/**
 * Notification Message class.
 * 
 * Forces game client to show temporary message on the screen and fade it slowly
 * 
 * @author bloodrizer
 */
public class ENotificationMessage extends Event{
    public String msg = null;
    
    public ENotificationMessage(String message){
        this.msg = message;
    }
}
