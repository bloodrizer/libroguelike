package com.nuclearunicorn.serialkiller.game.ai.mind;

/**
 * A behaviour that can say what it is doing, in the second person, for the prompt.
 *
 * <p>The words have to match the legs. A victim whose body is sprinting away from her
 * attacker while the model — told nothing about it — composes "lovely evening, isn't it"
 * is the single most conspicuous failure this system has, and it happens whenever the
 * prompt is built from what an NPC <i>sensed</i> rather than from what it is <i>doing</i>.
 * Events decay; a chase does not, so the chase describes itself.
 */
public interface Narrating {

    /** One or two sentences of situation, or null if this behaviour needs no framing. */
    String narrate();
}
