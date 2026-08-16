package com.nuclearunicorn.serialkiller.game.ai.llm.sense;

import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.serialkiller.game.world.entities.EntityRLHuman;

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

    private Relations() {}

    /** Plain display name — "the player" for the player entity. */
    public static String name(Entity subject) {
        if (subject == null) {
            return "someone";
        }
        return subject.isPlayerEnt() ? "the player" : subject.getName();
    }

    /**
     * {@code subject} as {@code observer} would refer to them: "your wife JUNE HALE",
     * "your father (the player)", or just the name when they are not related.
     */
    public static String describe(Entity observer, Entity subject) {
        String plain = name(subject);
        String relation = relation(observer, subject);
        if (relation == null) {
            return plain;
        }
        // The player has no surname to attach the relation to, so keep the tag separate.
        return subject != null && subject.isPlayerEnt()
                ? "your " + relation + " (the player)"
                : "your " + relation + " " + plain;
    }

    /** Bare relation word, or null when the two are unrelated / not both human. */
    public static String relation(Entity observer, Entity subject) {
        if (!(observer instanceof EntityRLHuman) || !(subject instanceof EntityRLHuman)) {
            return null;
        }
        return ((EntityRLHuman) observer).relationTo((EntityRLHuman) subject);
    }
}
