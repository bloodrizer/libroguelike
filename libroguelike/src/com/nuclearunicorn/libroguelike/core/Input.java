/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.core;

/**
 *
 * @author Administrator
 */
import com.nuclearunicorn.libroguelike.events.EKeyPress;
import com.nuclearunicorn.libroguelike.events.EMouseClick;
import com.nuclearunicorn.libroguelike.events.EMouseDrag;
import com.nuclearunicorn.libroguelike.events.EMouseRelease;
import com.nuclearunicorn.libroguelike.render.WindowRender;

import java.util.Collections;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.util.Point;


public class Input {

    public static float dx = 0;
    public static float dy = 0;

    private static boolean lmb_pressed = false;
    private static boolean rmb_pressed = false;

    public enum MouseInputType {
        LCLICK,
        RCLICK
    };

    public static boolean key_state_alt = false;
    public static boolean key_state_ctrl = false;
    private static java.util.Map<Integer,Boolean> key_states
            = Collections.synchronizedMap(new java.util.HashMap<Integer,Boolean>(100));
    

    public static int get_mx(){
        return Mouse.getX();
    }

    /*
     * Return mouse Y coord in *INVERTED* screen coord system (0 at top, H at bottom)
     */
    public static int get_my(){
        return WindowRender.get_window_h()-Mouse.getY();
    }

    public static void update(){

        //Mouse.
        while (Mouse.next()){

            int btn = Mouse.getEventButton();

            //distance in mouse movement from the last getDX() call.
            dx = Mouse.getEventDX();
            //distance in mouse movement from the last getDY() call.
            dy = Mouse.getEventDY();

            int x = Mouse.getX();
            int y = Mouse.getY();

            //--------------------------------LMB-----------------------------------
            if (Mouse.isButtonDown(0)){
                if (!lmb_pressed){
                    lmb_pressed = true;
                    new EMouseClick(new Point(Mouse.getX(),Mouse.getY()), MouseInputType.LCLICK).post();
                }else{  //mouse was pressed on previouse polling, so it should be draging
                     new EMouseDrag(dx, dy, MouseInputType.LCLICK).post();
                }

            }else{
                if (lmb_pressed){
                     new EMouseRelease( MouseInputType.LCLICK).post();
                }
                lmb_pressed = false;
            }
            //--------------------------------RMB-----------------------------------
            if (Mouse.isButtonDown(1)){
                if (!rmb_pressed){
                    rmb_pressed = true;
                    new EMouseClick(new Point(Mouse.getX(),Mouse.getY()), MouseInputType.RCLICK).post();
                }else{  //mouse was pressed on previouse polling, so it should be draging
                     Mouse.setGrabbed(true);
                     new EMouseDrag(dx, dy, MouseInputType.RCLICK).post();
                }
            }else{
                if (rmb_pressed){
                    Mouse.setGrabbed(false);
                    new EMouseRelease( MouseInputType.RCLICK).post();
                }
                rmb_pressed = false;
            }
        }


        //Keyboard Shit

        while (Keyboard.next()) {
	    if (Keyboard.getEventKeyState()) {
                if (Keyboard.getEventKey() == Keyboard.KEY_LMENU) {
		    key_state_alt = true;
		}
                if (Keyboard.getEventKey() == Keyboard.KEY_LCONTROL) {
		    key_state_ctrl = true;
		}

                Boolean state = key_states.get(Keyboard.getEventKey());
                if(state == null || state == false){
                    EKeyPress event = new EKeyPress(
                                Keyboard.getEventKey(),
                                Keyboard.getEventCharacter()
                            );
                    event.post();
                }
                key_states.put(Keyboard.getEventKey(), true);

            }else{
                if (Keyboard.getEventKey() == Keyboard.KEY_LMENU) {
		    key_state_alt = false;
		}
                 if (Keyboard.getEventKey() == Keyboard.KEY_LCONTROL) {
		    key_state_ctrl = false;
		}
                key_states.put(Keyboard.getEventKey(), false);
            }
        }

        //----------------------------------------------------------------------
        
    }
}
