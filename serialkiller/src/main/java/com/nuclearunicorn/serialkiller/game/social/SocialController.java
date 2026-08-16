package com.nuclearunicorn.serialkiller.game.social;

import com.nuclearunicorn.libroguelike.core.client.ClientGameEnvironment;
import com.nuclearunicorn.libroguelike.events.Event;
import com.nuclearunicorn.libroguelike.events.IEventListener;
import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.game.ent.EntityActor;
import com.nuclearunicorn.libroguelike.game.player.Player;
import com.nuclearunicorn.serialkiller.game.ai.PoliceAI;
import com.nuclearunicorn.serialkiller.game.ai.llm.LlmDebug;
import com.nuclearunicorn.serialkiller.game.events.NPCReportCrimeEvent;
import com.nuclearunicorn.serialkiller.render.RLMessages;
import org.lwjgl.util.Point;
import org.newdawn.slick.Color;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The town's shared board: what has been reported, and who needs to hear about it.
 *
 * <p>This is the piece the architecture was missing, and the reason a policeman could stand
 * eleven tiles from a beating and carry on asking a passer-by about a parked car. Every
 * channel an NPC had was <i>perceptual</i> and therefore short-ranged — the hearing sensor
 * reaches ten tiles, the pain sensor's witness sweep reaches ten tiles, and anything
 * point-based landing further away was dropped as out of earshot. Nothing carried knowledge
 * further than a person could shout, so a police force whose crime-detection radius was ten
 * tiles was not a police force.
 *
 * <p>Shared knowledge is standard equipment: F.E.A.R. gives a squad a blackboard to post to,
 * Thief propagates sense links between peers "with controls in place to constrain knowledge
 * cascades". The control here is that a report goes to police and to nobody else — the town
 * does not become collectively omniscient because one person saw something.
 */
public class SocialController implements IEventListener{

    public static List<Point> crimeplaces = new ArrayList<Point>();

    /** place+suspect pairs already broadcast, so one long fight is not forty calls. */
    private static final Set<String> dispatched = new HashSet<String>();

    public static SocialController instance = new SocialController();

    public static void init(){
        // Both of these are per-world, and this is the only place a new world announces
        // itself. Without the clear, "New game" starts you in a town that already has the
        // previous town's murder sites on the map and its reports deduplicated away.
        crimeplaces.clear();
        dispatched.clear();
        ClientGameEnvironment.getEnvironment().getEventManager().subscribe(instance);
    }

    public static List<Point> getCrimeplaces() {
        return crimeplaces;
    }

    /**
     * File a report and put it out over the radio. {@code criminal} may be null when the
     * caller only heard something — a scene to go and look at, with nobody named.
     */
    public static void reportCrime(Point where, EntityActor criminal, EntityActor reporter, long turn) {
        if (where == null) {
            return;
        }
        // Deduplicated on place *and* suspect. A sustained beating raises one of these per
        // blow and is seen by everyone on the street, so without this a single fight puts
        // the same call out forty times; but a later report that finally names someone is
        // new information about a scene already known, and has to get through.
        String key = where.getX() + "," + where.getY() + "/"
                + (criminal == null ? "?" : criminal.get_uid());
        if (!dispatched.add(key)) {
            return;
        }
        if (!hasCrimeplace(where)) {
            instance.addCrimeplace(new Point(where));
            if (reporter != null) {
                RLMessages.message(reporter.getName() + " has reported a crime to the police",
                        Color.orange);
            }
        }

        NPCReportCrimeEvent report = new NPCReportCrimeEvent(where, criminal, reporter);
        int officers = dispatchToPolice(report);
        LlmDebug.log("dispatch: crime at %d,%d suspect=%s -> %d officer(s)",
                where.getX(), where.getY(),
                criminal == null ? "unknown" : criminal.get_uid(), officers);
    }

    /**
     * Hand the report to every officer in the layer, at any distance. A radio has no
     * falloff; that is the entire point of one, and it is what lets an officer who was
     * nowhere near a crime still be the one who answers it.
     */
    private static int dispatchToPolice(NPCReportCrimeEvent report) {
        Entity[] ents = ClientGameEnvironment.getEnvironment()
                .getEntityManager().getEntities(Player.get_zindex());
        int count = 0;
        for (Entity ent : ents) {
            if (ent == report.reporter || !(ent.getAI() instanceof PoliceAI)) {
                continue;
            }
            ent.getAI().e_on_event(report);
            count++;
        }
        return count;
    }

    @Override
    public void e_on_event(Event event) {
        if (event instanceof NPCReportCrimeEvent){
            Point origin = ((NPCReportCrimeEvent) event).getOrigin();
            if (!crimeplaces.contains(origin)){
                addCrimeplace(origin);
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
