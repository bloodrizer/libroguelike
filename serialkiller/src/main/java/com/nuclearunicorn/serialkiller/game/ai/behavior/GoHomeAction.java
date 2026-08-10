package com.nuclearunicorn.serialkiller.game.ai.behavior;

import com.nuclearunicorn.libroguelike.game.ai.IAIAction;
import com.nuclearunicorn.libroguelike.game.ai.Impulse;
import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.game.ent.controller.NpcController;
import com.nuclearunicorn.libroguelike.game.world.WorldTile;
import com.nuclearunicorn.libroguelike.game.world.WorldTimer;
import com.nuclearunicorn.libroguelike.utils.Rng;
import com.nuclearunicorn.serialkiller.game.ai.TownAI;
import com.nuclearunicorn.serialkiller.game.controllers.RLController;
import com.nuclearunicorn.serialkiller.game.world.entities.EntityBed;
import com.nuclearunicorn.serialkiller.game.world.entities.EntityRLHuman;
import com.nuclearunicorn.serialkiller.generators.Apartment;

/**
 * The walk home at dusk. Ends the moment the NPC is standing on a bed, where
 * {@link SleepAction} takes over.
 *
 * <p>The two used to be one state, and one state cannot honestly answer "is this person
 * asleep": everything that reads it — the glyph over the head, the info panel, the replay
 * brain dump — said SLEEPING about someone walking down the middle of the street.
 */
public class GoHomeAction implements IAIAction {

    public static final String STATE = "ai_state_GOING_HOME";

    private final TownAI brain;

    public GoHomeAction(TownAI brain) {
        this.brain = brain;
    }

    @Override
    public void act(NpcController npcController) {
        if (npcController == null) {
            return;
        }
        Apartment apt = brain.human().getApartment();
        if (apt == null) {
            if (npcController.hasPath()) {
                npcController.clearPath();
            }
            return;   //little optimization for homeless npc
        }

        //force NPC to switch old path and move to bed
        if (npcController.hasPath()) {
            WorldTile target = brain.human().getLayer().getTile(npcController.destination);
            boolean headingForFreeBed = target != null
                    && target.has_ent(EntityBed.class) && !target.has_ent(EntityRLHuman.class);
            if (!headingForFreeBed) {
                npcController.clearPath();   //otherwise, recalculate new path
            }
        }
        if (!npcController.hasPath()) {
            routeToBed(npcController, apt);
        }
        if ((int) (Rng.random() * 100) >= 15) {
            npcController.follow_path();
        }
    }

    /**
     * Head for a bed nobody is in. Every free bed gets a try, not just the first: one
     * unroutable bed used to leave the NPC with no path at all, standing wherever they
     * happened to be — which, in a hallway, plugs it for everyone walking home behind them.
     */
    private void routeToBed(NpcController npcController, Apartment apt) {
        if (apt.beds == null || apt.beds.isEmpty()) {
            return;
        }
        for (Entity bed : apt.beds) {
            if (bed.tile.has_ent(EntityRLHuman.class)) {
                continue;
            }
            ((RLController) npcController).calculateAdaptivePath(brain.human().origin, bed.origin);
            if (npcController.hasPath()) {
                return;
            }
        }
    }

    /** Night, somewhere to sleep, and not in it yet. Police decline this one — see PoliceAI. */
    public static class Trigger implements Impulse {

        private final TownAI brain;

        public Trigger(TownAI brain) {
            this.brain = brain;
        }

        @Override
        public String name() {
            return "night";
        }

        @Override
        public String state() {
            return STATE;
        }

        @Override
        public boolean isRelevant() {
            return WorldTimer.is_night() && brain.human().getApartment() != null
                    && !SleepAction.inBed(brain.human());
        }
    }
}
