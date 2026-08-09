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

/** Go home and get into bed. Runs from nightfall until something more pressing happens. */
public class SleepAction implements IAIAction {

    public static final String STATE = "ai_state_SLEEPING";

    private final TownAI brain;

    public SleepAction(TownAI brain) {
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
        if (inBed()) {
            npcController.clearPath();
            return;
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

    private void routeToBed(NpcController npcController, Apartment apt) {
        if (apt.beds == null || apt.beds.isEmpty()) {
            return;
        }
        for (Entity bed : apt.beds) {
            if (!bed.tile.has_ent(EntityRLHuman.class)) {
                ((RLController) npcController).calculateAdaptivePath(brain.human().origin, bed.origin);
                return;
            }
        }
    }

    private boolean inBed() {
        return brain.human().getApartment() != null && brain.human().tile.has_ent(EntityBed.class);
    }

    /** Night, and somewhere to sleep. Police decline this one — see PoliceAI. */
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
            return WorldTimer.is_night() && brain.human().getApartment() != null;
        }
    }
}
