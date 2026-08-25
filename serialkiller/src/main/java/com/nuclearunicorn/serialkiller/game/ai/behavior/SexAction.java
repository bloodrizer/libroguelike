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
import com.nuclearunicorn.serialkiller.game.social.TownLog;
import com.nuclearunicorn.serialkiller.game.sound.PlayerEars;
import com.nuclearunicorn.serialkiller.game.sound.SoundEvent;
import com.nuclearunicorn.serialkiller.game.sound.SoundKind;
import com.nuclearunicorn.serialkiller.game.world.entities.EntityRLHuman;
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
        EntityRLHuman partner = partnerFor(brain);
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

    /**
     * Mate first, then the nearest live prostitute. Whoever is closest to hand.
     *
     * <p>Static and shared with {@link Trigger}, which is the whole point: the trigger used
     * to ask whether the town <i>has</i> a brothel while the action asked whether anyone is
     * actually in it. A town whose working girls have all been murdered therefore answered
     * yes to the first and null to the second, and every adult in it stood still with a
     * satisfiable-looking urge until libido hit the ceiling and {@link RapeAction} took over.
     */
    static EntityRLHuman partnerFor(TownAI brain) {
        EntityRLHuman mate = brain.human().getMate();
        if (mate != null && mate.origin != null && TownAI.isAlive(mate) && mate.isAdult()) {
            return mate;
        }
        return nearestProstitute(brain);
    }

    private static EntityRLHuman nearestProstitute(TownAI brain) {
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

    /** Pink enough to read at a glance over a head, and against the town's muted palette. */
    private static final Color HEART = new Color(255, 120, 180);

    /** Turns the heart stays up. Long enough to catch across a couple of key presses. */
    private static final int EMOTE_TURNS = 6;

    private void resolve(EntityRLHuman partner) {
        EntityRLHuman self = brain.human();
        BodySimulation body = self.getBodysim();
        body.setAttribute("libido", 0f);
        //both of them, or the other half walks over afterwards and reports it a second time
        if (partner.getBodysim() != null) {
            partner.getBodysim().setAttribute("libido", 0f);
        }

        self.emote("<3", HEART, EMOTE_TURNS);
        partner.emote("<3", HEART, EMOTE_TURNS);

        String line = Relations.name(self) + " had sex with " + Relations.name(partner);
        //the console is the player's record, not the town's: only what you saw or heard
        PlayerEars.report(self, SoundKind.MOAN.db(), line, "You hear moaning", Color.magenta);
        TownLog.record(TownLog.Kind.SEX, line, self.x(), self.y());
        if (self.origin != null) {
            new SoundEvent(self.origin, SoundKind.MOAN, self, self.getLayerId()).emit();
        }

        transmitStd(body, partner.getBodysim());
        transmitStd(partner.getBodysim(), body);
    }

    /**
     * An infected partner can pass it on, ~50% either way.
     *
     * <p>On the body-simulation stream, not the world-generation one {@code ActionRape} rolls
     * on: {@link Rng} splits the streams precisely so a runtime roll cannot reshuffle the
     * town, and a per-act {@code derive} on WORLDGEN advances it on every act.
     */
    private static void transmitStd(BodySimulation carrier, BodySimulation other) {
        if (carrier != null && other != null && carrier.isInfected() && !other.isInfected()) {
            if (Rng.nextInt(Rng.COMBAT, 100) <= 50) {
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
            return partnerFor(brain) != null;
        }
    }
}
