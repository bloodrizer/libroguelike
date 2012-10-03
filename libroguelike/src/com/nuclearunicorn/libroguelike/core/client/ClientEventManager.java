/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.core.client;

import com.nuclearunicorn.libroguelike.core.Game;
import com.nuclearunicorn.libroguelike.events.Event;
import com.nuclearunicorn.libroguelike.events.EventManager;
import com.nuclearunicorn.libroguelike.vgui.NE_GUI_System;

import java.util.ArrayList;


/**
 *
 * @author Administrator
 */

/*
 * It handels local client-side events and shit
 *
 *
 */

public class ClientEventManager {
    public static ArrayList<Event> scheduledEvents = new ArrayList<Event>();

    public static EventManager eventManager = new EventManager(){
        
        @Override
        public void notify_event(Event event){  
            
            NE_GUI_System ui =  Game.get_game_mode().get_ui().get_nge_ui();
            if(ui!=null){
                ui.e_on_event(event);
            }

            /*
             *  Note, that event manager does not notify
             *  GUI System as regular listener.
             *  It makes explicit call to ensure that
             *  message is registered by GUI overlay first
             *  and dispatched if nececary
             */
            
            super.notify_event(event);
        }
        
    };

    public static void addEvent(Event event){
        scheduledEvents.add(event);
    }

    public static synchronized void update(){
            for (Event event: scheduledEvents){
                System.out.println("posting scheduled event of type "+event.classname());
                event.post();
            }
            scheduledEvents.clear();
    }
}
