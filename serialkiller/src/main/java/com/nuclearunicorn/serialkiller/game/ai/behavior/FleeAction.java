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
import com.nuclearunicorn.serialkiller.game.world.RLWorldChunk;
import com.nuclearunicorn.serialkiller.game.world.entities.EntityDoor;
import com.nuclearunicorn.serialkiller.game.world.entities.EntityFurniture;
import org.lwjgl.util.Point;

import java.util.List;

/**
 * Get away from whoever just hurt us.
 *
 * <p>The trigger lives next to it as {@link Trigger} because the two are halves of one
 * behaviour: the trigger decides how long panic lasts, the action decides where the feet go,
 * and changing one almost always means changing the other. What they must <i>not</i> share
 * is a class with everything else the NPC can do, which is where all of this used to live.
 */
public class FleeAction implements IAIAction, Narrating {

    public static final String STATE = "ai_state_FLEEING";

    private final TownAI brain;
    private String fleeingFromName;

    public FleeAction(TownAI brain) {
        this.brain = brain;
    }

    @Override
    public void onEnter() {
        // Costs no inference and happens on the same turn as the blow. The model still gets
        // to speak afterwards; it just no longer gates survival.
        brain.ctrl().clearPath();   // wherever we were going is moot
        if (brain.deliberation() != null) {
            brain.deliberation().interrupt();
        }
        brain.voice().scream();
        LlmDebug.log("%s: FLEE engaged (threat=%s)", brain.human().get_uid(), threatUid());
    }

    @Override
    public void act(NpcController npcController) {
        Entity threat = threat();
        RLController ctrl = brain.ctrl();
        if (threat == null) {
            return;
        }
        fleeingFromName = displayName(threat);

        // Route the escape rather than stepping greedily away. escapeTarget() walks straight
        // at whatever is behind you, which indoors is a wall — the victim got two tiles from
        // the attacker, jammed in a corner, and stood there being stabbed.
        if (!ctrl.hasPath()) {
            routeAwayFrom(ctrl, threat);
        }
        if (ctrl.hasPath()) {
            ctrl.follow_path();
        } else {
            ctrl.escapeTarget(threat);   // open ground, or nowhere left to route to
        }
        if (brain.deliberation() != null) {
            brain.deliberation().flushSpeech();   // running for your life does not make you mute
        }
    }

    /**
     * Cornered. The pathfinder will not route anyone through a window, so a panicking NPC only
     * ever walks into one when {@link NpcController#escapeTarget} has backed them into a dead
     * end — and someone with a knife behind them goes through the glass rather than turn round.
     * The commute does not get this; see {@link TownAI#e_on_obstacle}.
     */
    @Override
    public boolean onObstacle(NpcController npcController, Entity blocker) {
        if (!breaksThrough(blocker) || brain.human().get_combat() == null) {
            return false;
        }
        LlmDebug.log("%s: FLEE breaks through %s", brain.human().get_uid(), blocker.getName());
        brain.human().get_combat().inflict_damage(blocker);
        return true;
    }

    /**
     * Is this something a panicking person goes <i>through</i>? Furniture and glass, yes.
     * A door opens, so there is nothing to break; a bystander is a person to be shoved past
     * rather than attacked, and mistaking one for the other turns every crowded escape into
     * a massacre.
     */
    static boolean breaksThrough(Entity blocker) {
        return blocker instanceof EntityFurniture && !(blocker instanceof EntityDoor);
    }

    @Override
    public String narrate() {
        if (fleeingFromName == null) {
            return null;
        }
        return "You are running away from " + fleeingFromName
                + ", who attacked you. You are terrified. Anything you say to them now is"
                + " a plea, an accusation or a cry for help - never small talk.";
    }

    private String threatUid() {
        Percept percept = brain.knowledge().threat();
        return percept == null ? null : percept.uid;
    }

    private Entity threat() {
        return brain.entity(threatUid());
    }

    private String displayName(Entity ent) {
        return ent.isPlayerEnt() ? "the player" : ent.getName();
    }

    /**
     * Head for the nav-mesh milestone furthest from the threat. Milestones are routable by
     * construction and lead out through doors, so panic takes an NPC out of the building
     * instead of into the nearest corner.
     */
    private void routeAwayFrom(RLController ctrl, Entity threat) {
        if (!(brain.human().get_chunk() instanceof RLWorldChunk)) {
            return;
        }
        List<Point> milestones = ((RLWorldChunk) brain.human().get_chunk()).getMilestones();
        if (milestones == null || milestones.isEmpty()) {
            return;
        }
        // Furthest from the threat *among nearby milestones*. Scoring distance globally picks
        // the far corner of the map, and A* refuses a route that long — panic is a short
        // sprint to the next junction, not an expedition.
        long reach = (long) SEARCH_RADIUS * SEARCH_RADIUS;
        Point best = null;
        long bestScore = Long.MIN_VALUE;
        Point nearest = null;
        long nearestDist = Long.MAX_VALUE;
        for (Point milestone : milestones) {
            long fromUs = distanceSq(milestone, brain.human().origin);
            if (fromUs < nearestDist) {
                nearestDist = fromUs;
                nearest = milestone;
            }
            if (fromUs > reach) {
                continue;
            }
            long fromThreat = distanceSq(milestone, threat.origin);
            if (fromThreat > bestScore) {
                bestScore = fromThreat;
                best = milestone;
            }
        }
        if (best == null) {
            best = nearest;   // nothing in reach; head for the nav mesh at all
        }
        if (best != null) {
            ctrl.set_destination(new Point(best));
        }
    }

    /** How far to look for an escape milestone. Bounded so A* is asked a routable question. */
    private static final int SEARCH_RADIUS = 20;

    private static long distanceSq(Point a, Point b) {
        long dx = a.getX() - b.getX();
        long dy = a.getY() - b.getY();
        return dx * dx + dy * dy;
    }

    /**
     * Relevant while someone who hurt us is still close enough to be worth running from.
     *
     * <p>The clock is a <i>grace period</i>, not a budget. As a flat budget it made panic a
     * stopwatch: the victim ran ten turns, the player simply walked after her at the same
     * speed, and on turn eleven she stopped and went back to her errands with him standing
     * one tile away. You stop running when you are safe, not when a timer says so — with a
     * hard ceiling only so that someone cornered in a room eventually gets her plan back.
     */
    public static class Trigger implements Impulse {

        private final TownAI brain;
        private long engagedAt = -1;

        public Trigger(TownAI brain) {
            this.brain = brain;
        }

        @Override
        public String name() {
            return "threat";
        }

        @Override
        public String state() {
            return STATE;
        }

        @Override
        public boolean isRelevant() {
            Percept percept = brain.knowledge().threat();
            Entity threat = percept == null ? null : brain.entity(percept.uid);
            if (threat == null || threat.origin == null || !TownAI.isAlive(threat)) {
                return release("threat gone");
            }
            long now = GameTurn.current();
            if (now - percept.attackedUsTurn > Tuning.priority().fleeTurns
                    && !inRange(threat)) {
                return release("clear");
            }
            if (engagedAt >= 0 && now - engagedAt >= Tuning.priority().fleeMaxTurns) {
                return release("exhausted after " + (now - engagedAt) + " turns");
            }
            if (engagedAt < 0) {
                engagedAt = now;
            }
            return true;
        }

        private boolean inRange(Entity threat) {
            int distance = brain.ctrl().distanceToTarget(threat.origin);
            return distance > 0 && distance <= Tuning.priority().fleeDistance;
        }

        private boolean release(String why) {
            if (engagedAt >= 0) {
                LlmDebug.log("%s: FLEE released (%s)", brain.human().get_uid(), why);
                engagedAt = -1;
            }
            return false;
        }
    }
}
