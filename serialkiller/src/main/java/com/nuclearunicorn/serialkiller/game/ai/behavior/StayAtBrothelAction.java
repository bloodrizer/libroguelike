package com.nuclearunicorn.serialkiller.game.ai.behavior;

import com.nuclearunicorn.libroguelike.game.ai.IAIAction;
import com.nuclearunicorn.libroguelike.game.ai.Impulse;
import com.nuclearunicorn.libroguelike.game.ent.controller.NpcController;
import com.nuclearunicorn.serialkiller.game.ai.TownAI;
import com.nuclearunicorn.serialkiller.game.controllers.RLController;
import com.nuclearunicorn.serialkiller.game.world.RLWorldModel;
import com.nuclearunicorn.serialkiller.generators.Apartment;
import com.nuclearunicorn.serialkiller.generators.Block;
import org.lwjgl.util.Point;

/**
 * A prostitute's day job: stay at the brothel where a customer can find her.
 *
 * <p>The brothel is her {@code apartment}, so "night" already walks her to a private-room bed;
 * this is the daytime half — whenever nothing higher-priority has the body, walk back to the
 * brothel and stand around. It replaces {@code patrol} (a prostitute does not roam the
 * milestones), which is why it sits at the bottom of her impulse list as a catch-all.
 */
public class StayAtBrothelAction implements IAIAction {

    public static final String STATE = "ai_state_AT_BROTHEL";

    private final TownAI brain;

    public StayAtBrothelAction(TownAI brain) {
        this.brain = brain;
    }

    @Override
    public void act(NpcController npcController) {
        if (npcController == null) {
            return;
        }
        RLController ctrl = brain.ctrl();

        if (insideBrothel()) {
            // On shift: stand around, wait for a client. Not a place to wander away from.
            if (ctrl.hasPath()) {
                ctrl.clearPath();
            }
            return;
        }

        // Walked out (talked into it, or chased out) — come back.
        if (!ctrl.hasPath()) {
            Point brothel = RLWorldModel.brothelLocation;
            if (brothel != null) {
                ctrl.set_destination(new Point(brothel));
            }
        }
        if (ctrl.hasPath()) {
            ctrl.follow_path();
        }
    }

    /** Standing inside any room of the brothel building (which is also her home). */
    private boolean insideBrothel() {
        Apartment apt = brain.human().getApartment();
        Point origin = brain.human().origin;
        if (apt == null || apt.rooms == null || origin == null) {
            return false;
        }
        for (Block room : apt.rooms) {
            if (origin.getX() >= room.getX() && origin.getX() <= room.getX() + room.getW()
                    && origin.getY() >= room.getY() && origin.getY() <= room.getY() + room.getH()) {
                return true;
            }
        }
        return false;
    }

    /** Always relevant — the catch-all below plan/night, above nothing (patrol is removed). */
    public static class Trigger implements Impulse {

        private final TownAI brain;

        public Trigger(TownAI brain) {
            this.brain = brain;
        }

        @Override
        public String name() {
            return "work";
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
}
