package com.nuclearunicorn.libroguelike.game.ai;

/**
 * A free-floating trigger: a condition that, when true, names the state the AI should be in.
 * Borrowed wholesale from the Halo 2 AI (Isla, GDC 2005), where impulses are kept
 * <i>separate</i> from the behaviours they invoke, and selection is a prioritised list over
 * binary relevance rather than a pile of floats to tune.
 *
 * <p>The point of the separation is that the condition becomes a named, inspectable thing.
 * Before this, deciding what an NPC should do was an {@code if} chain inside {@code update()}
 * that both tested the world and assigned the state, so "when does a policeman give chase"
 * had no answer you could point at — it was three clauses spread across two classes and an
 * enum. An impulse is that answer, and a subclass changes an NPC's priorities by registering
 * a different set rather than by editing the chain.
 *
 * @see AI#registerImpulse(int, Impulse)
 */
public interface Impulse {

    /** Human-readable, for logs and debug dumps. */
    String name();

    /** State to enter while this holds. */
    String state();

    /** Binary relevance: is this worth doing right now? */
    boolean isRelevant();
}
