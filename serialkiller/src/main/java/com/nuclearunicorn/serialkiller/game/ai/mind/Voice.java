package com.nuclearunicorn.serialkiller.game.ai.mind;

import com.nuclearunicorn.libroguelike.game.ent.EntityActor;
import com.nuclearunicorn.serialkiller.game.ai.llm.LlmDebug;
import com.nuclearunicorn.serialkiller.game.ai.llm.sense.DialogueLog;
import com.nuclearunicorn.serialkiller.game.ai.llm.sense.GameTurn;
import com.nuclearunicorn.serialkiller.render.RLMessages;
import org.newdawn.slick.Color;

import java.io.Serializable;

/**
 * Everything an NPC says, and the record of having said it. One funnel, so the two things a
 * speaker always gets wrong — running long, and repeating itself — are caught in one place,
 * and so that anything spoken lands in the transcript whoever produced it.
 *
 * <p>That second part is why this is a component rather than a method on the LLM brain. A
 * scream is not a sentence a model composed; it is a reflex, and it used to bypass the
 * speech path entirely. The NPC therefore had no memory of screaming, and asked about it
 * one turn later answered, quite honestly, that she had not heard anything.
 */
public class Voice implements Serializable {

    private static final String SCREAM = "AAAAAAAA!";
    private static final String CHALLENGE = "STOP! POLICE!";

    private final EntityActor owner;
    private final DialogueLog log;

    public Voice(EntityActor owner) {
        this.owner = owner;
        this.log = new DialogueLog(Tuning.memory().dialogueLines);
    }

    public DialogueLog log() {
        return log;
    }

    /** Speak a composed line. Clipped to fit a text bubble, dropped if we just said it. */
    public void say(String text) {
        String line = clip(text, Tuning.speech().maxSayChars);
        if (line.isEmpty()) {
            return;
        }
        if (log.alreadySaid(line)) {
            LlmDebug.log("  %s: suppressed repeat \"%s\"", owner.get_uid(), line);
            return;
        }
        LlmDebug.log("  %s says: \"%s\"", owner.get_uid(), line);
        owner.say_message(line);
        log.said(GameTurn.current(), line);
    }

    /** Terror. Announced in the message log because the player needs to hear it happen. */
    public void scream() {
        shout(SCREAM, owner.getName() + " screams!", Color.red);
    }

    /** The police equivalent: same rung, same immediacy, opposite intent. */
    public void challenge() {
        shout(CHALLENGE, owner.getName() + " gives chase!", Color.cyan);
    }

    /** A reflex line: always spoken, repeat guard and all, plus a line in the message log. */
    public void shout(String line, String message, Color color) {
        RLMessages.message(message, color);
        owner.say_message(line);
        log.said(GameTurn.current(), line);
    }

    public void heard(String speaker, String text, boolean directed) {
        log.heard(GameTurn.current(), speaker, text, directed);
    }

    public boolean alreadySaid(String text) {
        return log.alreadySaid(text);
    }

    /**
     * Cut a line back to {@code limit} characters, preferring a sentence boundary and then a
     * word boundary. NPCs speak in a floating bubble over their head, and a model asked for
     * "short sentences" still returns paragraphs — the instruction is advice, this is the rule.
     */
    public static String clip(String text, int limit) {
        if (text == null) {
            return "";
        }
        String line = text.trim();
        if (limit <= 0 || line.length() <= limit) {
            return line;
        }
        String head = line.substring(0, limit);
        int sentence = Math.max(head.lastIndexOf('.'), Math.max(head.lastIndexOf('!'), head.lastIndexOf('?')));
        if (sentence >= limit / 2) {
            return head.substring(0, sentence + 1).trim();
        }
        // Ellipsis has to fit inside the budget, not be added on top of it.
        String body = head.substring(0, Math.max(1, limit - 3));
        int word = body.lastIndexOf(' ');
        return (word >= limit / 2 ? body.substring(0, word) : body).trim() + "...";
    }
}
