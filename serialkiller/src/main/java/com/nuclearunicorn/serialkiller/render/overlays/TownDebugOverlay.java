package com.nuclearunicorn.serialkiller.render.overlays;

import com.nuclearunicorn.libroguelike.core.Input;
import com.nuclearunicorn.libroguelike.core.client.ClientGameEnvironment;
import com.nuclearunicorn.libroguelike.core.replay.Replay;
import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.game.player.Player;
import com.nuclearunicorn.libroguelike.game.world.WorldView;
import com.nuclearunicorn.libroguelike.render.WindowRender;
import com.nuclearunicorn.serialkiller.game.ai.Libido;
import com.nuclearunicorn.serialkiller.game.ai.PoliceAI;
import com.nuclearunicorn.serialkiller.game.ai.ProstituteAI;
import com.nuclearunicorn.serialkiller.game.ai.TownAI;
import com.nuclearunicorn.serialkiller.game.ai.llm.InferenceService;
import com.nuclearunicorn.serialkiller.game.ai.llm.LlamaHttpInferenceService;
import com.nuclearunicorn.serialkiller.game.ai.llm.LlmRuntime;
import com.nuclearunicorn.serialkiller.game.ai.llm.sense.GameTurn;
import com.nuclearunicorn.serialkiller.game.ai.mind.Deliberation;
import com.nuclearunicorn.serialkiller.game.bodysim.BodySimulation;
import com.nuclearunicorn.serialkiller.game.social.TownLog;
import com.nuclearunicorn.serialkiller.game.world.RLWorldModel;
import com.nuclearunicorn.serialkiller.game.world.entities.EntityRLHuman;
import com.nuclearunicorn.serialkiller.render.RLMessages;
import org.lwjgl.util.Point;
import org.newdawn.slick.Color;
import org.newdawn.slick.TrueTypeFont;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The other half of ALT: not one NPC's mind, but the whole town's.
 *
 * <p>{@link NpcDebugOverlay} answers "why did <i>she</i> do that". It cannot answer the
 * questions that come up while a social simulation is actually running — is anybody
 * deliberating at all, is the model queue saturated, is half the town about to hit the libido
 * ceiling at once, did anything happen while I was three streets away. Those are population
 * statistics and a chronology, and neither fits in a per-NPC inspector.
 *
 * <p>Note in particular the event feed. The message console is deliberately gated on the
 * player's senses now — you only read about what you saw or heard — which is right for play
 * and useless for tuning, so the town keeps its own unfiltered record in {@link TownLog} and
 * this is where it is read.
 *
 * <p>F8 toggles it; ALT shows it, like everything else here.
 */
public final class TownDebugOverlay {

    private TownDebugOverlay() {}

    /** {@code -Dlrl.town=true} forces it on for an offscreen capture, as lrl.npc does. */
    private static final String FORCED = System.getProperty("lrl.town");

    private static final int PANEL_W = 400;
    private static final int MARGIN = 8;
    private static final int PANEL_TOP = 52;
    private static final int PANEL_BOTTOM_GAP = 40;

    /** Recent events worth the space. The feed is the tail of the night, not the whole night. */
    private static final int MAX_EVENTS = 12;

    private static boolean shown = FORCED != null;

    /** F8. */
    public static void toggle() {
        shown = !shown;
        RLMessages.message("town overlay: " + (shown ? "on (hold ALT)" : "off"), Color.yellow);
    }

    public static void render() {
        if (!shown || (!Input.key_state_alt && FORCED == null)) {
            return;
        }
        List<EntityRLHuman> town = townsfolk();
        Census census = new Census(town);

        TrueTypeFont font = Panel.font();
        int maxY = WindowRender.get_window_h() - PANEL_BOTTOM_GAP;
        Panel.Text text = new Panel.Text(font, MARGIN + 6, PANEL_TOP + 4, PANEL_W - 12, maxY);

        text.line("TOWN  turn " + GameTurn.current(), Panel.HEADER);
        population(text, census);
        drives(text, census);
        states(text, census);
        llm(text, census);
        places(text);
        events(text);
        text.blank();
        text.line("ALT hold  F8 hide", Panel.DIM);

        Panel.backdrop(MARGIN, PANEL_TOP, PANEL_W, text.height() + 8);
        text.flush();
    }

    private static void population(Panel.Text text, Census census) {
        text.section("POPULATION");
        text.wrap("people " + census.total
                + "   adults " + census.adults
                + "   asleep " + census.asleep
                + "   dead " + census.dead, Panel.VALUE);
        text.wrap("police " + census.police
                + "   working girls " + census.prostitutes
                + "   infected " + census.infected
                + "   bleeding " + census.bleeding,
                census.infected > 0 || census.bleeding > 0 ? Panel.WARM : Panel.KEY);
        //who has somebody at home, and who has to go out for it - the drive's supply side
        text.wrap("married " + census.married + "/" + census.adults
                + "   single " + (census.adults - census.married), Panel.KEY);
    }

    /**
     * The drive that now steers behaviour, across the whole population. "Frenzied" is the
     * number that matters: those are the adults with no lawful outlet left, and each of them
     * is one turn away from {@code RapeAction}.
     */
    private static void drives(Panel.Text text, Census census) {
        text.section("DRIVES");
        if (census.adults == 0) {
            text.line("nobody to have any", Panel.DIM);
            return;
        }
        int mean = Math.round(census.libidoSum / census.adults);
        text.line("libido    " + Panel.bar(mean, 100, 20) + " mean " + mean, Panel.KEY);
        text.wrap("needy " + census.needy + "/" + census.adults
                + "   FRENZIED " + census.frenzied
                + "   seeking " + census.count(SEX_STATE)
                + "   forcing " + census.count(RAPE_STATE),
                census.frenzied > 0 ? Panel.HOT : Panel.LUST);
    }

    /** Who is doing what, biggest group first: one line and the town's mood is legible. */
    private static void states(Panel.Text text, Census census) {
        text.section("DOING");
        if (census.states.isEmpty()) {
            text.line("nobody has a brain", Panel.DIM);
            return;
        }
        List<Map.Entry<String, Integer>> rows =
                new ArrayList<Map.Entry<String, Integer>>(census.states.entrySet());
        rows.sort((a, b) -> b.getValue() - a.getValue());
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> row : rows) {
            if (sb.length() > 0) {
                sb.append("   ");
            }
            sb.append(label(row.getKey())).append(' ').append(row.getValue());
        }
        text.wrap(sb.toString(), Panel.VALUE);
    }

    /**
     * The model, from the town's side rather than one NPC's: whether the tiers are live, how
     * many people are waiting on them, and how long the last answer took. A town that has
     * gone quiet is nearly always this panel's fault and nearly never the AI's.
     */
    private static void llm(Panel.Text text, Census census) {
        text.section("LLM");
        if (!LlmRuntime.isEnabled()) {
            text.line("disabled - reflexes only", Panel.DIM);
            return;
        }
        InferenceService reactor = LlmRuntime.reactor();
        boolean live = reactor instanceof LlamaHttpInferenceService;
        text.wrap("reactor " + (live ? "live" : "STUB")
                + "   director " + (LlmRuntime.director() == null ? "off" : "on")
                + "   near " + census.near + "/" + census.total
                + "   thinking " + census.thinking,
                live ? Panel.VALUE : Panel.WARM);
        if (live) {
            LlamaHttpInferenceService http = (LlamaHttpInferenceService) reactor;
            text.wrap("queue " + http.queueDepth() + "   in flight " + http.inFlightCount()
                    + "   last reply " + http.lastLatencyMs() + "ms", Panel.KEY);
        }
        if (LlmRuntime.degradedReason() != null) {
            text.wrap(LlmRuntime.degradedReason(), Panel.HOT);
        }
    }

    private static void places(Panel.Text text) {
        text.section("PLACES");
        text.wrap("brothel " + where(RLWorldModel.brothelLocation)
                + "   safehouse " + where(RLWorldModel.playerSafeHouseLocation), Panel.KEY);
    }

    /** Everything the town did, whether or not the player was there. See {@link TownLog}. */
    private static void events(Panel.Text text) {
        text.section("EVENTS  sex " + TownLog.count(TownLog.Kind.SEX)
                + "  rape " + TownLog.count(TownLog.Kind.RAPE)
                + "  deaths " + TownLog.count(TownLog.Kind.DEATH));
        List<TownLog.Entry> entries = TownLog.entries();
        if (entries.isEmpty()) {
            text.line("nothing yet", Panel.DIM);
            return;
        }
        int from = Math.max(0, entries.size() - MAX_EVENTS);
        for (int i = from; i < entries.size(); i++) {
            TownLog.Entry entry = entries.get(i);
            text.wrap("t" + Panel.pad(String.valueOf(entry.turn), 6)
                    + entry.text + " @" + entry.x + "," + entry.y, color(entry.kind));
        }
    }

    /** F8-and-hold is a glance; this is the paste-into-a-bug-report version. */
    public static void dump() {
        Census census = new Census(townsfolk());
        System.out.println("[TOWN] turn " + GameTurn.current()
                + " people=" + census.total + " adults=" + census.adults
                + " asleep=" + census.asleep + " needy=" + census.needy
                + " frenzied=" + census.frenzied + " married=" + census.married
                + " near=" + census.near
                + " thinking=" + census.thinking);
        for (Map.Entry<String, Integer> row : census.states.entrySet()) {
            System.out.println("[TOWN]   " + label(row.getKey()) + " " + row.getValue());
        }
        for (TownLog.Entry entry : TownLog.entries()) {
            String line = "t" + entry.turn + " " + entry.kind + " " + entry.text
                    + " @" + entry.x + "," + entry.y;
            System.out.println("[TOWN]   " + line);
            Replay.trace(line);
        }
    }

    // ------------------------------------------------------------------ census

    private static final String SEX_STATE = "ai_state_SEEKING_SEX";
    private static final String RAPE_STATE = "ai_state_RAPING";

    /** One pass over the layer, because every section above wants a slice of the same walk. */
    private static final class Census {
        int total, adults, asleep, dead, police, prostitutes, infected, bleeding;
        int needy, frenzied, near, thinking, married;
        float libidoSum;
        final Map<String, Integer> states = new LinkedHashMap<String, Integer>();

        Census(List<EntityRLHuman> town) {
            for (EntityRLHuman npc : town) {
                total++;
                if (npc.get_combat() != null && !npc.get_combat().is_alive()) {
                    dead++;
                    continue;
                }
                if (npc.isAdult()) {
                    adults++;
                    if (npc.getMate() != null) {
                        married++;
                    }
                }
                if (npc.getAI() instanceof PoliceAI) {
                    police++;
                }
                if (ProstituteAI.is(npc)) {
                    prostitutes++;
                }

                BodySimulation sim = npc.getBodysim();
                if (sim != null) {
                    if (sim.isInfected()) {
                        infected++;
                    }
                    if (sim.isBleeding()) {
                        bleeding++;
                    }
                    if (npc.isAdult()) {
                        float libido = sim.getAttribute("libido");
                        libidoSum += libido;
                        if (libido >= Libido.FRENZY) {
                            frenzied++;
                        } else if (libido >= Libido.NEEDY) {
                            needy++;
                        }
                    }
                }

                if (!(npc.getAI() instanceof TownAI)) {
                    continue;
                }
                TownAI brain = (TownAI) npc.getAI();
                if (brain.isAsleep()) {
                    asleep++;
                }
                String state = brain.getState();
                states.merge(state == null ? "?" : state, 1, Integer::sum);

                Deliberation mind = brain.deliberation();
                if (mind != null) {
                    if (mind.isNearPlayer()) {
                        near++;
                    }
                    if (mind.isBusy()) {
                        thinking++;
                    }
                }
            }
        }

        int count(String state) {
            Integer n = states.get(state);
            return n == null ? 0 : n;
        }
    }

    /** Everyone on the player's layer — the whole town, not only what is on screen. */
    private static List<EntityRLHuman> townsfolk() {
        List<EntityRLHuman> out = new ArrayList<EntityRLHuman>();
        if (ClientGameEnvironment.getEntityManager() == null) {
            return out;
        }
        List<Entity> all = ClientGameEnvironment.getEntityManager().getList(WorldView.get_zindex());
        if (all == null) {
            return out;
        }
        //defensive copy: the entity list is mutated by the simulation
        for (Entity ent : all.toArray(new Entity[0])) {
            if (ent instanceof EntityRLHuman && ent != Player.get_ent()) {
                out.add((EntityRLHuman) ent);
            }
        }
        return out;
    }

    private static String label(String state) {
        return state == null ? "?" : state.replace("ai_state_", "").toLowerCase();
    }

    private static String where(Point p) {
        return p == null ? "-" : p.getX() + "," + p.getY();
    }

    private static Color color(TownLog.Kind kind) {
        switch (kind) {
            case RAPE:   return Panel.HOT;
            case SEX:    return Panel.LUST;
            case DEATH:  return Panel.WARM;
            case ARREST: return Panel.POLICE;
            default:     return Panel.VALUE;
        }
    }
}
