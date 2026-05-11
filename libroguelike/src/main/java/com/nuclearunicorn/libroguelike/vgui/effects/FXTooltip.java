/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.vgui.effects;

import com.nuclearunicorn.libroguelike.utils.Timer;
import com.nuclearunicorn.libroguelike.vgui.NE_GUI_Element;

/**
 *
 * @author Administrator
 */
public class FXTooltip extends FXTextBubble {
    private boolean enabled = true;
    
    NE_GUI_Element gui_elem;
    
    public FXTooltip(NE_GUI_Element gui_elem){
        super(null);    //no message event for tooltip
        life_time = 1500;   //1.5k ms for nice opaque rendering
        
        this.gui_elem = gui_elem;
    }

    public void refresh(){
        spawn_time = Timer.get_time();
    }

    private boolean is_expired() {
        return enabled;
    }
    
    public void disable(){
        enabled = false;
    }

    //hacky shit. I never have time to correctly handle tooltip life cycle, so we just 'prolongate' tooltip life if it's enabled
    @Override
    public void update(){
        if (enabled){
            spawn_time = Timer.get_time();
        }else{
            life_time = 400;    //400 ms to hide qute fast
        }
        super.update();
    }

    @Override
    public String get_message(){
        
        /*if (gui_elem instanceof NE_GUI_InventoryItem){
            NE_GUI_InventoryItem item_elem = (NE_GUI_InventoryItem)gui_elem;
            BaseItem item = item_elem.get_item();
            if ( item != null){
                return item.get_type();
            }
        }*/

        if (gui_elem != null){
            return gui_elem.get_tooltip_message();
        }

        return "undefined";
    }
    
    @Override
    public void render(){

        if (gui_elem == null){
            return;
        }
        
        render_bubble(gui_elem.get_x() - get_message().length()/2*8 + 15, gui_elem.get_y() - 5);
    }
    
    

}
