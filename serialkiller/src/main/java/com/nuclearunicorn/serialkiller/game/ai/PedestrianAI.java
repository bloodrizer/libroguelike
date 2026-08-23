package com.nuclearunicorn.serialkiller.game.ai;

import com.nuclearunicorn.libroguelike.events.Event;
import com.nuclearunicorn.serialkiller.game.ai.behavior.DeliberateAction;
import com.nuclearunicorn.serialkiller.game.ai.behavior.FleeAction;
import com.nuclearunicorn.serialkiller.game.ai.behavior.GoHomeAction;
import com.nuclearunicorn.serialkiller.game.ai.behavior.PatrolAction;
import com.nuclearunicorn.serialkiller.game.ai.behavior.RapeAction;
import com.nuclearunicorn.serialkiller.game.ai.behavior.SexAction;
import com.nuclearunicorn.serialkiller.game.ai.behavior.SleepAction;
import com.nuclearunicorn.serialkiller.game.ai.llm.sense.GameTurn;
import com.nuclearunicorn.serialkiller.game.events.NPCWitnessCrimeEvent;
import com.nuclearunicorn.serialkiller.game.social.SocialController;
import com.nuclearunicorn.serialkiller.game.sound.SoundHeard;

/**
 * An ordinary person: patrols the streets, goes home at night, runs from anyone who hurts
 * them, and calls the police about anything alarming.
 *
 * <p>The priorities below are the whole of what it means to be a civilian here. {@link
 * PoliceAI} inherits the machinery and changes exactly these, which is why there is no
 * longer a role flag anywhere: a policeman is not a pedestrian with {@code role=POLICE}, he
 * is a pedestrian who declines to flee and registers two urges of his own.
 */
public class PedestrianAI extends TownAI {

    // Priorities. Spaced out so a subclass can slot something in without renumbering.
    protected static final int PRIORITY_THREAT = 100;
    protected static final int PRIORITY_SUSPECT = 90;
    protected static final int PRIORITY_RAPE = 85;      // libido maxed, no outlet -> frenzy
    protected static final int PRIORITY_SLEEP = 60;
    protected static final int PRIORITY_CRIME_SCENE = 50;
    protected static final int PRIORITY_SEX = 45;       // libido needy, partner available
    protected static final int PRIORITY_PLAN = 40;
    protected static final int PRIORITY_NIGHT = 30;
    protected static final int PRIORITY_PATROL = 10;

    public PedestrianAI() {
        super();

        registerState(FleeAction.STATE, new FleeAction(this));
        registerState(GoHomeAction.STATE, new GoHomeAction(this));
        registerState(SleepAction.STATE, new SleepAction(this));
        registerState(PatrolAction.STATE, new PatrolAction(this));
        registerState(DeliberateAction.STATE, new DeliberateAction(this));
        registerState(SexAction.STATE, new SexAction(this));
        registerState(RapeAction.STATE, new RapeAction(this));

        registerImpulse(PRIORITY_THREAT, new FleeAction.Trigger(this));
        // Libido is a homeostatic need, not a whim: the frenzy outranks everything but
        // self-preservation, and even the lawful outlet outranks the planner. Both are
        // gated on "awake" inside their triggers, so a sleeper is never roused by either.
        registerImpulse(PRIORITY_RAPE, new RapeAction.Trigger(this));
        // The night is two states, not one: walking home, then being in the bed. They split
        // on "are we there yet", so only one of the pair is ever relevant - and they sit on
        // opposite sides of the planner, because only one of them is something you can be
        // talked out of.
        //
        // Sleep above it: a completion that lands while its author is in bed is not a reason
        // to sit up and deliver it. Below the planner, that is exactly what happened - the
        // plan impulse won for the single turn it took to say a line, and the town spent the
        // night making small talk in its sleep (INVARIANTS D2).
        registerImpulse(PRIORITY_SLEEP, new SleepAction.Trigger(this));
        // The commute below it, on purpose: while the model has something for us to do we do
        // it, and drift home only once the plan runs out. Above the plan and a town with
        // inference on simply goes to bed at dusk and stops being worth watching.
        registerImpulse(PRIORITY_PLAN, new DeliberateAction.Trigger(this));
        registerImpulse(PRIORITY_SEX, new SexAction.Trigger(this));
        registerImpulse(PRIORITY_NIGHT, new GoHomeAction.Trigger(this));
        registerImpulse(PRIORITY_PATROL, new PatrolAction.Trigger());
    }

    /**
     * Civilians are the town's sensor net. Seeing a crime, or hearing one happen out of
     * sight, both end in the same phone call — which is the only way an officer four streets
     * away ever learns that anything happened.
     */
    @Override
    public void e_on_event(Event event) {
        super.e_on_event(event);
        if (!ensureWired()) {
            return;
        }
        if (event instanceof NPCWitnessCrimeEvent) {
            NPCWitnessCrimeEvent crime = (NPCWitnessCrimeEvent) event;
            SocialController.reportCrime(crime.origin, crime.criminal, human(), GameTurn.current());
            return;
        }
        if (event instanceof SoundHeard) {
            SoundHeard heard = (SoundHeard) event;
            if (!heard.kind().isSuspicious()) {
                return;     // footsteps and chatter are heard and ignored
            }
            // Deliberately not gated on sleep, unlike everything routed through sense(). A
            // sound that got this far already beat the sleeper's own threshold — Acoustics
            // added HEAR_ASLEEP before delivering it — and a scream through the wall at 3am
            // getting called in is the entire point of that constant. Calling the police is
            // also the one reaction that is not talking, moving or acting on the scene, so
            // it costs nothing against INVARIANTS D2.
            // Heard, not seen: no suspect to name, so this only ever produces a scene to
            // go and look at. Naming one from a noise is how a rumour becomes an arrest.
            //
            // The place reported is the sound's true origin, which is more than this NPC
            // actually knows — they have a direction, not a map reference. Correcting that
            // means walking heard.fromDir, and belongs with InvestigateAction rather than
            // here, where it would silently break every existing crime-scene consumer.
            SocialController.reportCrime(heard.getOrigin(), null,
                    human(), GameTurn.current());
        }
    }
}
