/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nuclearunicorn.libroguelike.events;

import com.nuclearunicorn.libroguelike.vgui.NE_GUI_Element;

/**
 *
 * @author bloodrizer
 */
public class ETooltipShow extends Event{
    
    public NE_GUI_Element element = null;
    
    public ETooltipShow(NE_GUI_Element element){
        this.element = element;
    }
    
}
