/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.events;


import com.nuclearunicorn.libroguelike.core.Input;
import com.nuclearunicorn.libroguelike.render.WindowRender;
import org.lwjgl.util.Point;

/**
 *
 * @author Administrator
 */
public class EMouseClick extends Event {
    public Point origin = null;
    
    public Input.MouseInputType type = null;

    public EMouseClick(Point origin, Input.MouseInputType type){
        this.origin = origin;
        this.type = type;
    }

    public int get_window_y(){
        return WindowRender.get_window_h() - origin.getY();
    }
}
