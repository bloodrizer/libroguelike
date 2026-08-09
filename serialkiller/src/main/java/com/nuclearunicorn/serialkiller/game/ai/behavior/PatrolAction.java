package com.nuclearunicorn.serialkiller.game.ai.behavior;

import com.nuclearunicorn.libroguelike.game.ai.IAIAction;
import com.nuclearunicorn.libroguelike.game.ent.controller.NpcController;
import com.nuclearunicorn.libroguelike.utils.Rng;
import com.nuclearunicorn.serialkiller.game.ai.TownAI;
import com.nuclearunicorn.serialkiller.game.world.RLWorldChunk;
import org.lwjgl.util.Point;

import java.util.List;

/** Wander the nav-mesh milestones. What an NPC does when nothing else is going on. */
public class PatrolAction implements IAIAction {

    public static final String STATE = "ai_state_PATROLLING";

    private final TownAI brain;

    public PatrolAction(TownAI brain) {
        this.brain = brain;
    }

    @Override
    public void act(NpcController npcController) {
        if (npcController == null) {
            return;
        }
        if (!npcController.hasPath()) {
            routeToRandomMilestone(npcController);
        }
        //add 10% chance of standing still, making it more natural for player to chase target
        if ((int) (Rng.random() * 100) >= 15) {
            npcController.follow_path();
        }
    }

    /** Always relevant, so it sits at the bottom of the list and catches everything. */
    public static class Trigger implements com.nuclearunicorn.libroguelike.game.ai.Impulse {

        @Override
        public String name() {
            return "patrol";
        }

        @Override
        public String state() {
            return STATE;
        }

        @Override
        public boolean isRelevant() {
            return true;
        }
    }

    private void routeToRandomMilestone(NpcController npcController) {
        if (!(brain.human().get_chunk() instanceof RLWorldChunk)) {
            return;
        }
        List<Point> milestones = ((RLWorldChunk) brain.human().get_chunk()).getMilestones();
        if (milestones == null || milestones.isEmpty()) {
            return;
        }
        Point milestone = milestones.get((int) (Rng.random() * milestones.size()));

        //get random point near the milestone node (more natural ai behavior)
        npcController.set_destination(new Point(
                milestone.getX() + (int) Rng.random() * 4 - 2,
                milestone.getY() + (int) Rng.random() * 4 - 2));
    }
}
