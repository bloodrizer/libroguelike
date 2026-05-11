/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.events;

/**
 *
 * @author Administrator
 */
public class EKeyPress extends Event {
    public int key;
    public char chr;
    
    public EKeyPress(int key, char chr){
        this.key = key;
        this.chr = chr;
    }
}
