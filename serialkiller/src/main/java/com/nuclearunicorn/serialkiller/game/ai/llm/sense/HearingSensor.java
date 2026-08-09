package com.nuclearunicorn.serialkiller.game.ai.llm.sense;

import com.nuclearunicorn.libroguelike.core.client.ClientGameEnvironment;
import com.nuclearunicorn.libroguelike.events.Event;
import com.nuclearunicorn.libroguelike.events.IEventListener;
import com.nuclearunicorn.libroguelike.events.network.EChatMessage;
import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.utils.Fov;
import com.nuclearunicorn.serialkiller.game.ai.TownAI;
import com.nuclearunicorn.serialkiller.game.ai.llm.LlmDebug;
import com.nuclearunicorn.serialkiller.game.ai.llm.LlmRuntime;

/**
 * The hearing sensor (§8): one subscriber that turns every {@code EChatMessage} into a
 * ranked {@link Stimulus} for whoever is in earshot.
 *
 * <p>Two things were wrong before. Player speech was hand-delivered from {@code InGameMode},
 * so it was a special case rather than a sense; and NPC speech was delivered to nobody at
 * all — {@code SayCommand} posted a chat event that only the text-bubble renderer consumed,
 * which is why NPCs never answered each other.
 *
 * <p>Distance sets the band, which is what gives conversation its focus without a dialogue
 * manager: close enough and you are being <i>addressed</i> ({@link Salience#DIRECTED},
 * preempts the running plan); merely within earshot and you have <i>overheard</i> something
 * ({@link Salience#NOTABLE}, remembered but not urgent).
 *
 * <p>Radius alone left a dead band the player fell into constantly. "Standing near someone"
 * in a room is 5-10 tiles, but only {@code directedRadius} (4) counted as addressed, so a
 * deliberate hello landed as NOTABLE — below the interrupt threshold, hence no reply. Player
 * speech therefore also promotes the <i>nearest</i> hearer: the player types a line rarely
 * and on purpose, unlike NPC chatter, so someone should always take it as meant for them.
 */
public class HearingSensor implements IEventListener {

    private static HearingSensor instance;

    public static void init() {
        if (instance != null) {
            return;
        }
        instance = new HearingSensor();
        ClientGameEnvironment.getEnvironment().getEventManager().subscribe(instance);
        LlmDebug.log("hearing sensor subscribed (directed<=%d, earshot<=%d)",
                LlmRuntime.config().speech.directedRadius,
                LlmRuntime.config().speech.earshotRadius);
    }

    @Override
    public void e_on_event(Event event) {
        if (!(event instanceof EChatMessage)) {
            return;
        }
        EChatMessage chat = (EChatMessage) event;

        Entity speaker = ClientGameEnvironment.getEnvironment()
                .getEntityManager().get_entity(chat.uid);
        if (speaker == null) {
            return;
        }

        int directed = LlmRuntime.config().speech.directedRadius;
        int earshot = Math.max(directed, LlmRuntime.config().speech.earshotRadius);

        Entity[] nearby = Fov.get_entity_in_radius(
                ClientGameEnvironment.getEnvironment().getEntityManager(),
                speaker.origin, earshot, speaker.getLayerId());

        // The player always gets one definite addressee, whatever the radius says.
        Entity nearest = speaker.isPlayerEnt() ? nearestListener(speaker, nearby) : null;

        for (Entity listener : nearby) {
            if (listener == speaker || !(listener.getAI() instanceof TownAI)) {
                continue;
            }
            boolean addressed = listener == nearest
                    || Fov.in_range(speaker.origin, listener.origin, directed);
            // Named from this listener's side: the same speaker is "your daughter" to one
            // and a stranger to the next.
            String speakerName = Relations.describe(listener, speaker);
            Stimulus stimulus = new Stimulus(
                    GameTurn.current(),
                    Stimulus.Channel.SPEECH,
                    addressed ? Salience.DIRECTED : Salience.NOTABLE,
                    chat.uid,
                    addressed
                            ? speakerName + " is talking to you and just said: \"" + chat.message + "\""
                            : "you overheard " + speakerName + " say: \"" + chat.message + "\"");

            // Two deliveries, two jobs: the stimulus decides whether to react and how hard,
            // the transcript line is what a reply is actually built from.
            TownAI brain = (TownAI) listener.getAI();
            brain.hear(speakerName, chat.message, addressed);
            brain.sense(stimulus);
        }
    }

    /** Closest LLM-brained hearer, by squared distance; null if nobody is listening. */
    private static Entity nearestListener(Entity speaker, Entity[] nearby) {
        Entity best = null;
        long bestDist = Long.MAX_VALUE;
        for (Entity listener : nearby) {
            if (listener == speaker || !(listener.getAI() instanceof TownAI)) {
                continue;
            }
            long dx = listener.origin.getX() - speaker.origin.getX();
            long dy = listener.origin.getY() - speaker.origin.getY();
            long dist = dx * dx + dy * dy;
            if (dist < bestDist) {
                bestDist = dist;
                best = listener;
            }
        }
        return best;
    }
}
