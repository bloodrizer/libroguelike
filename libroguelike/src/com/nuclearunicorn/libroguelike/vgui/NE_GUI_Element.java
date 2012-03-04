/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.vgui;

import com.nuclearunicorn.libroguelike.core.Input;
import com.nuclearunicorn.libroguelike.events.*;
import com.nuclearunicorn.libroguelike.game.ui.IUserInterface;
import com.nuclearunicorn.libroguelike.render.WindowRender;

import java.util.ArrayList;
import org.lwjgl.util.Point;

/**
 *
 * @author Administrator
 */
public class NE_GUI_Element {
    ArrayList<NE_GUI_Element> children = new ArrayList<NE_GUI_Element>();

    public void add(NE_GUI_Element child){
        children.add(child);
        child.set_parent(this);
    }
    public void add_at(int index, NE_GUI_Element child){
        children.add(index, child);
        child.set_parent(this);
    }

    public boolean remove(NE_GUI_Element child){
        return children.remove(child);
    }

    public int w = 200;
    public int h = 200;

    public int x = 100;
    public int y = 100;

    public int get_x(){
        if (parent!=null){
            return this.x + parent.get_x();
        }
            return this.x;
    }

    public int get_y(){
        if (parent!=null){
            return this.y + parent.get_y();
        }
            return this.y;
    }

    public void set_coord(int x, int y){
        this.x = x;
        this.y = y;
    }

    public void set_size(int x, int y, int w, int h){
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }


    protected NE_GUI_Element parent = null;
    public void set_parent(NE_GUI_Element parent){
        this.parent = parent;
    }

    public boolean visible = true;
    public boolean solid = true;

    public void render(){
        if (visible){
            render_children();
        }
    }

    public void render_children(){
        if (children.isEmpty()){
            return;
        }

        Object[] elem =  children.toArray();
        for(int i = 0; i<elem.length; i++){
            NE_GUI_Element __elem = (NE_GUI_Element)elem[i];
            __elem.render();
        }
    }

    public boolean dragable = true;
    boolean drag_start = false;

    /*
     * This method is called by a NE_GUI_System when
     * InputEvent is generated
     *
     * Method should route event through the children tree
     * until event is not dispatched
     */
    public void notify_event(Event e){
        //invisible controls do not handle events
        if(!visible){
            return;
        }

        //System.out.println("Sending to children ["+children.size()+"]");

        //allow children to catch event and dispatch it,
        //before we handle it in parent control

        //note that we iterate items in the reverse order as larger index means larger z-order and
        //therefore larger priority in event dispatching

        Object[] elem =  children.toArray();
        for(int i = elem.length-1; i>=0; i--){
            NE_GUI_Element __elem = (NE_GUI_Element)elem[i];
            __elem.notify_event(e);
        }
        
        //catch mouse click inside of control area
        //System.out.println("Checking if event "+e.toString()+"is EMouseClick ");
        if (e instanceof EMouseClick){

            EMouseClick event = (EMouseClick)e;

            //this hack allows other controls to loose focus, even if event was dispatched
            if (event.is_dispatched()){
                //System.out.println("Event is dispatched, lol, loosing focus");
                e_on_mouse_out_click(event);
                return;
            }

            int mx = event.origin.getX();
            int my = WindowRender.get_window_h() - event.origin.getY();

            /*
             * now we should translate this coords into
             * local coord system, set by our parent control
             */
            /*if (parent != null){
                mx = mx - parent.get_x();
                my = my - parent.get_y();
            }*/
            //System.out.println("Checking if event in aabb");

            if( is_client_rect(mx,my)){
                if(!solid){
                    return; //do not check bounding for non-solid controls
                }
                
                /*System.out.println(
                        this.toString()+" event in working area!"
                );
                System.out.println(event.origin);*/

                e.dispatch();
                e_on_mouse_click(event);
                drag_start = true;
            }else{
                e_on_mouse_out_click(event);
            }
        }

         if (e instanceof EMouseRelease && !e.is_dispatched()){
             if (drag_start){   //this meens, object was in drag state, so we should trigger drop event
                drop();
             }
             drag_start = false;
         }

        if (e instanceof EMouseDrag && !e.is_dispatched()){
            EMouseDrag event = (EMouseDrag)e;

            if (drag_start && dragable){

                drag((int)event.dx, (int)event.dy);
                e_on_drag();
            }

        }

        if (e instanceof EGUIDrop && !e.is_dispatched()){
            EGUIDrop event = (EGUIDrop)e;

            if (is_client_rect(event.coord.getX(), event.coord.getY())){
                e_on_grab(event);
            }
        }

        if (e instanceof EKeyPress){
            EKeyPress event = (EKeyPress)e;

            e_on_key_press(event);
        }
    }

    boolean is_client_rect(int mx, int my){
        return  mx > get_x()     &&
                mx < get_x()+w   &&
                my > get_y()     &&
                my < get_y()+h;
    }

    public void drag(int dx, int dy){
        x = x + dx;
        y = y - dy;
    }

    public void drop(){
        e_on_drop();


        EGUIDrop drop_event = new EGUIDrop(new Point(Input.get_mx(),Input.get_my()), this);
        drop_event.post();
    }

    public void e_on_mouse_click(EMouseClick e){
        System.out.println(this+"::click");
    }

    public void e_on_drop(){
        //override me!
    }

    public void e_on_drag(){
        //override me!
    }

    public void e_on_grab(EGUIDrop event){
        //override me!
    }

    public void e_on_mouse_out_click(EMouseClick e){
        //override me!
    }
    
    public void e_on_key_press(EKeyPress e){
        //override me!
    }

    public void clear() {
        Object[] elem =  children.toArray();
        for(int i = 0; i<elem.length; i++){
            NE_GUI_Element __elem = (NE_GUI_Element)elem[i];
            __elem.clear();
        }

        children.clear();
    }


    private IUserInterface controller;
    public void set_controller(IUserInterface controller){
        this.controller = controller;
    }

    @Override
    public String toString(){
        return "GUIElement[]";
    }

    public void toggle(){
        this.visible = !this.visible;
    }

    //ultra-useful helper function to set window on center
    public void center(){
        if (parent == null){
            this.x = WindowRender.get_window_w()/2 - w/2;
            this.y = WindowRender.get_window_h()/2 - h/2;
        }else{
            this.x = parent.w/2 - w/2;
            this.y = parent.h/2 - h/2;
        }
    }
    
     /*
     * This function should return topmost gui element, related to the current gui object hierarchy tree
     */
    
    public NE_GUI_Element get_gui_element(int mx, int my){
        if (is_client_rect(mx, my)){
            
            Object[] elem =  children.toArray();
            for(int i = 0; i<elem.length; i++){
                NE_GUI_Element __elem = (NE_GUI_Element)elem[i];

                NE_GUI_Element child_subelem = __elem.get_gui_element(mx, my);
                if (child_subelem != null && child_subelem.visible){
                    return child_subelem;
                }

                if (__elem.is_client_rect(mx, my)){
                    return __elem;
                }
            }
            
            return this;
        }
        return null;
    }

    public String get_tooltip_message() {
        return this.toString();
    }

}
