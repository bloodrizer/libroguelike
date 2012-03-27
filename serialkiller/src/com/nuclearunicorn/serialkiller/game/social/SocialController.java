package com.nuclearunicorn.serialkiller.game.social;

import com.nuclearunicorn.libroguelike.core.client.ClientEventManager;
import com.nuclearunicorn.libroguelike.core.client.ClientGameEnvironment;
import com.nuclearunicorn.libroguelike.events.Event;
import com.nuclearunicorn.libroguelike.events.IEventListener;
import com.nuclearunicorn.serialkiller.game.events.NPCReportCrimeEvent;
import org.lwjgl.util.Point;

import java.util.ArrayList;
import java.util.List;

/**

 */
public class SocialController implements IEventListener{

    public static List<Point> crimeplaces = new ArrayList<Point>();

    public static SocialController instance = new SocialController();

    public static void init(){
        ClientGameEnvironment.getEnvironment().getEventManager().subscribe(instance);
    }

    @Override
    public void e_on_event(Event event) {
        if (event instanceof NPCReportCrimeEvent){
            if (crimeplaces.contains(((NPCReportCrimeEvent) event).getOrigin())){
                return;
            }
            addCrimeplace(((NPCReportCrimeEvent) event).getOrigin());
        }
    }
    
    public void addCrimeplace(Point crimeplace){
        crimeplaces.add(crimeplace);
    }
}
