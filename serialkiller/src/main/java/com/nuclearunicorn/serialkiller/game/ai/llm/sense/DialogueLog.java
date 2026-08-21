package com.nuclearunicorn.serialkiller.game.ai.llm.sense;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Recent dialogue in the order it happened — what this NPC heard, and what it said (§8).
 *
 * <p>{@link StimulusMemory} ranks by salience, which is right for deciding what to react to
 * and wrong for holding a conversation: a ranked bag has no notion of who spoke last. Worse,
 * an NPC's own speech was recorded nowhere at all ({@code HearingSensor} skips the speaker,
 * and the prompt never said what the NPC had just said), so every re-plan opened cold on the
 * same scene. Observed: an NPC answered the player, then two turns later greeted them again
 * with <i>"sorry, I didn't hear you come in"</i> — mid-conversation, to the same person.
 *
 * <p>Attribution also does work the ranked list could not. Overheard lines used to reach the
 * model as <i>"- you overheard X say: Y"</i> among other bullets, and a 3.8B model copies the
 * nearest quoted string: one NPC repeated, as her own line, a sentence another NPC had said
 * <i>about her</i>. A transcript with a speaker against every line is much harder to mistake
 * for something to repeat.
 */
public class DialogueLog implements Serializable {

    /** How an own turn is stored and marked. Rendered as "<name> (you)" — see {@link #render}. */
    private static final String SELF = "you";

    private static final class Line implements Serializable {
        final long turn;
        final String speaker;
        final String text;
        final boolean overheard;

        Line(long turn, String speaker, String text, boolean overheard) {
            this.turn = turn;
            this.speaker = speaker;
            this.text = text;
            this.overheard = overheard;
        }
    }

    private final List<Line> lines = new ArrayList<>();
    private final int capacity;

    public DialogueLog(int capacity) {
        this.capacity = Math.max(1, capacity);
    }

    /** A line this NPC heard. {@code directed} separates being spoken to from overhearing. */
    public void heard(long turn, String speaker, String text, boolean directed) {
        add(new Line(turn, speaker, text, !directed));
    }

    /** A line this NPC spoke. */
    public void said(long turn, String text) {
        add(new Line(turn, SELF, text, false));
    }

    private void add(Line line) {
        lines.add(line);
        while (lines.size() > capacity) {
            lines.remove(0);
        }
    }

    public boolean isEmpty() {
        return lines.isEmpty();
    }

    /** Lines held, for the replay log. */
    public int size() {
        return lines.size();
    }

    /**
     * Have we already said this? Last-resort guard against literal repetition — the prompt
     * asks the model not to repeat itself, and this is what happens when it does anyway.
     */
    public boolean alreadySaid(String text) {
        String key = normalize(text);
        if (key.isEmpty()) {
            return false;
        }
        for (Line line : lines) {
            if (SELF.equals(line.speaker) && normalize(line.text).equals(key)) {
                return true;
            }
        }
        return false;
    }

    /** Punctuation and case are not a difference worth speaking twice for. */
    private static String normalize(String text) {
        return text == null ? "" : text.toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    /**
     * The transcript block, oldest first, one attributed line each.
     *
     * <p>{@code selfName} is the speaker's own name, and leaving it out was a real bug. Own
     * turns were labelled with a bare "you", so the NPC's name appeared in its own transcript
     * only inside somebody else's quoted vocative — and a small model reaching for a name to
     * address someone with grabs the nearest one it can see. Observed: DANIAL MICHAEL, told
     * <i>"Sure, Danial. Come on in."</i>, replied <i>"What's new with you, Danial?"</i>.
     * Labelling the line "DANIAL MICHAEL (you)" binds the name to the speaker instead.
     */
    public String render(String selfName) {
        String self = selfName == null || selfName.isEmpty() ? SELF : selfName + " (" + SELF + ")";
        StringBuilder sb = new StringBuilder(256);
        sb.append("Conversation so far, oldest first:\n");
        for (Line line : lines) {
            sb.append("  ").append(SELF.equals(line.speaker) ? self : line.speaker);
            if (line.overheard) {
                sb.append(" (overheard, not said to you)");
            }
            sb.append(": \"").append(line.text).append("\"\n");
        }
        return sb.toString();
    }
}
