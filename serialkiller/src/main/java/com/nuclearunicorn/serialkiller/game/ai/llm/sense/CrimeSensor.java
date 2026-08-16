package com.nuclearunicorn.serialkiller.game.ai.llm.sense;

import com.nuclearunicorn.libroguelike.core.client.ClientGameEnvironment;
import com.nuclearunicorn.libroguelike.events.Event;
import com.nuclearunicorn.libroguelike.events.IEventListener;
import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.utils.Fov;
import com.nuclearunicorn.serialkiller.game.ai.PoliceAI;
import com.nuclearunicorn.serialkiller.game.ai.TownAI;
import com.nuclearunicorn.serialkiller.game.ai.llm.LlmDebug;
import com.nuclearunicorn.serialkiller.game.combat.RLCombat;
import com.nuclearunicorn.serialkiller.game.events.CriminalActionEvent;
import com.nuclearunicorn.serialkiller.game.events.NPCWitnessCrimeEvent;
import com.nuclearunicorn.serialkiller.game.world.entities.EntityRLHuman;

/**
 * Who actually saw it. Turns a {@link CriminalActionEvent} into a
 * {@link NPCWitnessCrimeEvent} for every person close enough to have witnessed it, using
 * <b>their</b> field of view rather than the player's.
 *
 * <p>The test this replaces lived in {@code RLWorldModel} and read
 * {@code ((RLTile)ent.tile).isVisible()} — the flag the renderer sets on tiles <i>the player
 * can currently see</i>. So "witnessing a crime" meant "standing somewhere lit and on
 * screen", which produced two symmetrical absurdities: a crate got a crime report because it
 * happened to be in shot, and a policeman standing in an unlit street eleven tiles away got
 * nothing at all, four times over, while the player beat two people to death in front of him.
 *
 * <p>The event also went to every actor in the layer regardless of distance and was then
 * filtered by that flag, which is the wrong way round: distance first, cheaply, then per-agent
 * perception. Sensing is per-agent — that is what makes an agent able to miss something.
 */
public class CrimeSensor implements IEventListener {

    private static CrimeSensor instance;

    /** Fallback sight range for anyone without combat stats to ask. */
    private static final int DEFAULT_FOV = 8;

    /** Re-subscribes on every world; see {@link HearingSensor#init} for why that matters. */
    public static void init() {
        if (instance == null) {
            instance = new CrimeSensor();
        }
        ClientGameEnvironment.getEnvironment().getEventManager().subscribe(instance);
        LlmDebug.log("crime sensor subscribed");
    }

    @Override
    public void e_on_event(Event event) {
        if (!(event instanceof CriminalActionEvent)) {
            return;
        }
        CriminalActionEvent crime = (CriminalActionEvent) event;
        if (crime.criminal == null || crime.origin == null || !isCrime(crime)) {
            return;
        }

        // Widest sight range anyone might have, so the cheap radius query cannot miss a
        // witness the per-agent check would have accepted.
        Entity[] nearby = Fov.get_entity_in_radius(
                ClientGameEnvironment.getEnvironment().getEntityManager(),
                crime.origin, MAX_FOV, crime.criminal.getLayerId());

        int witnesses = 0;
        for (Entity ent : nearby) {
            if (ent == crime.criminal || !(ent instanceof EntityRLHuman)
                    || !(ent.getAI() instanceof TownAI)) {
                continue;
            }
            if (!Fov.in_range(ent.origin, crime.origin, sightRadius(ent))) {
                continue;
            }
            ent.getAI().e_on_event(new NPCWitnessCrimeEvent(crime.origin, crime.criminal));
            witnesses++;
        }
        LlmDebug.log("crime by %s at %d,%d seen by %d",
                crime.criminal.get_uid(), crime.origin.getX(), crime.origin.getY(), witnesses);
    }

    private static final int MAX_FOV = 24;

    /**
     * Violence against a person, by someone with no right to use it.
     *
     * <p>Both halves are load-bearing and both were missing. Every swing at a crate or a
     * locked door raises the same event, so without the victim check a pedestrian shouldering
     * a door open is reported as an assault; and an officer's baton lands through the same
     * code path as a murder, so without the second check the first arrest starts a chain
     * reaction — witnesses report the officer, the other three arrest <i>him</i>, and their
     * blows are reported in turn. Measured: eleven blows in a session became thirty-two.
     *
     * <p>The old code got the same effect by only counting crimes committed by the player,
     * which also meant no NPC could ever commit one.
     */
    private static boolean isCrime(CriminalActionEvent crime) {
        if (!(crime.victim instanceof EntityRLHuman) && !crime.victim.isPlayerEnt()) {
            return false;
        }
        return !(crime.criminal.getAI() instanceof PoliceAI);
    }

    private static int sightRadius(Entity ent) {
        return ent.get_combat() instanceof RLCombat
                ? ((RLCombat) ent.get_combat()).getFovRadius() : DEFAULT_FOV;
    }
}
