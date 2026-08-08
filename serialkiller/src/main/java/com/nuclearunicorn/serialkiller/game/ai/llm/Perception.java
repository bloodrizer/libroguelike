package com.nuclearunicorn.serialkiller.game.ai.llm;

import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.game.player.Player;
import com.nuclearunicorn.libroguelike.game.world.WorldTimer;
import com.nuclearunicorn.libroguelike.utils.Fov;
import com.nuclearunicorn.serialkiller.game.ai.llm.sense.DialogueLog;
import com.nuclearunicorn.serialkiller.game.ai.llm.sense.Salience;
import com.nuclearunicorn.serialkiller.game.ai.llm.sense.Stimulus;
import com.nuclearunicorn.serialkiller.game.ai.llm.sense.StimulusMemory;
import com.nuclearunicorn.serialkiller.game.world.entities.EntityRLHuman;

import java.util.List;

/**
 * Builds the reactor prompt on the game thread (§3): who the NPC is, the time of day, who is
 * nearby, what it has sensed, and the instruction to emit a JSON command program. The result
 * is an immutable String — the worker thread sees only this, never live game objects.
 *
 * <p>Structure follows salience. The strongest unconsumed stimulus gets its own block at the
 * top with an explicit instruction; everything else is background. Flattening all of it into
 * one undifferentiated "Recent memory" list is what let a 3.8B model treat "the player is
 * talking to you" as no more important than a chunk-change event — measurably so: with a
 * diluted list, half of all completions came back as an empty command array.
 */
public final class Perception {

    private static final int NEARBY_RADIUS = 8;

    private Perception() {}

    public static String snapshot(EntityRLHuman owner, StimulusMemory memory,
                                  DialogueLog dialogue, String attending) {
        StringBuilder sb = new StringBuilder(768);

        sb.append("You are ").append(owner.getName())
          .append(", a ").append(owner.age).append("-year-old ")
          .append(owner.race.diplayName()).append(" ")
          .append(owner.getSex().toString().toLowerCase())
          .append(" living in this town.\n");

        sb.append("Time: ").append(WorldTimer.is_night() ? "night" : "day").append(".\n");

        appendNearby(sb, owner);
        appendDialogue(sb, dialogue);

        Stimulus top = memory == null ? null : memory.peekTop();
        appendUrgent(sb, top, attending);
        appendBackground(sb, memory, top, dialogue);

        int maxSay = LlmRuntime.config().speech.maxSayChars;
        int maxSays = LlmRuntime.config().speech.maxSaysPerPlan;

        sb.append("\nDecide what to do next. Reply ONLY with a JSON array of commands.");
        sb.append(" Never reply with an empty array.\n");
        sb.append("At most ").append(maxSays).append(" 'say' command per reply, ")
          .append("one short sentence under ").append(maxSay).append(" characters.\n");
        sb.append("Do not repeat a line you have already said, and do not greet someone you have")
          .append(" already greeted. Never repeat a line someone else said.\n");
        if (attending != null) {
            sb.append("You are in the middle of a conversation with ").append(attending)
              .append(" - stay where you are and keep talking, do not walk off.\n");
        }
        sb.append("Commands: {\"verb\":\"goto\",\"target\":\"<home|random|uid>\"}, ");
        sb.append("{\"verb\":\"say\",\"text\":\"<line>\"}, ");
        sb.append("{\"verb\":\"wait\",\"ticks\":<n>}.\n");

        return sb.toString();
    }

    /** What has actually been said, in order and attributed. See {@link DialogueLog}. */
    private static void appendDialogue(StringBuilder sb, DialogueLog dialogue) {
        if (dialogue == null || dialogue.isEmpty()) {
            return;
        }
        sb.append('\n').append(dialogue.render());
    }

    /** The one signal that earned the re-plan, stated as a demand rather than a memory. */
    private static void appendUrgent(StringBuilder sb, Stimulus top, String attending) {
        if (top == null || top.salience < Salience.NOTABLE) {
            return;
        }
        sb.append("\nRIGHT NOW: ").append(top.text()).append("\n");
        if (top.salience >= Salience.URGENT) {
            sb.append("This is an emergency. React to it immediately.\n");
        } else if (top.salience >= Salience.DIRECTED) {
            sb.append("You are being spoken to directly. Answer with a 'say' command before doing anything else.\n");
        }
    }

    /** Everything else, strongest first, clearly subordinate to the block above. */
    private static void appendBackground(StringBuilder sb, StimulusMemory memory, Stimulus top,
                                         DialogueLog dialogue) {
        if (memory == null || memory.isEmpty()) {
            return;
        }
        boolean haveTranscript = dialogue != null && !dialogue.isEmpty();
        List<Stimulus> ranked = memory.ranked();
        boolean any = false;
        for (Stimulus s : ranked) {
            if (s == top || s.salience < Salience.NOTABLE) {
                continue;   // ambient churn is not worth prompt budget
            }
            if (haveTranscript && s.channel == Stimulus.Channel.SPEECH) {
                continue;   // already in the transcript, attributed and in order
            }
            if (!any) {
                sb.append("Background, most important first:\n");
                any = true;
            }
            sb.append("- ").append(s.text()).append("\n");
        }
    }

    private static void appendNearby(StringBuilder sb, EntityRLHuman owner) {
        Entity[] ents = Fov.get_entity_in_radius(
                owner.getEnvironment().getEntityManager(),
                owner.origin, NEARBY_RADIUS, owner.getLayerId());

        boolean any = false;
        for (Entity ent : ents) {
            if (ent == owner) {
                continue;
            }
            if (ent.isPlayerEnt() || ent instanceof EntityRLHuman) {
                if (!any) {
                    sb.append("Nearby: ");
                    any = true;
                } else {
                    sb.append(", ");
                }
                sb.append(ent.isPlayerEnt() ? "the player" : ent.getName());
            }
        }
        if (any) {
            sb.append(".\n");
        }

        if (Player.get_ent() != null && Fov.in_range(owner.origin, Player.get_origin(), NEARBY_RADIUS)) {
            sb.append("The player is watching you.\n");
        }
    }
}
