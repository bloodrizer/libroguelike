package com.nuclearunicorn.serialkiller.game.ai.llm.sense;

import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.serialkiller.game.world.entities.EntityRLHuman;

import java.util.ArrayList;
import java.util.List;

/**
 * How a sensor names somebody <i>to the NPC doing the sensing</i> (§8).
 *
 * <p>Sensors used to hand the model a bare name, which flattens the only social structure
 * the world actually has: being shouted at by a stranger and being shouted at by your wife
 * are the same event with different text. Family is generated (mate, children, siblings)
 * and then never surfaced anywhere a model could reason about it.
 *
 * <p>The relation is resolved from the observer's side, so the same speaker is "your
 * daughter" to one listener and just a name to the next.
 */
public final class Relations {

    /** Last resort for someone with no name at all, so a prompt never says "undefined". */
    private static final String ANONYMOUS = "someone";
    /** What the engine calls an entity nobody named. Not a name, and never a thing to say. */
    private static final String UNNAMED = "undefined";

    private Relations() {}

    /**
     * Plain display name, the player's included.
     *
     * <p>This used to return the literal string {@code "the player"}, and that single
     * substitution is why an NPC asked <i>"who am I?"</i> by her own husband answered that
     * she did not recognise him: his name was never in her prompt to begin with, so the
     * honest answer to the question was the one she gave. The player entity is named like
     * everybody else in this town — and a game-engine word for a person is not something a
     * 3.8B model can reason about anyway, it is a fourth wall in the middle of a sentence.
     */
    public static String name(Entity subject) {
        if (subject == null) {
            return ANONYMOUS;
        }
        String name = subject.getName();
        if (name == null || name.trim().isEmpty() || UNNAMED.equals(name)) {
            return ANONYMOUS;
        }
        return titleCase(name);
    }

    /**
     * "DANIAL MICHAEL" -> "Danial Michael". The town stores names in caps and the UI draws
     * them that way, but a prompt is prose, and a model handed a name in caps hands it back:
     * an NPC introduced himself as <i>"Hello Barney, it's DANIAL."</i> — shouting his own
     * name, in a text bubble, at a neighbour. Display is unaffected; this is the model's copy.
     */
    private static String titleCase(String name) {
        StringBuilder sb = new StringBuilder(name.length());
        boolean startOfWord = true;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            sb.append(startOfWord ? Character.toUpperCase(c) : Character.toLowerCase(c));
            // Apostrophes and hyphens keep the letter after them capital: O'Neil, Mary-Jane.
            startOfWord = !Character.isLetter(c);
        }
        return sb.toString();
    }

    /**
     * {@code subject} as {@code observer} would refer to them: "your wife JUNE HALE",
     * "your father BRET HALE", or just the name when they are not related.
     */
    public static String describe(Entity observer, Entity subject) {
        String plain = name(subject);
        String relation = relation(observer, subject);
        return relation == null ? plain : "your " + relation + " " + plain;
    }

    /**
     * Name first, tie in brackets: "JUNE HALE (your wife)". For lists of people, where
     * {@link #describe}'s relation-first phrasing stacks up into "your wife JUNE HALE, your
     * son BRET HALE" and buries the names the model actually needs to use.
     */
    public static String tagged(Entity observer, Entity subject) {
        String relation = relation(observer, subject);
        String plain = name(subject);
        return relation == null ? plain : plain + " (your " + relation + ")";
    }

    /** Bare relation word, or null when the two are unrelated / not both human. */
    public static String relation(Entity observer, Entity subject) {
        if (!(observer instanceof EntityRLHuman) || !(subject instanceof EntityRLHuman)) {
            return null;
        }
        return ((EntityRLHuman) observer).relationTo((EntityRLHuman) subject);
    }

    /**
     * Everyone {@code observer} is related to, closest tie first: mate, parent, children,
     * siblings. Who your family <i>are</i> is a standing fact about you; {@link #relation}
     * only answers what somebody already in front of you is, which is no help at all when
     * the question is about somebody who is not.
     */
    public static List<EntityRLHuman> family(EntityRLHuman observer) {
        List<EntityRLHuman> kin = new ArrayList<EntityRLHuman>();
        if (observer == null) {
            return kin;
        }
        add(kin, observer.getMate());
        if (observer.getParent() instanceof EntityRLHuman) {
            add(kin, (EntityRLHuman) observer.getParent());
        }
        for (EntityRLHuman child : observer.getChildren()) {
            add(kin, child);
        }
        for (EntityRLHuman sibling : observer.getSiblings()) {
            add(kin, sibling);
        }
        return kin;
    }

    private static void add(List<EntityRLHuman> kin, EntityRLHuman member) {
        if (member != null && !kin.contains(member)) {
            kin.add(member);
        }
    }
}
