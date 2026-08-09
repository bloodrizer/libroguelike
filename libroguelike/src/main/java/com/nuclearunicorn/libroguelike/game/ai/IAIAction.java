package com.nuclearunicorn.libroguelike.game.ai;


import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.game.ent.controller.NpcController;

/**
 * One thing an NPC can be <i>doing</i>: patrolling, fleeing, sleeping, chasing a suspect.
 * Registered against a state name in {@link AI#registerState} and run by {@link AI#think()}
 * for as long as that state is selected.
 *
 * <p>The lifecycle hooks are what make an action more than a function. Most behaviour that
 * matters is not "what do I do this tick" but "what do I do the moment this starts" — a
 * fleeing NPC screams once and drops whatever it was walking towards, and without an
 * {@code onEnter} that has to live in whoever changed the state, which is how the caller
 * ends up knowing everything about the callee. {@code onEnter} fires when the state is
 * selected, {@code onExit} when the AI moves to a different one.
 */
public interface IAIAction {

    void act(NpcController npcController);

    /** Selected. Fires once, before the first {@link #act}. */
    default void onEnter() {}

    /** Deselected. Fires once, after the last {@link #act}. */
    default void onExit() {}

    /**
     * The controller walked into something while this action had the body. Return true if
     * that counts as handled — a policeman striking his suspect does, a pedestrian bumping
     * a lamppost does not. Bump-into-it <i>is</i> the melee trigger in this game, so contact
     * belongs to whatever behaviour asked for the step.
     */
    default boolean onObstacle(NpcController npcController, Entity blocker) {
        return false;
    }
}
