package com.nuclearunicorn.serialkiller.game.ai.llm;

import com.nuclearunicorn.libroguelike.game.combat.Combat;
import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.game.player.Player;
import com.nuclearunicorn.libroguelike.game.world.WorldTimer;
import com.nuclearunicorn.libroguelike.utils.Fov;
import com.nuclearunicorn.serialkiller.game.ai.llm.sense.DialogueLog;
import com.nuclearunicorn.serialkiller.game.ai.llm.sense.GameTurn;
import com.nuclearunicorn.serialkiller.game.ai.llm.sense.Relations;
import com.nuclearunicorn.serialkiller.game.ai.llm.sense.Salience;
import com.nuclearunicorn.serialkiller.game.ai.llm.sense.Stimulus;
import com.nuclearunicorn.serialkiller.game.ai.llm.sense.StimulusMemory;
import com.nuclearunicorn.serialkiller.game.ai.mind.Knowledge;
import com.nuclearunicorn.serialkiller.game.ai.mind.Narrating;
import com.nuclearunicorn.serialkiller.game.ai.mind.Percept;
import com.nuclearunicorn.serialkiller.game.ai.mind.Persona;
import com.nuclearunicorn.serialkiller.game.ai.mind.Tuning;
import com.nuclearunicorn.serialkiller.game.world.Sight;
import com.nuclearunicorn.serialkiller.game.world.entities.EntityRLHuman;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the reactor prompt on the game thread (§3): who the NPC is, who its people are, the
 * time of day, who is nearby, what it has sensed, and the instruction to emit a JSON command
 * program. The result is an immutable String — the worker thread sees only this, never live
 * game objects.
 *
 * <p>Structure follows salience. The strongest unconsumed stimulus gets its own block at the
 * top with an explicit instruction; everything else is background. Flattening all of it into
 * one undifferentiated "Recent memory" list is what let a 3.8B model treat "the player is
 * talking to you" as no more important than a chunk-change event — measurably so: with a
 * diluted list, half of all completions came back as an empty command array.
 *
 * <p>Standing facts and happenings are also kept apart, and that split is load-bearing. A
 * stimulus decays; who your wife is does not. An NPC whose only route to a name was the
 * decaying stream could answer a question about someone in the room and had nothing whatever
 * to say about someone who had just left it.
 */
public final class Perception {

    private static final int NEARBY_RADIUS = 8;

    private Perception() {}

    /**
     * Who the NPC is and what it is already doing about its situation. Loose Strings in a
     * row are a trap — every one of these is "a person's name", and swapping two of them
     * silently produces a victim chatting with her attacker — so the framing travels as one
     * named object, and each field is filled by whoever actually knows the answer.
     */
    public static class Situation {
        /** Supplied by the brain: a policeman describes himself as one. */
        public Persona persona;
        /** In conversation with this person: a bias on cadence and framing, not an order. */
        public String attending;
        /** What the body is currently doing, from the behaviour doing it. See {@link Narrating}. */
        public String doing;
        /** True while a reflex owns the body, so conversational advice is suppressed. */
        public boolean reflexive;
    }

    /**
     * Takes the whole of {@link Knowledge} rather than just its stimulus stream. The stream
     * is what <i>happened</i>; the beliefs are what this NPC holds about particular people,
     * and those never reached the model at all — a witness to a murder could stand next to
     * the killer with the memory of it filed and no way to say so.
     */
    public static String snapshot(EntityRLHuman owner, Knowledge knowledge,
                                  DialogueLog dialogue, Situation situation) {
        StringBuilder sb = new StringBuilder(1024);
        String attending = situation.attending;
        StimulusMemory memory = knowledge == null ? null : knowledge.stream();
        List<Entity> visible = visibleHumans(owner);

        if (situation.persona != null) {
            situation.persona.describeSelf(sb, owner);
        }
        appendFamily(sb, owner);

        sb.append("Time: ").append(WorldTimer.is_night() ? "night" : "day").append(".\n");

        appendNearby(sb, owner, visible);
        appendBeliefs(sb, owner, knowledge, visible);
        appendCondition(sb, owner, situation);
        appendDialogue(sb, owner, dialogue);

        // Two different questions: what earned this re-plan, and what matters most now.
        Stimulus trigger = memory == null ? null : memory.peekTop();
        Stimulus focus = memory == null ? null : memory.peekStrongest();
        if (trigger == focus || (trigger != null && trigger.salience < Salience.DIRECTED)) {
            trigger = null;   // only a directed signal earns a line of its own; rest is background
        }
        appendUrgent(sb, focus, trigger);
        appendBackground(sb, memory, focus, trigger, dialogue);

        int maxSay = LlmRuntime.peekConfig().speech.maxSayChars;
        int maxSays = LlmRuntime.peekConfig().speech.maxSaysPerPlan;

        sb.append("\nDecide what to do next. Reply ONLY with a JSON array of commands.");
        sb.append(" Never reply with an empty array.\n");
        sb.append("At most ").append(maxSays).append(" 'say' command per reply, ")
          .append("one short sentence under ").append(maxSay).append(" characters.\n");
        sb.append("Do not repeat a line you have already said, and do not greet someone you have")
          .append(" already greeted. Never repeat a line someone else said.\n");
        // Conversation focus is a bias, not an order to stand still while something is
        // happening to you — an NPC mid-flight was told to "keep talking, do not walk off".
        if (attending != null && !situation.reflexive) {
            sb.append("You are in the middle of a conversation with ").append(attending)
              .append(" - stay where you are and keep talking, do not walk off.\n");
        }
        sb.append("Commands: {\"verb\":\"goto\",\"target\":\"<home|random|uid>\"}, ");
        sb.append("{\"verb\":\"say\",\"text\":\"<line>\"}, ");
        sb.append("{\"verb\":\"wait\",\"ticks\":<n>}.\n");

        return sb.toString();
    }

    /**
     * Name, age, species, sex — the part of a self-description that is true of everyone.
     * What the NPC is <i>for</i> is appended by its own {@link Persona}, because a
     * policeman used to be introduced as "you are Policeman, a 34-year-old human male
     * living in this town" — no uniform, no duty, no authority — so the model played him as
     * a bystander who happened to be called that.
     *
     * <p>Through {@link Relations} like every other name here, so the NPC's own is written
     * the same way as everybody else's. It was not: "You are BERTHA CARPENTER" over a roster
     * of title-cased neighbours reads as emphasis, and a model shown one name in caps says
     * it back in caps.
     */
    public static void appendIdentity(StringBuilder sb, EntityRLHuman owner) {
        sb.append("You are ").append(Relations.name(owner))
          .append(", a ").append(owner.age).append("-year-old ")
          .append(owner.race.diplayName()).append(" ")
          .append(owner.getSex().toString().toLowerCase());
    }

    /**
     * The household, by name. The town generates families and then told the model about them
     * only in passing, through whichever sensor happened to fire: a wife was "your wife" for
     * the few turns her speech was still decaying in memory, and a stranger before and after.
     *
     * <p>What that cost, exactly: JACINTA ANDREWS, asked "who am I?" by the player — her own
     * husband, standing in front of her — had no name for him anywhere in her prompt and
     * answered "I'm sorry, I don't recognise you." Correctly, given what she was handed.
     *
     * <p>Death is marked rather than filtered. In a game about a murderer, an NPC who cannot
     * tell a living wife from a buried one has nothing to grieve and nothing to suspect.
     */
    private static void appendFamily(StringBuilder sb, EntityRLHuman owner) {
        List<EntityRLHuman> kin = Relations.family(owner);
        if (kin.isEmpty()) {
            return;
        }
        sb.append("Your family: ");
        for (int i = 0; i < kin.size(); i++) {
            EntityRLHuman member = kin.get(i);
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(Relations.tagged(owner, member));
            if (!isAlive(member)) {
                sb.append(" - dead");
            }
        }
        sb.append(".\n");
    }

    /**
     * The NPC's own state — the block that was missing entirely. Everything else here is
     * something that <i>happened</i>, and happenings decay: a stabbing became a past-tense
     * bullet within a few turns, so a victim standing bleeding two tiles from her attacker
     * read her own situation as "everything is normal" and chatted. Being hurt and being
     * afraid are conditions, not events, and they belong in the prompt as long as they hold.
     */
    private static void appendCondition(StringBuilder sb, EntityRLHuman owner, Situation situation) {
        Combat combat = owner.get_combat();
        if (combat != null && combat.get_max_hp() > 0 && combat.get_hp() < combat.get_max_hp()) {
            sb.append(combat.get_hp() * 2 >= combat.get_max_hp()
                    ? "You are hurt and bleeding.\n"
                    : "You are badly wounded and barely able to stand.\n");
        }
        // Positive and actionable, and written by the behaviour actually running. "Do not
        // chat as if nothing happened" is a prohibition with no alternative, and a 3.8B
        // model handed one answered with an empty array.
        if (situation.doing != null) {
            sb.append(situation.doing);
            if (!situation.doing.endsWith("\n")) {
                sb.append('\n');
            }
        }
    }

    /** What has actually been said, in order and attributed. See {@link DialogueLog}. */
    private static void appendDialogue(StringBuilder sb, EntityRLHuman owner, DialogueLog dialogue) {
        if (dialogue == null || dialogue.isEmpty()) {
            return;
        }
        sb.append('\n').append(dialogue.render(Relations.name(owner)));
    }

    /**
     * The strongest live signal, stated as a demand rather than a memory — plus whatever
     * triggered this re-plan if that is something else. Both matter: an NPC asked a question
     * while bleeding needs to see the question, and needs to answer it as someone bleeding.
     */
    private static void appendUrgent(StringBuilder sb, Stimulus focus, Stimulus trigger) {
        if (focus == null || focus.salience < Salience.NOTABLE) {
            return;
        }
        sb.append("\nRIGHT NOW: ").append(focus.text()).append("\n");
        if (focus.salience >= Salience.URGENT) {
            sb.append("This is an emergency. React to it immediately.\n");
        } else if (focus.salience >= Salience.DIRECTED) {
            sb.append("You are being spoken to directly. Answer with a 'say' command before doing anything else.\n");
        }
        if (trigger != null) {
            sb.append("Also just now: ").append(trigger.text())
              .append("\nAnswer them, but answer as someone in the situation above.\n");
        }
    }

    /** Everything else, strongest first, clearly subordinate to the block above. */
    private static void appendBackground(StringBuilder sb, StimulusMemory memory, Stimulus focus,
                                         Stimulus trigger, DialogueLog dialogue) {
        if (memory == null || memory.isEmpty()) {
            return;
        }
        boolean haveTranscript = dialogue != null && !dialogue.isEmpty();
        List<Stimulus> ranked = memory.ranked();
        boolean any = false;
        for (Stimulus s : ranked) {
            if (s == focus || s == trigger || s.salience < Salience.NOTABLE) {
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

    /**
     * What we hold against the people we can actually see. {@link Percept} is where the game
     * already records "this one attacked me" and "this one is a killer", and until now it fed
     * the reflexes and nothing else: a witness could recognise a murderer well enough to run
     * from him and had no way to say a word about it.
     *
     * <p>Only the visible, and only the two facts that change how you speak to someone. A
     * roster of everyone this NPC has ever laid eyes on is prompt budget spent on strangers.
     *
     * <p>Package-private rather than private so a headless test can hand it a list of people
     * without standing up a world to see them in.
     */
    static void appendBeliefs(StringBuilder sb, EntityRLHuman owner, Knowledge knowledge,
                              List<Entity> visible) {
        if (knowledge == null || knowledge.beliefs().isEmpty()) {
            return;
        }
        long now = GameTurn.current();
        boolean any = false;
        for (Entity ent : visible) {
            Percept percept = knowledge.beliefs().get(ent.get_uid());
            if (percept == null) {
                continue;
            }
            String line = null;
            if (percept.isThreat(now, Tuning.priority().fleeMaxTurns)) {
                line = " attacked you.";
            } else if (percept.isCriminal(now, Tuning.priority().pursueMaxTurns)) {
                line = " is the one you know committed a crime.";
            }
            if (line == null) {
                continue;
            }
            if (!any) {
                sb.append("What you know about who is here:\n");
                any = true;
            }
            sb.append("- ").append(Relations.tagged(owner, ent)).append(line).append("\n");
        }
    }

    /**
     * Who this NPC can actually see from where they stand.
     *
     * <p>{@code Fov.in_range} is a squared-distance test and nothing more, so this list used
     * to include everyone within eight tiles through any number of walls — and being told
     * "Nearby: BRET MAYNARD" is an invitation to address him. That is most of what "they talk
     * to each other through walls" was: not a hearing bug (speech has gone through the sound
     * field since {@code HearingSensor} was rewritten) but a seeing one, in the prompt.
     *
     * <p>Sight rather than earshot on purpose. Someone silent in the next room is not
     * perceivable at all; someone talking in the next room arrives as a transcript line from
     * the hearing sensor, which is the honest way to learn they are there.
     */
    private static List<Entity> visibleHumans(EntityRLHuman owner) {
        List<Entity> visible = new ArrayList<Entity>();
        // No environment means a headless caller - a test rendering a prompt, not a bug.
        if (owner == null || owner.getEnvironment() == null || owner.origin == null) {
            return visible;
        }
        Entity[] ents = Fov.get_entity_in_radius(
                owner.getEnvironment().getEntityManager(),
                owner.origin, NEARBY_RADIUS, owner.getLayerId());

        for (Entity ent : ents) {
            if (ent == owner || ent.get_uid() == null) {
                continue;
            }
            if (!(ent.isPlayerEnt() || ent instanceof EntityRLHuman)) {
                continue;
            }
            if (!Sight.canSee(owner, ent)) {
                continue;
            }
            visible.add(ent);
        }
        return visible;
    }

    private static void appendNearby(StringBuilder sb, EntityRLHuman owner, List<Entity> visible) {
        for (int i = 0; i < visible.size(); i++) {
            sb.append(i == 0 ? "Nearby: " : ", ").append(Relations.tagged(owner, visible.get(i)));
        }
        if (!visible.isEmpty()) {
            sb.append(".\n");
        }

        //"watching" needs eyes on both ends - an NPC indoors was told this about a player
        //standing in the street outside, and behaved as though observed while alone
        if (Player.get_ent() != null && owner != null && owner.origin != null
                && Fov.in_range(owner.origin, Player.get_origin(), NEARBY_RADIUS)
                && Sight.canSee(owner, Player.get_ent())) {
            sb.append(Relations.tagged(owner, Player.get_ent())).append(" is watching you.\n");
        }
    }

    private static boolean isAlive(Entity ent) {
        return ent != null && (ent.get_combat() == null || ent.get_combat().is_alive());
    }
}
