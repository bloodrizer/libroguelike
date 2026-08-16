/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.events;

import com.nuclearunicorn.libroguelike.vgui.NE_GUI_Element;
import org.lwjgl.util.Point;

/**
 *
 * @author Administrator
 */
public class EGUIDrop extends Event{

     public Point coord;
     public NE_GUI_Element element;

     public EGUIDrop(Point coord, NE_GUI_Element element){
        this.coord = coord;
        this.element = element;
     }
}
