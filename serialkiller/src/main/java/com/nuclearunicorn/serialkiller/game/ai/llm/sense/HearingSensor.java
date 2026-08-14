package com.nuclearunicorn.serialkiller.game.ai.llm.sense;

import com.nuclearunicorn.libroguelike.core.client.ClientGameEnvironment;
import com.nuclearunicorn.libroguelike.events.Event;
import com.nuclearunicorn.libroguelike.events.IEventListener;
import com.nuclearunicorn.libroguelike.events.network.EChatMessage;
import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.serialkiller.game.ai.TownAI;
import com.nuclearunicorn.serialkiller.game.ai.llm.LlmDebug;
import com.nuclearunicorn.serialkiller.game.sound.Acoustics;
import com.nuclearunicorn.serialkiller.game.sound.Ambient;
import com.nuclearunicorn.serialkiller.game.sound.SoundConfig;
import com.nuclearunicorn.serialkiller.game.sound.SoundField;
import com.nuclearunicorn.serialkiller.game.sound.SoundKind;
import com.nuclearunicorn.serialkiller.game.world.RLTile;

/**
 * The hearing sensor (§8): one subscriber that turns every {@code EChatMessage} into a
 * ranked {@link Stimulus} for whoever is in earshot.
 *
 * <p>Two things were wrong originally. Player speech was hand-delivered from
 * {@code InGameMode}, so it was a special case rather than a sense; and NPC speech was
 * delivered to nobody at all — {@code SayCommand} posted a chat event that only the
 * text-bubble renderer consumed, which is why NPCs never answered each other.
 *
 * <p>Distance used to set the band, and that was the third thing wrong. "Standing near
 * someone" in a room is 5-10 tiles but only {@code directedRadius} (4) counted as addressed,
 * so a deliberate hello landed as NOTABLE — below the interrupt threshold, hence no reply.
 * The patch for that dead band was to promote the nearest hearer unconditionally.
 *
 * <p>Both are gone. Speech now propagates through {@link Acoustics} like every other noise,
 * and the band is set by <i>received level</i>: loud in your ear means you are being
 * addressed, faint across the room means you overheard something. That reproduces the old
 * radii almost exactly outdoors (SOUND_DESIGN.md 4.5 — 3 and 9 tiles against the hand-tuned
 * 4 and 10) while finally being correct indoors, where a wall now exists between the speaker
 * and the listener and a shut door is the difference between hearing a confession and not.
 */
public class HearingSensor implements IEventListener {

    private static HearingSensor instance;

    public static void init() {
        if (instance != null) {
            return;
        }
        instance = new HearingSensor();
        ClientGameEnvironment.getEnvironment().getEventManager().subscribe(instance);
        LlmDebug.log("hearing sensor subscribed (speech %ddB, directed at >=%ddB)",
                SoundKind.TALK.db(), SoundConfig.DIRECTED_LEVEL);
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

        SoundField field = Acoustics.propagate(speaker.origin, SoundKind.TALK.db(),
                speaker.getLayerId());
        if (field == null) {
            return;
        }

        Entity[] ents = ClientGameEnvironment.getEnvironment()
                .getEntityManager().getEntities(speaker.getLayerId());

        for (int i = 0; i < ents.length; i++) {
            Entity listener = ents[i];
            if (listener == null || listener == speaker || listener.origin == null) {
                continue;
            }
            if (!(listener.getAI() instanceof TownAI)) {
                continue;
            }
            int received = field.received(listener.x(), listener.y());
            if (received == Integer.MIN_VALUE) {
                continue;
            }
            RLTile tile = tileOf(listener);
            if (received <= Ambient.threshold(tile, Acoustics.threshold(listener))) {
                continue;   // drowned out by the street, or through too much wall
            }

            boolean addressed = received >= SoundConfig.DIRECTED_LEVEL;

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

    private static RLTile tileOf(Entity ent) {
        return ent.tile instanceof RLTile ? (RLTile) ent.tile : null;
    }
}
