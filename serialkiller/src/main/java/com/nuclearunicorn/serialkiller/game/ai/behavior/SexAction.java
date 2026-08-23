package com.nuclearunicorn.serialkiller.game.ai.behavior;

import com.nuclearunicorn.libroguelike.game.ai.IAIAction;
import com.nuclearunicorn.libroguelike.game.ai.Impulse;
import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.game.ent.controller.NpcController;
import com.nuclearunicorn.libroguelike.utils.Rng;
import com.nuclearunicorn.serialkiller.game.ai.Libido;
import com.nuclearunicorn.serialkiller.game.ai.ProstituteAI;
import com.nuclearunicorn.serialkiller.game.ai.TownAI;
import com.nuclearunicorn.serialkiller.game.ai.llm.sense.Relations;
import com.nuclearunicorn.serialkiller.game.ai.mind.Narrating;
import com.nuclearunicorn.serialkiller.game.bodysim.BodySimulation;
import com.nuclearunicorn.serialkiller.game.controllers.RLController;
import com.nuclearunicorn.serialkiller.game.world.RLWorldModel;
import com.nuclearunicorn.serialkiller.game.world.entities.EntityRLHuman;
import com.nuclearunicorn.serialkiller.render.RLMessages;
import org.lwjgl.util.Point;
import org.newdawn.slick.Color;

/**
 * The lawful half of the libido drive: find a partner and go to bed with them.
 *
 * <p>The partner is a mate (family, at home) first and a prostitute (at the brothel) second.
 * This is the town's <i>outlet</i> — an adult whose need is satisfied here never reaches the
 * frenzy of {@link RapeAction}. Purely mechanical: route, meet, reset libido, log the line.
 * Works with inference off, exactly like every other reflex.
 */
public class SexAction implements IAIAction, Narrating {

    public static final String STATE = "ai_state_SEEKING_SEX";

    private final TownAI brain;
    private String partnerName;

    public SexAction(TownAI brain) {
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
        EntityRLHuman partner = pickPartner();
        if (partner == null || partner.origin == null) {
            if (npcController.hasPath()) {
                npcController.clearPath();
            }
            return;
        }
        partnerName = Relations.name(partner);

        RLController ctrl = brain.ctrl();
        int distance = ctrl.distanceToTarget(partner.origin);
        if (distance <= 1) {
            resolve(partner);
            return;
        }
        if (!ctrl.hasPath()) {
            ctrl.set_destination(new Point(partner.origin));
        }
        if (ctrl.hasPath()) {
            ctrl.follow_path();
        }
    }

    /** Mate first, then the nearest live prostitute. Whoever is closest to hand. */
    private EntityRLHuman pickPartner() {
        EntityRLHuman mate = brain.human().getMate();
        if (mate != null && mate.origin != null && TownAI.isAlive(mate) && mate.isAdult()) {
            return mate;
        }
        return nearestProstitute();
    }

    private EntityRLHuman nearestProstitute() {
        if (brain.human().getEnvironment() == null || brain.human().origin == null) {
            return null;
        }
        Entity[] ents = brain.human().getEnvironment().getEntityManager()
                .getEntities(brain.human().getLayerId());
        EntityRLHuman best = null;
        long bestDist = Long.MAX_VALUE;
        for (Entity ent : ents) {
            if (!(ent instanceof EntityRLHuman)) {
                continue;
            }
            EntityRLHuman candidate = (EntityRLHuman) ent;
            if (candidate == brain.human() || !ProstituteAI.is(candidate)) {
                continue;
            }
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

    private void resolve(EntityRLHuman partner) {
        BodySimulation self = brain.human().getBodysim();
        self.setAttribute("libido", 0f);
        RLMessages.message(Relations.name(brain.human()) + " had sex with " + Relations.name(partner),
                Color.magenta);
        transmitStd(self, partner.getBodysim());
        transmitStd(partner.getBodysim(), self);
    }

    /** An infected partner can pass it on, ~50% either way. Mirrors ActionRape's roll. */
    private static void transmitStd(BodySimulation carrier, BodySimulation other) {
        if (carrier != null && other != null && carrier.isInfected() && !other.isInfected()) {
            if (Rng.derive(Rng.WORLDGEN).nextInt(100) <= 50) {
                other.setInfected(true);
            }
        }
    }

    @Override
    public String narrate() {
        return partnerName == null ? null
                : "You are in the mood and are on your way to be with " + partnerName + ".";
    }

    private static long distanceSq(Point a, Point b) {
        long dx = a.getX() - b.getX();
        long dy = a.getY() - b.getY();
        return dx * dx + dy * dy;
    }

    /**
     * Relevant while an adult is in the mood, still below the frenzy threshold, and has
     * somewhere to take it. Asleep is not in the mood — the urge does not sit an NPC up in
     * bed (INVARIANTS D2), and a sleeping mate is not what this behaviour is about.
     */
    public static class Trigger implements Impulse {

        private final TownAI brain;

        public Trigger(TownAI brain) {
            this.brain = brain;
        }

        @Override
        public String name() {
            return "sex";
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
            float libido = brain.human().getBodysim().getAttribute("libido");
            if (libido < Libido.NEEDY || libido >= Libido.FRENZY) {
                return false;
            }
            return mateAvailable() || RLWorldModel.brothelLocation != null;
        }

        private boolean mateAvailable() {
            EntityRLHuman mate = brain.human().getMate();
            return mate != null && mate.origin != null && TownAI.isAlive(mate) && mate.isAdult();
        }
    }
}
