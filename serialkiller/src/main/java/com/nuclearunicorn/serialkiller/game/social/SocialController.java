package com.nuclearunicorn.serialkiller.game.social;

import com.nuclearunicorn.libroguelike.core.client.ClientGameEnvironment;
import com.nuclearunicorn.libroguelike.events.Event;
import com.nuclearunicorn.libroguelike.events.IEventListener;
import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.serialkiller.game.events.NPCReportCrimeEvent;
import com.nuclearunicorn.serialkiller.game.events.SuspiciousSoundEvent;
import com.nuclearunicorn.serialkiller.game.world.entities.EntityRLActor;
import com.nuclearunicorn.serialkiller.utils.RLMath;
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

    public static List<Point> getCrimeplaces() {
        return crimeplaces;
    }

    @Override
    public void e_on_event(Event event) {
        if (event instanceof NPCReportCrimeEvent){
            if (crimeplaces.contains(((NPCReportCrimeEvent) event).getOrigin())){
                return;
            }
            System.out.println(">>> added crimeplace <<<");
            addCrimeplace(((NPCReportCrimeEvent) event).getOrigin());
        }
        if (event instanceof SuspiciousSoundEvent){
            List<Entity> ents = RLMath.getEntitiesInRadius(((SuspiciousSoundEvent) event).getOrigin(), ((SuspiciousSoundEvent) event).radius);
            for (Entity ent: ents){
                if (ent instanceof EntityRLActor){
                    ((EntityRLActor)ent).e_on_event(event);
                }
            }
        }
    }
    
    public void addCrimeplace(Point crimeplace){
        crimeplaces.add(crimeplace);
    }


    public static boolean hasCrimeplace(Point origin) {
        return crimeplaces.contains(origin);
    }
}
