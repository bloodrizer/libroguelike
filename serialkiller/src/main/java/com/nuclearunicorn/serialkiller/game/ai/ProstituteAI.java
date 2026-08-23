package com.nuclearunicorn.serialkiller.game.ai;

import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.serialkiller.game.ai.behavior.StayAtBrothelAction;
import com.nuclearunicorn.serialkiller.game.ai.llm.Perception;
import com.nuclearunicorn.serialkiller.game.world.entities.EntityRLHuman;

/**
 * A sex worker. The same person as a {@link PedestrianAI} with four differences, each of them
 * a line in the constructor — the standard "one brain per kind of person" shape from
 * {@code LLM_NPC_SPEC.md} §8.6, instead of a {@code role} flag branching at call sites.
 *
 * <ol>
 *   <li>She does not seek sex — she <i>provides</i> it, so both libido impulses are removed.</li>
 *   <li>She does not roam the town — {@code patrol} is removed.</li>
 *   <li>She stays at the brothel: a {@code work} impulse (the brothel is also her home).</li>
 *   <li>Her persona says all of that to the model, in the same file as the behaviour.</li>
 * </ol>
 *
 * <p>{@link #is} is how a customer recognises one without a role flag: a prostitute is a
 * person whose brain is a {@code ProstituteAI}.
 */
public class ProstituteAI extends PedestrianAI {

    /** Above nothing but patrol(10), below the plan(40) and night(30): a catch-all. */
    private static final int PRIORITY_WORK = 20;

    public ProstituteAI() {
        super();

        removeImpulse("sex");
        removeImpulse("rape");
        removeImpulse("patrol");

        registerState(StayAtBrothelAction.STATE, new StayAtBrothelAction(this));
        registerImpulse(PRIORITY_WORK, new StayAtBrothelAction.Trigger(this));
    }

    /** Is this entity a prostitute? The customer's answer to "who here takes clients". */
    public static boolean is(Entity ent) {
        return ent != null && ent.getAI() instanceof ProstituteAI;
    }

    @Override
    public void describeSelf(StringBuilder sb, EntityRLHuman self) {
        Perception.appendIdentity(sb, self);
        sb.append(" and a prostitute who works at the town brothel.\n")
          .append("You take clients into a private room. You are not looking for romance or")
          .append(" conversation, and you do not roam the streets looking for customers.\n");
    }
}
