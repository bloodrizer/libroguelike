package com.nuclearunicorn.serialkiller.game.ai.mind;

import com.nuclearunicorn.serialkiller.game.world.entities.EntityRLHuman;

/**
 * How an NPC introduces itself to the model: name, age, and — the part that was missing —
 * what it is <i>for</i>.
 *
 * <p>Implemented by the brain, so the description of a policeman lives in {@code PoliceAI}
 * next to the behaviour that makes him one. It used to live in a {@code Role} enum consulted
 * by the prompt builder, which meant the two halves of "being a policeman" — the duty and
 * the chase — sat in different files and could disagree. They did: the officer was told he
 * was on duty by an enum branch while running a brain with no way to arrest anyone.
 */
public interface Persona {

    /** Second-person self-description, ending in a newline. Opens the prompt. */
    void describeSelf(StringBuilder sb, EntityRLHuman owner);
}
