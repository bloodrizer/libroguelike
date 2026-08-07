package com.nuclearunicorn.serialkiller.game.ai.llm.sense;

import com.nuclearunicorn.libroguelike.core.client.ClientGameEnvironment;
import com.nuclearunicorn.libroguelike.events.Event;
import com.nuclearunicorn.libroguelike.events.IEventListener;
import com.nuclearunicorn.libroguelike.events.network.EChatMessage;
import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.utils.Fov;
import com.nuclearunicorn.serialkiller.game.ai.LLMAgentAI;
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
        if (!(event instanceof EChatMessage) || !LlmRuntime.isEnabled()) {
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
        String speakerName = speaker.isPlayerEnt() ? "the player" : speaker.getName();

        Entity[] nearby = Fov.get_entity_in_radius(
                ClientGameEnvironment.getEnvironment().getEntityManager(),
                speaker.origin, earshot, speaker.getLayerId());

        for (Entity listener : nearby) {
            if (listener == speaker || !(listener.getAI() instanceof LLMAgentAI)) {
                continue;
            }
            boolean addressed = Fov.in_range(speaker.origin, listener.origin, directed);
            Stimulus stimulus = new Stimulus(
                    GameTurn.current(),
                    Stimulus.Channel.SPEECH,
                    addressed ? Salience.DIRECTED : Salience.NOTABLE,
                    chat.uid,
                    addressed
                            ? speakerName + " is standing right next to you and said to you: \"" + chat.message + "\""
                            : "you overheard " + speakerName + " say: \"" + chat.message + "\"");

            ((LLMAgentAI) listener.getAI()).sense(stimulus);
        }
    }
}
