/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.events;

import com.nuclearunicorn.libroguelike.core.Input;

/**
 *
 * @author Administrator
 */
public class EMouseRelease extends Event{
    public Input.MouseInputType type = null;

    public EMouseRelease(Input.MouseInputType type){
        this.type = type;
    }
}
