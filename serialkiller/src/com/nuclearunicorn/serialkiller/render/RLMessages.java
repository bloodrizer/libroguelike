package com.nuclearunicorn.serialkiller.render;

import com.nuclearunicorn.libroguelike.events.EventManager;
import com.nuclearunicorn.serialkiller.messages.EConsoleMessage;
import org.newdawn.slick.Color;

/**
 */
public class RLMessages {
    
    static EventManager manager;
    
    public static void setEventManager(EventManager manager){
        RLMessages.manager = manager;
        //manager.subscribe(manager);
    }
    
    public void message(String s) {
        message(s, Color.lightGray);
    }

    public static void message(String s, Color color) {
        EConsoleMessage event = new EConsoleMessage(s, color);
        manager.notify_event(event);
    }
}
