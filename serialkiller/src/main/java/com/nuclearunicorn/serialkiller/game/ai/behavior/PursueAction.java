package com.nuclearunicorn.serialkiller.game.ai.behavior;

import com.nuclearunicorn.libroguelike.game.ai.IAIAction;
import com.nuclearunicorn.libroguelike.game.ai.Impulse;
import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.game.ent.controller.NpcController;
import com.nuclearunicorn.serialkiller.game.ai.TownAI;
import com.nuclearunicorn.serialkiller.game.ai.llm.LlmDebug;
import com.nuclearunicorn.serialkiller.game.ai.llm.sense.GameTurn;
import com.nuclearunicorn.serialkiller.game.ai.mind.Narrating;
import com.nuclearunicorn.serialkiller.game.ai.mind.Percept;
import com.nuclearunicorn.serialkiller.game.ai.mind.Tuning;
import com.nuclearunicorn.serialkiller.game.controllers.RLController;
import org.lwjgl.util.Point;

/**
 * Close on a wanted person and put hands on them. The mirror of {@link FleeAction}: same
 * rung of the same ladder, pointed the other way.
 *
 * <p>It is a reflex for the same reason fleeing is. An arrest that waits on a model round
 * trip is an arrest that happens three streets later, and the command vocabulary has no verb
 * for it in any case — asked what to do about an assault happening in front of him, the model
 * replies "I'm taking him into custody", which the interpreter can do exactly nothing with.
 * This does it. Adding an {@code attack} verb instead would hand every NPC in town the
 * ability to choose violence, which is a much larger design decision than policing needs.
 */
public class PursueAction implements IAIAction, Narrating {

    public static final String STATE = "ai_state_PURSUING";

    /** Inside this many tiles, step greedily; further out, route. */
    private static final int CONTACT_RANGE = 2;

    private final TownAI brain;
    private String suspectName;

    public PursueAction(TownAI brain) {
        this.brain = brain;
    }

    @Override
    public void onEnter() {
        brain.ctrl().clearPath();
        if (brain.deliberation() != null) {
            brain.deliberation().interrupt();   // this is not a conversation any more
        }
        brain.voice().challenge();
        LlmDebug.log("%s: PURSUE engaged (suspect=%s)", brain.human().get_uid(), suspectUid());
    }

    @Override
    public void act(NpcController npcController) {
        Entity suspect = suspect();
        if (suspect == null) {
            return;
        }
        suspectName = suspect.isPlayerEnt() ? "the player" : suspect.getName();

        RLController ctrl = brain.ctrl();
        int distance = ctrl.distanceToTarget(suspect.origin);

        // Greedy step once they are close, routed while they are not. A* to a target that
        // moves every turn is both expensive and stale by the time it is followed.
        if (distance <= CONTACT_RANGE) {
            ctrl.clearPath();
            ctrl.chaseTarget(suspect);
        } else {
            if (!ctrl.hasPath()) {
                ctrl.set_destination(new Point(suspect.origin));
            }
            if (ctrl.hasPath()) {
                ctrl.follow_path();
            } else {
                ctrl.chaseTarget(suspect);
            }
        }
        if (brain.deliberation() != null) {
            brain.deliberation().flushSpeech();
        }
    }

    /**
     * Contact. Walking into an occupied tile is this game's melee trigger, so the chase is
     * just movement and the blow falls out of arriving. Only our own suspect: an officer
     * shouldering past a bystander mid-chase is not committing assault.
     */
    @Override
    public boolean onObstacle(NpcController npcController, Entity blocker) {
        String suspect = suspectUid();
        if (suspect == null || blocker.get_uid() == null || !blocker.get_uid().equals(suspect)) {
            return false;
        }
        if (brain.human().get_combat() == null) {
            return false;
        }
        LlmDebug.log("%s: PURSUE strikes suspect %s", brain.human().get_uid(), suspect);
        brain.human().get_combat().inflict_damage(blocker);
        return true;
    }

    @Override
    public String narrate() {
        if (suspectName == null) {
            return null;
        }
        return "You are chasing " + suspectName
                + ", a violent criminal, and closing in to arrest them. Anything you say"
                + " now is an order to stop, a warning, or a call for backup.";
    }

    private String suspectUid() {
        Percept percept = brain.knowledge().suspect();
        return percept == null ? null : percept.uid;
    }

    private Entity suspect() {
        return brain.entity(suspectUid());
    }

    /**
     * Relevant while a known criminal is close enough to catch. Grace period and ceiling
     * work exactly as they do for fleeing — a chase that has lost its target keeps going for
     * a few turns rather than stopping dead the instant a corner is turned, and a cop who
     * cannot close eventually goes back on the beat instead of chasing for the whole session.
     */
    public static class Trigger implements Impulse {

        private final TownAI brain;
        private long engagedAt = -1;

        public Trigger(TownAI brain) {
            this.brain = brain;
        }

        @Override
        public String name() {
            return "suspect";
        }

        @Override
        public String state() {
            return STATE;
        }

        @Override
        public boolean isRelevant() {
            Percept percept = brain.knowledge().suspect();
            Entity suspect = percept == null ? null : brain.entity(percept.uid);
            if (suspect == null || suspect.origin == null || !TownAI.isAlive(suspect)) {
                return giveUp("suspect down or gone");
            }
            // Hearsay is not grounds for a chase. A report names a suspect, but chasing on
            // one means an officer across town running unerringly at a player he cannot
            // possibly have seen; he goes and looks instead, and this engages when he does.
            if (percept.source != Percept.Source.SEEN) {
                return giveUp("only a report, not a sighting");
            }
            long now = GameTurn.current();
            int distance = brain.ctrl().distanceToTarget(suspect.origin);
            if (distance > Tuning.priority().pursueDistance
                    && now - percept.crimeTurn > Tuning.priority().pursueTurns) {
                return giveUp("lost them, distance " + distance);
            }
            if (engagedAt >= 0 && now - engagedAt >= Tuning.priority().pursueMaxTurns) {
                return giveUp("chase ran too long");
            }
            if (engagedAt < 0) {
                engagedAt = now;
            }
            return true;
        }

        private boolean giveUp(String why) {
            if (engagedAt >= 0) {
                LlmDebug.log("%s: PURSUE released (%s)", brain.human().get_uid(), why);
                engagedAt = -1;
            }
            return false;
        }
    }
}
