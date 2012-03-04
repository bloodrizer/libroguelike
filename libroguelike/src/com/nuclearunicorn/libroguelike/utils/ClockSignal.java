/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nuclearunicorn.libroguelike.utils;

/**
 *
 * @author bloodrizer
 * 
 * Basic component for every thinkable object, that able to perform actions based on
 * given time interval
 */
public class ClockSignal {
    private long last_signal;
    private long idle_time;
    
    public final static boolean FORCE_AUTORESET = true;
            
    public ClockSignal(){
        last_signal = Timer.get_time();
    }
    
    /*
     * updates idle time after last signal generation
     */
   
    public boolean get_signal(long period){
        return get_signal(period, FORCE_AUTORESET);
    }
    
    public boolean get_signal(long period, boolean autoreset){
        if (Timer.get_time() - last_signal > period) {
            if (autoreset){
                reset();
            }
            return true;
        }
        return false;
    }
    
    public void reset(){
        last_signal = Timer.get_time();
    }
}
