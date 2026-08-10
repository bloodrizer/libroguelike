package com.nuclearunicorn.serialkiller.game.ai.behavior;

import com.nuclearunicorn.libroguelike.game.ai.IAIAction;
import com.nuclearunicorn.libroguelike.game.ai.Impulse;
import com.nuclearunicorn.libroguelike.game.ent.controller.NpcController;
import com.nuclearunicorn.libroguelike.game.world.WorldTimer;
import com.nuclearunicorn.serialkiller.game.ai.TownAI;
import com.nuclearunicorn.serialkiller.game.world.entities.EntityBed;
import com.nuclearunicorn.serialkiller.game.world.entities.EntityRLHuman;

/**
 * In bed, out for the night. Getting there is {@link GoHomeAction} — this state means the
 * NPC is actually on the bed, which is what makes it safe for the renderer and the info
 * panel to say so.
 */
public class SleepAction implements IAIAction {

    public static final String STATE = "ai_state_SLEEPING";

    private final TownAI brain;

    public SleepAction(TownAI brain) {
        this.brain = brain;
    }

    @Override
    public void act(NpcController npcController) {
        if (npcController != null && npcController.hasPath()) {
            npcController.clearPath();   //asleep: wherever we were going can wait for morning
        }
    }

    /** Standing on a bed at home. The one condition under which a Z is not a lie. */
    public static boolean inBed(EntityRLHuman human) {
        return human.getApartment() != null
                && human.tile != null && human.tile.has_ent(EntityBed.class);
    }

    /** Night, and already in bed. Police decline this one — see PoliceAI. */
    public static class Trigger implements Impulse {

        private final TownAI brain;

        public Trigger(TownAI brain) {
            this.brain = brain;
        }

        @Override
        public String name() {
            return "asleep";
        }

        @Override
        public String state() {
            return STATE;
        }

        @Override
        public boolean isRelevant() {
            return WorldTimer.is_night() && inBed(brain.human());
        }
    }
}
