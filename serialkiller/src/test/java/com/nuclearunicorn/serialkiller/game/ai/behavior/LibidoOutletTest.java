package com.nuclearunicorn.serialkiller.game.ai.behavior;

import com.nuclearunicorn.libroguelike.game.ai.Impulse;
import com.nuclearunicorn.serialkiller.game.ai.Libido;
import com.nuclearunicorn.serialkiller.game.ai.PedestrianAI;
import com.nuclearunicorn.serialkiller.game.controllers.RLController;
import com.nuclearunicorn.serialkiller.game.world.RLWorldModel;
import com.nuclearunicorn.serialkiller.game.world.entities.EntityRLHuman;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.Point;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * The libido drive's escalation rule: the lawful outlet is tried first, and violence is what
 * is left when there is none.
 *
 * <p>Which makes the agreement between {@code SexAction}'s trigger and its action the load
 * bearing part. They used to ask different questions — the trigger asked whether the town
 * <i>has</i> a brothel, the action asked whether anyone is <i>in</i> it — so a town whose
 * working girls had been murdered answered yes to the first and null to the second. Every
 * adult in it then stood still in a satisfiable-looking state while libido climbed the last
 * thirty points to the ceiling, and the whole town escalated to {@code RapeAction} at once.
 */
class LibidoOutletTest {

    @AfterEach
    void clearTown() {
        RLWorldModel.brothelLocation = null;
    }

    @Test
    void aBrothelWithNobodyInItIsNotAnOutlet() {
        RLWorldModel.brothelLocation = new Point(40, 40);   //the building is there...
        assertFalse(sexTrigger(needyAdult()).isRelevant(),
                "...but there is nobody in it: seeking sex would stand still until frenzy");
    }

    @Test
    void anAdultWithNoMateAndNoBrothelDoesNotSeekSex() {
        assertFalse(sexTrigger(needyAdult()).isRelevant());
    }

    /** Below the threshold nothing fires at all, however available a partner is. */
    @Test
    void anUnbotheredAdultIsNotLookingForAnybody() {
        RLWorldModel.brothelLocation = new Point(40, 40);
        EntityRLHuman npc = needyAdult();
        npc.getBodysim().setAttribute("libido", Libido.NEEDY - 1);
        assertFalse(sexTrigger(npc).isRelevant());
    }

    // ------------------------------------------------------------------- helpers

    private static EntityRLHuman needyAdult() {
        EntityRLHuman npc = new EntityRLHuman();
        npc.setName("MABEL FARR");
        npc.setSex(EntityRLHuman.Sex.FEMALE);
        npc.age = 30;
        npc.origin = new Point(1, 1);
        npc.set_controller(new RLController());
        npc.getBodysim().setAttribute("libido", Libido.NEEDY + 5);
        return npc;
    }

    /** The "sex" impulse off a real brain, so the test cannot drift from the registration. */
    private static Impulse sexTrigger(EntityRLHuman npc) {
        PedestrianAI brain = new PedestrianAI();
        brain.set_owner(npc);
        npc.set_ai(brain);
        return new SexAction.Trigger(brain);
    }
}
