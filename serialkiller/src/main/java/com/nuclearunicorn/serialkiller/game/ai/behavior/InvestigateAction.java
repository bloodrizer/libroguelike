package com.nuclearunicorn.serialkiller.game.ai.behavior;

import com.nuclearunicorn.libroguelike.game.ai.IAIAction;
import com.nuclearunicorn.libroguelike.game.ai.Impulse;
import com.nuclearunicorn.libroguelike.game.ent.controller.NpcController;
import com.nuclearunicorn.serialkiller.game.ai.TownAI;
import com.nuclearunicorn.serialkiller.game.ai.llm.LlmDebug;
import com.nuclearunicorn.serialkiller.game.ai.mind.Narrating;
import com.nuclearunicorn.serialkiller.game.controllers.RLController;
import com.nuclearunicorn.serialkiller.render.RLMessages;
import org.lwjgl.util.Point;
import org.newdawn.slick.Color;

/**
 * Walk to a reported crime scene and look around. The slow half of policing: it is what an
 * officer does about a crime he did <i>not</i> see, which — given a town and four
 * policemen — is nearly all of them.
 */
public class InvestigateAction implements IAIAction, Narrating {

    public static final String STATE = "ai_state_INVESTIGATING";

    /** Close enough to count as having looked. */
    private static final int ARRIVED = 1;

    private final TownAI brain;

    public InvestigateAction(TownAI brain) {
        this.brain = brain;
    }

    @Override
    public void onEnter() {
        brain.ctrl().clearPath();
        LlmDebug.log("%s: INVESTIGATE %s", brain.human().get_uid(), brain.knowledge().openCrimeScene());
    }

    @Override
    public void act(NpcController npcController) {
        Point scene = brain.knowledge().openCrimeScene();
        if (scene == null) {
            return;
        }
        RLController ctrl = (RLController) npcController;
        if (ctrl.distanceToTarget(scene) <= ARRIVED) {
            brain.knowledge().closeCrimeScene();
            RLMessages.message("Police has closed the case", Color.cyan);
            return;
        }
        if (!ctrl.hasPath()) {
            ctrl.calculateAdaptivePath(brain.human().origin, scene);
        }
        ctrl.follow_path();
    }

    @Override
    public String narrate() {
        return "You are on your way to the scene of a reported crime. Anything you say now is"
                + " a question to a witness, an instruction to a bystander, or a radio call.";
    }

    /** There is an unattended crime scene. Registered only by brains with a duty to attend. */
    public static class Trigger implements Impulse {

        private final TownAI brain;

        public Trigger(TownAI brain) {
            this.brain = brain;
        }

        @Override
        public String name() {
            return "crime scene";
        }

        @Override
        public String state() {
            return STATE;
        }

        @Override
        public boolean isRelevant() {
            return brain.knowledge().openCrimeScene() != null;
        }
    }
}
