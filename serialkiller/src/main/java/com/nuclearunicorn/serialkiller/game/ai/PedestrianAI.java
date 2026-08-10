package com.nuclearunicorn.serialkiller.game.ai;

import com.nuclearunicorn.libroguelike.events.Event;
import com.nuclearunicorn.serialkiller.game.ai.behavior.DeliberateAction;
import com.nuclearunicorn.serialkiller.game.ai.behavior.FleeAction;
import com.nuclearunicorn.serialkiller.game.ai.behavior.GoHomeAction;
import com.nuclearunicorn.serialkiller.game.ai.behavior.PatrolAction;
import com.nuclearunicorn.serialkiller.game.ai.behavior.SleepAction;
import com.nuclearunicorn.serialkiller.game.ai.llm.sense.GameTurn;
import com.nuclearunicorn.serialkiller.game.events.NPCWitnessCrimeEvent;
import com.nuclearunicorn.serialkiller.game.events.SuspiciousSoundEvent;
import com.nuclearunicorn.serialkiller.game.social.SocialController;

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
    protected static final int PRIORITY_CRIME_SCENE = 50;
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

        registerImpulse(PRIORITY_THREAT, new FleeAction.Trigger(this));
        // Above sleep on purpose: while the model has something for us to do we do it, and
        // drift home only once the plan runs out. Below it and a town with inference on
        // simply goes to bed at dusk and stops being worth watching.
        registerImpulse(PRIORITY_PLAN, new DeliberateAction.Trigger(this));
        // The night is two states, not one: walking home, then being in the bed. They split
        // on "are we there yet", so only one of the pair is ever relevant.
        registerImpulse(PRIORITY_NIGHT, new SleepAction.Trigger(this));
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
        if (event instanceof SuspiciousSoundEvent) {
            // Heard, not seen: no suspect to name, so this only ever produces a scene to
            // go and look at. Naming one from a noise is how a rumour becomes an arrest.
            SocialController.reportCrime(((SuspiciousSoundEvent) event).getOrigin(), null,
                    human(), GameTurn.current());
        }
    }
}
