package com.nuclearunicorn.serialkiller.game.ai.behavior;

import com.nuclearunicorn.libroguelike.game.ai.IAIAction;
import com.nuclearunicorn.libroguelike.game.ai.Impulse;
import com.nuclearunicorn.libroguelike.game.ent.controller.NpcController;
import com.nuclearunicorn.serialkiller.game.ai.TownAI;

/**
 * Run whatever the model last decided on. The bottom rung: it gets the body only when no
 * reflex wants it, which is the whole reason the LLM is a component and not a brain.
 */
public class DeliberateAction implements IAIAction {

    public static final String STATE = TownAI.AI_STATE_DELIBERATE;

    private final TownAI brain;

    public DeliberateAction(TownAI brain) {
        this.brain = brain;
    }

    @Override
    public void act(NpcController npcController) {
        if (brain.deliberation() != null) {
            brain.deliberation().tick();
        }
    }

    /**
     * Relevant whenever there is a planner with something to run. When the plan runs dry the
     * NPC falls through to whatever is registered below — patrolling, usually — instead of
     * standing in the street waiting for a completion.
     */
    public static class Trigger implements Impulse {

        private final TownAI brain;

        public Trigger(TownAI brain) {
            this.brain = brain;
        }

        @Override
        public String name() {
            return "plan";
        }

        @Override
        public String state() {
            return STATE;
        }

        @Override
        public boolean isRelevant() {
            return brain.deliberation() != null && !brain.deliberation().isIdle();
        }
    }
}
