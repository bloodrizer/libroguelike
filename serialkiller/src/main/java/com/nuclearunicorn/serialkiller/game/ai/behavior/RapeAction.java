package com.nuclearunicorn.serialkiller.game.ai.behavior;

import com.nuclearunicorn.libroguelike.game.ai.IAIAction;
import com.nuclearunicorn.libroguelike.game.ai.Impulse;
import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.game.ent.controller.NpcController;
import com.nuclearunicorn.libroguelike.utils.Rng;
import com.nuclearunicorn.serialkiller.game.ai.Libido;
import com.nuclearunicorn.serialkiller.game.ai.TownAI;
import com.nuclearunicorn.serialkiller.game.ai.llm.sense.Relations;
import com.nuclearunicorn.serialkiller.game.ai.mind.Narrating;
import com.nuclearunicorn.serialkiller.game.bodysim.BodySimulation;
import com.nuclearunicorn.serialkiller.game.controllers.RLController;
import com.nuclearunicorn.serialkiller.game.world.entities.EntityRLHuman;
import com.nuclearunicorn.serialkiller.render.RLMessages;
import org.lwjgl.util.Point;
import org.newdawn.slick.Color;

/**
 * The unlawful half of the libido drive, and the mirror of the player's bloodlust frenzy
 * ({@code PlayerAI}: bloodlust past its ceiling flips the player out of control).
 *
 * <p>An adult whose libido maxes out <i>with no outlet</i> — no mate, no brothel — loses
 * control and forces themself on the nearest person. Same rung as the flee reflex, so being
 * attacked still wins (the victim fighting back makes the attacker run), but nothing short of
 * that interrupts it. Resolving resets libido, so the frenzy ends on the next tick.
 */
public class RapeAction implements IAIAction, Narrating {

    public static final String STATE = "ai_state_RAPING";

    private static final int CONTACT_RANGE = 1;

    private final TownAI brain;
    private String victimName;

    public RapeAction(TownAI brain) {
        this.brain = brain;
    }

    @Override
    public void onEnter() {
        brain.ctrl().clearPath();
        if (brain.deliberation() != null) {
            brain.deliberation().interrupt();
        }
    }

    @Override
    public void act(NpcController npcController) {
        if (npcController == null) {
            return;
        }
        EntityRLHuman victim = pickVictim();
        if (victim == null || victim.origin == null) {
            if (npcController.hasPath()) {
                npcController.clearPath();
            }
            return;
        }
        victimName = Relations.name(victim);

        RLController ctrl = brain.ctrl();
        int distance = ctrl.distanceToTarget(victim.origin);
        if (distance <= CONTACT_RANGE) {
            resolve(victim);
            return;
        }
        if (!ctrl.hasPath()) {
            ctrl.set_destination(new Point(victim.origin));
        }
        if (ctrl.hasPath()) {
            ctrl.follow_path();
        } else {
            ctrl.chaseTarget(victim);
        }
    }

    /** The nearest adult townsperson, as the bloodlust frenzy picks the nearest NPC. */
    private EntityRLHuman pickVictim() {
        if (brain.human().getEnvironment() == null || brain.human().origin == null) {
            return null;
        }
        Entity[] ents = brain.human().getEnvironment().getEntityManager()
                .getEntities(brain.human().getLayerId());
        EntityRLHuman best = null;
        long bestDist = Long.MAX_VALUE;
        for (Entity ent : ents) {
            if (!(ent instanceof EntityRLHuman) || ent == brain.human() || ent.isPlayerEnt()) {
                continue;
            }
            EntityRLHuman candidate = (EntityRLHuman) ent;
            if (!TownAI.isAlive(candidate) || candidate.origin == null || !candidate.isAdult()) {
                continue;
            }
            long d = distanceSq(brain.human().origin, candidate.origin);
            if (d < bestDist) {
                bestDist = d;
                best = candidate;
            }
        }
        return best;
    }

    private void resolve(EntityRLHuman victim) {
        BodySimulation self = brain.human().getBodysim();
        self.setAttribute("libido", 0f);
        self.depleteBloodlust(10f);   // the same cost the player's ActionRape pays
        RLMessages.message(Relations.name(brain.human()) + " rapes " + Relations.name(victim),
                Color.orange);
        transmitStd(self, victim.getBodysim());
        transmitStd(victim.getBodysim(), self);
    }

    private static void transmitStd(BodySimulation carrier, BodySimulation other) {
        if (carrier != null && other != null && carrier.isInfected() && !other.isInfected()) {
            if (Rng.derive(Rng.WORLDGEN).nextInt(100) <= 50) {
                other.setInfected(true);
            }
        }
    }

    @Override
    public String narrate() {
        return victimName == null ? null
                : "You have lost control and are forcing yourself on " + victimName + ".";
    }

    private static long distanceSq(Point a, Point b) {
        long dx = a.getX() - b.getX();
        long dy = a.getY() - b.getY();
        return dx * dx + dy * dy;
    }

    /**
     * Relevant only at the ceiling and only with someone to hurt. The libido test comes
     * first so the victim scan never runs for the whole town on an ordinary tick.
     */
    public static class Trigger implements Impulse {

        private final TownAI brain;

        public Trigger(TownAI brain) {
            this.brain = brain;
        }

        @Override
        public String name() {
            return "rape";
        }

        @Override
        public String state() {
            return STATE;
        }

        @Override
        public boolean isRelevant() {
            if (brain.isAsleep() || !brain.human().isAdult()) {
                return false;
            }
            if (brain.human().getBodysim().getAttribute("libido") < Libido.FRENZY) {
                return false;
            }
            return victimExists();
        }

        private boolean victimExists() {
            if (brain.human().getEnvironment() == null) {
                return false;
            }
            Entity[] ents = brain.human().getEnvironment().getEntityManager()
                    .getEntities(brain.human().getLayerId());
            for (Entity ent : ents) {
                if (ent instanceof EntityRLHuman && ent != brain.human() && !ent.isPlayerEnt()
                        && ((EntityRLHuman) ent).isAdult() && TownAI.isAlive(ent)) {
                    return true;
                }
            }
            return false;
        }
    }
}
