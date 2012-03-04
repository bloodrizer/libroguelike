/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.utils;

/**
 *
 * @author Administrator
 */
public class NLTimer {
    public long time = 0;

    public void push(){
        time = System.nanoTime();
    }

    public float popDiff(){
        return (float)( System.nanoTime() - time ) / (1000*1000);
    }

    public void pop(){
        float time_diff = (float)( System.nanoTime() - time ) / (1000*1000);
        System.out.println( "NLTimer : " + Float.toString(time_diff) + " ms elasped");
    }

    public void pop(String message){
        float time_diff = (float)( System.nanoTime() - time ) / (1000*1000);
        System.out.println( "NLTimer : " + Float.toString(time_diff) + " ms elasped. (" + message + ")" );
    }
}
