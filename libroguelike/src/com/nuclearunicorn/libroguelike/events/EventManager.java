package com.nuclearunicorn.libroguelike.events;

import java.util.ArrayList;
import java.util.List;

/**
 * EventManager keeps a track of event stack and makes a transitive rollback if event is timed out.
 */
public class EventManager {

    public static int EVENT_TIMEOUT = 500000; //in ms

    List<IEventListener> listeners = new ArrayList<IEventListener>();

    public void subscribe(IEventListener listener){
        if (!listeners.contains(listener)){
            listeners.add(listener);
        }
    }

    public void notify_event(Event event){
        
        if(event == null){
            return;
        }

        if (event.is_dispatched()){
            return; //do not allow to handle events, catched by gui overlay
        }

        //WARN: problematic place
        //use defensive copy of list and than iterate it
        //otherwase obscure ConcurentModification exception
        for(IEventListener listener: listeners.toArray(new IEventListener[0])){
            listener.e_on_event(event);
        }

    }

    public static void rollback_event(Event event){

    }

    public boolean hasListener(IEventListener listener) {
        return listeners.contains(listener);
    }
}
