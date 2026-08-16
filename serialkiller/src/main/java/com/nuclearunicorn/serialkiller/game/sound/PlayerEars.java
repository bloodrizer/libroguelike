package com.nuclearunicorn.serialkiller.game.sound;

import com.nuclearunicorn.libroguelike.core.client.ClientGameEnvironment;
import com.nuclearunicorn.libroguelike.events.Event;
import com.nuclearunicorn.libroguelike.events.IEventListener;
import com.nuclearunicorn.libroguelike.events.network.EChatMessage;
import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.game.player.Player;
import com.nuclearunicorn.libroguelike.game.player.PlayerSpeech;
import com.nuclearunicorn.libroguelike.game.world.WorldTile;
import com.nuclearunicorn.libroguelike.game.world.layers.WorldLayer;
import com.nuclearunicorn.serialkiller.game.ai.llm.LlmDebug;
import com.nuclearunicorn.serialkiller.game.world.RLTile;
import com.nuclearunicorn.serialkiller.render.RLMessages;
import com.nuclearunicorn.serialkiller.render.RenderConfig;
import org.newdawn.slick.Color;

/**
 * The player's own hearing, on the same terms as everyone else's.
 *
 * <p>{@link com.nuclearunicorn.serialkiller.game.ai.llm.sense.HearingSensor HearingSensor}
 * made NPCs listen through the sound field; the player was still wired straight to the raw
 * chat event, which is broadcast to the whole layer. So every line anyone
 * said anywhere reached you: a bubble drawn over a speaker you have never seen, hanging in
 * the black outside your field of view or in remembered fog, and a chat log transcribing a
 * conversation four streets away through two exterior walls.
 *
 * <p>Sight and hearing are asked separately, because the cases where they disagree are the
 * whole point. You watch two people talk across the square and cannot make out a word. You
 * hear someone in the next room, and get the words without the face. Both are information;
 * they are just not the same information, so they are not drawn the same way.
 *
 * <table>
 *   <tr><th></th><th>heard</th><th>not heard</th></tr>
 *   <tr><td><b>seen</b></td><td>the line, in a bubble</td><td>a "..." bubble: lips move</td></tr>
 *   <tr><td><b>unseen</b></td><td>a message-log line with a bearing</td><td>nothing at all</td></tr>
 * </table>
 */
public final class PlayerEars implements IEventListener, PlayerSpeech.Filter {

    /** What the player got out of one spoken line. */
    public enum Heard {
        /** Seen and heard: the words, over their head. */
        WORDS,
        /** Seen, not heard: you can tell they are talking and that is all. */
        LIPS,
        /** Heard, not seen: words and a direction, no bubble in the dark. */
        EARSHOT,
        /** Neither. */
        NOTHING
    }

    /** A verdict plus what the message log needs to say it — the arrival bearing. */
    public static final class Verdict {
        public final Heard heard;
        /** Compass word for where it came from; only meaningful for {@link Heard#EARSHOT}. */
        public final String bearing;

        Verdict(Heard heard, String bearing) {
            this.heard = heard;
            this.bearing = bearing;
        }
    }

    /** What a bubble shows when you can see someone speak but cannot hear them. */
    static final String INAUDIBLE_BUBBLE = "...";

    private static final Color OVERHEARD = new Color(170, 175, 200);

    private static PlayerEars instance;

    /**
     * Subscribe and install the speech filter for this world. Re-subscribes every time, for
     * the reason in
     * {@link com.nuclearunicorn.serialkiller.game.ai.llm.sense.HearingSensor#init}.
     */
    public static void init() {
        if (instance == null) {
            instance = new PlayerEars();
        }
        ClientGameEnvironment.getEnvironment().getEventManager().subscribe(instance);
        PlayerSpeech.setFilter(instance);
        LlmDebug.log("player ears subscribed (speech %ddB)", SoundKind.TALK.db());
    }

    // ------------------------------------------------------------- the verdict

    /**
     * What the player makes of a line spoken at {@code (sx,sy)} while standing at
     * {@code (lx,ly)}.
     *
     * <p>Takes tiles rather than entities so the question can be posed to a hand-built grid.
     * {@code visible} is the caller's, because seeing is not this class's business: in the
     * game it is the renderer's own FOV mask, which is exactly the thing that decides whether
     * there is anything on screen to hang a bubble on.
     */
    public static Verdict perceive(WorldLayer layer, int sx, int sy, int lx, int ly,
                                   int listenerThreshold, boolean visible) {
        SoundField field = Acoustics.propagate(layer, sx, sy, SoundKind.TALK.db(), 0);
        if (field == null) {
            return new Verdict(visible ? Heard.LIPS : Heard.NOTHING, null);
        }
        int received = field.received(lx, ly);
        int threshold = Ambient.threshold(tileAt(layer, lx, ly), listenerThreshold);
        boolean audible = received != Integer.MIN_VALUE && received > threshold;

        if (visible) {
            return new Verdict(audible ? Heard.WORDS : Heard.LIPS, null);
        }
        if (!audible) {
            return new Verdict(Heard.NOTHING, null);
        }
        return new Verdict(Heard.EARSHOT, Acoustics.bearing(field.directionAt(lx, ly)));
    }

    /** The same question about a live speaker, answered once per chat event and memoised. */
    public static Verdict perceive(EChatMessage chat, Entity speaker) {
        if (instance == null) {
            instance = new PlayerEars();
        }
        return instance.verdict(chat, speaker);
    }

    // ------------------------------------------------------- PlayerSpeech.Filter

    @Override
    public String bubble(EChatMessage chat, Entity speaker) {
        return bubbleText(verdict(chat, speaker).heard, chat.message);
    }

    @Override
    public boolean audible(EChatMessage chat, Entity speaker) {
        return audible(verdict(chat, speaker).heard);
    }

    /** What floats over the speaker's head, or null when there is nothing to draw it on. */
    public static String bubbleText(Heard heard, String line) {
        switch (heard) {
            case WORDS: return line;
            case LIPS:  return INAUDIBLE_BUBBLE;
            default:    return null;
        }
    }

    /** Whether the words themselves reached the player, wherever they were standing. */
    public static boolean audible(Heard heard) {
        return heard == Heard.WORDS || heard == Heard.EARSHOT;
    }

    // --------------------------------------------------------------- the event

    /**
     * Speech you heard but could not see gets a line in the message log — it is the only
     * channel that can carry it, since there is nothing on screen to draw it over.
     */
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
        Verdict verdict = verdict(chat, speaker);
        if (verdict.heard != Heard.EARSHOT) {
            return;
        }
        // Deliberately anonymous: hearing gives you the words, not the face. Naming them
        // would hand the player an identification through a wall that they never earned.
        RLMessages.message("You hear someone to the " + verdict.bearing
                + " say: \"" + chat.message + "\"", OVERHEARD);
    }

    // ------------------------------------------------------------------ innards

    private EChatMessage memoOf;
    private Verdict memo;

    /**
     * One flood per spoken line, however many times it is asked about.
     *
     * <p>Three consumers ask independently and in an order set by the listener list — the
     * bubble, the chat log, and this class's own message-log line — so the answer is cached
     * against the event that provoked it rather than recomputed for each.
     */
    private Verdict verdict(EChatMessage chat, Entity speaker) {
        if (chat != memoOf) {
            memoOf = chat;
            memo = compute(chat, speaker);
            // Which of the four it was, in the replay. "The bubble did not appear" has four
            // possible causes and they are not distinguishable from a screenshot.
            LlmDebug.log("player ears: %s said \"%s\" -> %s", speaker.getName(),
                    chat.message, memo.heard);
        }
        return memo;
    }

    private static Verdict compute(EChatMessage chat, Entity speaker) {
        Entity player = Player.get_ent();
        if (player == null || player.origin == null || speaker.origin == null) {
            return new Verdict(Heard.WORDS, null);  //no player to filter for: draw everything
        }
        if (speaker == player) {
            return new Verdict(Heard.WORDS, null);  //you always hear yourself
        }
        if (speaker.getLayerId() != player.getLayerId()) {
            return new Verdict(Heard.NOTHING, null);    //floors are acoustically sealed (11)
        }
        return perceive(ClientGameEnvironment.getWorldLayer(speaker.getLayerId()),
                speaker.x(), speaker.y(), player.x(), player.y(),
                Acoustics.threshold(player), sees(speaker));
    }

    /** The renderer's own FOV mask: if there is no lit tile there, there is nothing to draw on. */
    private static boolean sees(Entity speaker) {
        if (RenderConfig.REVEAL) {
            return true;    //screenshot and debug view: the map is drawn as if fully known
        }
        return speaker.tile instanceof RLTile && ((RLTile) speaker.tile).isVisible();
    }

    private static RLTile tileAt(WorldLayer layer, int x, int y) {
        if (layer == null) {
            return null;
        }
        WorldTile tile = layer.get_tile(x, y);
        return tile instanceof RLTile ? (RLTile) tile : null;
    }
}
