package com.nuclearunicorn.libroguelike.game.player;

import com.nuclearunicorn.libroguelike.events.network.EChatMessage;
import com.nuclearunicorn.libroguelike.game.ent.Entity;

/**
 * What the player actually perceives of a line somebody said.
 *
 * <p>The engine has no acoustics and no field of view, so it cannot answer this itself: it
 * asks whoever the game installed. Unfilled it perceives everything, which is the old
 * behaviour — a bubble over every speaker in the world, wall or no wall, lit or in the black
 * void beyond the player's FOV, and a chat log line for a conversation four streets away.
 *
 * <p>Two questions rather than one because they are two senses. Seeing someone's lips move
 * and making out the words are independent, and the interesting cases are the ones where
 * they disagree. See {@code serialkiller}'s {@code PlayerEars} for the answers.
 */
public final class PlayerSpeech {

    public interface Filter {
        /** Text to float over the speaker's head, or null to draw nothing there. */
        String bubble(EChatMessage chat, Entity speaker);

        /** Whether the player made out the words at all. */
        boolean audible(EChatMessage chat, Entity speaker);
    }

    private static Filter filter;

    /** Installed per world, next to the sensors. Passing null restores "hears everything". */
    public static void setFilter(Filter filter) {
        PlayerSpeech.filter = filter;
    }

    public static String bubble(EChatMessage chat, Entity speaker) {
        return filter == null ? chat.message : filter.bubble(chat, speaker);
    }

    public static boolean audible(EChatMessage chat, Entity speaker) {
        return filter == null || filter.audible(chat, speaker);
    }

    private PlayerSpeech() {}
}
