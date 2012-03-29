/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.vgui;

import com.nuclearunicorn.libroguelike.events.EMouseClick;
import com.nuclearunicorn.libroguelike.game.actions.IAction;

/**
 *
 * @author Administrator
 */
public class NE_GUI_Popup extends NE_GUI_Element {

    public NE_GUI_Popup(){
        solid = false;
    }

    class NE_GUI_Popup_item extends NE_GUI_Button{
        public IAction action;

        @Override
        public void e_on_mouse_click(EMouseClick e){
            //System.out.println("executing action "+action);
            action.execute();
            e.dispatch();
        }

        @Override
        public String toString(){
            return "GUI[Popup_item]";
        }
    }

    //Collection<NE_GUI_Popup_item> items = new ArrayList<NE_GUI_Popup_item>();

    public void add_item(IAction action){
        NE_GUI_Popup_item item = new NE_GUI_Popup_item();
        item.action = action;
        //items.add(item);

        this.add(item);

        item.set_tw(4);
        item.x = 0;
        item.y = item.h*(children.size()-1);
        item.text = action.get_name();
        item.dragable = false;
    }

    @Override
    public void e_on_mouse_out_click(EMouseClick e){
        this.clear();
        parent.children.remove(this);
    }
}
