/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nuclearunicorn.libroguelike.events;

/**
 *
 * @author bloodrizer
 */

/*
 * Start client-side action. That will draw timer animation(?) and cancel every pending action
 * 
 */
public class EStartAction {
    /*
     * @action_time - how long takes player to finish this action (in seconds)
     * 
     */
    long action_time;
    public EStartAction(long action_time){
        this.action_time = action_time;
    }
}
