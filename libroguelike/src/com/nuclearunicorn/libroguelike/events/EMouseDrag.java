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
public class EMouseDrag extends Event{
    public Input.MouseInputType type = null;

    public float dx;
    public float dy;

    public EMouseDrag(float dx, float dy, Input.MouseInputType type){
        this.dx = dx;
        this.dy = dy;
        this.type = type;
    }
}
