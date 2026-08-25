package com.nuclearunicorn.serialkiller.render.overlays;

import com.nuclearunicorn.libroguelike.core.Input;
import com.nuclearunicorn.libroguelike.core.client.ClientGameEnvironment;
import com.nuclearunicorn.libroguelike.core.replay.Replay;
import com.nuclearunicorn.libroguelike.game.ai.AI;
import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.game.player.Player;
import com.nuclearunicorn.libroguelike.game.world.WorldView;
import com.nuclearunicorn.libroguelike.game.world.WorldViewCamera;
import com.nuclearunicorn.libroguelike.render.WindowRender;
import com.nuclearunicorn.serialkiller.game.ai.Libido;
import com.nuclearunicorn.serialkiller.game.ai.TownAI;
import com.nuclearunicorn.serialkiller.game.ai.llm.LlmRuntime;
import com.nuclearunicorn.serialkiller.game.ai.llm.sense.DialogueLog;
import com.nuclearunicorn.serialkiller.game.ai.llm.sense.GameTurn;
import com.nuclearunicorn.serialkiller.game.ai.llm.sense.Salience;
import com.nuclearunicorn.serialkiller.game.ai.llm.sense.Stimulus;
import com.nuclearunicorn.serialkiller.game.ai.llm.sense.StimulusMemory;
import com.nuclearunicorn.serialkiller.game.ai.mind.Deliberation;
import com.nuclearunicorn.serialkiller.game.ai.mind.Knowledge;
import com.nuclearunicorn.serialkiller.game.ai.mind.Percept;
import com.nuclearunicorn.serialkiller.game.ai.mind.Tuning;
import com.nuclearunicorn.serialkiller.game.bodysim.BodySimulation;
import com.nuclearunicorn.serialkiller.game.combat.RLCombat;
import com.nuclearunicorn.serialkiller.game.controllers.RLController;
import com.nuclearunicorn.serialkiller.game.social.CrimeRecord;
import com.nuclearunicorn.serialkiller.game.world.entities.EntityRLHuman;
import com.nuclearunicorn.serialkiller.render.Draw;
import com.nuclearunicorn.serialkiller.render.Grid;
import com.nuclearunicorn.serialkiller.render.RLMessages;
import com.nuclearunicorn.serialkiller.render.RenderConfig;
import org.lwjgl.input.Mouse;
import org.lwjgl.util.Point;
import org.newdawn.slick.Color;
import org.newdawn.slick.TrueTypeFont;

import java.util.ArrayList;
import java.util.List;

/**
 * Hold ALT to see what the town is thinking.
 *
 * <p>Two layers. Every NPC on screen gets a two-line tag over its head — name, the state
 * that owns its body, and the salience of whatever is currently shouting loudest at it — so
 * a street reads at a glance and you can see a reaction travel through it. One NPC at a
 * time gets the full inspector on the right: identity, body, the impulse list <i>with every
 * verdict</i>, the ranked stimulus stream, per-person beliefs, the planner's queue state and
 * the model's last reply verbatim, and the tail of its conversation.
 *
 * <p>What this replaces was a UUID drawn next to the '@' in cell-sized type. It was the
 * whole of the NPC debug view and it answered nothing: a uid is not a reason, and the one
 * question worth asking of this AI — <i>why did she do that instead of the obvious thing</i>
 * — is decided by a walk down the impulse list, a decayed salience number and a queue state,
 * none of which were visible anywhere but a replay you had to go and read afterwards.
 *
 * <p>The inspector follows the mouse. F5 pins it to whoever is under the cursor so the NPC
 * can be watched across turns while you play, F6 cycles through everyone on screen, and F7
 * dumps the whole brain — the last prompt included — to stdout and the replay.
 */
public final class NpcDebugOverlay {

    private NpcDebugOverlay() {}

    /**
     * {@code -Dlrl.npc=true} forces the overlay on with no key held, so an offscreen capture
     * can show it. {@code -Dlrl.npc=<text>} also pins the inspector to the first NPC whose
     * name or uid contains that text — how a screenshot names its subject.
     */
    private static final String FORCED = System.getProperty("lrl.npc");

    private static final int PANEL_W = 430;
    private static final int MARGIN = 8;
    /** Clear of the version string, which the overlay system draws in the same corner. */
    private static final int PANEL_TOP = 52;
    /**
     * How much of the window bottom the panel will not use. Small on purpose: the panel is
     * only as tall as it needs to be, and stopping it clear of the console meant a busy
     * brain lost the planner and the transcript — the two sections that say what the NPC is
     * about to do — to a gap that was empty most of the time.
     */
    private static final int PANEL_BOTTOM_GAP = 40;

    // How much of each unbounded section is worth the space it costs. Everything below the
    // one that overruns is lost, and the planner and the transcript sit at the bottom — so
    // these are cut to what fits a 768-high window with room to spare, and each section
    // says how many rows it elided.
    private static final int MAX_STIMULI = 4;
    private static final int MAX_BELIEFS = 4;
    private static final int MAX_PLAN = 4;
    private static final int MAX_TALK = 4;

    // One palette across every debug panel; see Panel.
    private static final Color HEADER = Panel.HEADER;
    private static final Color SECTION = Panel.SECTION;
    private static final Color KEY = Panel.KEY;
    private static final Color VALUE = Panel.VALUE;
    private static final Color DIM = Panel.DIM;
    private static final Color HOT = Panel.HOT;
    private static final Color WARM = Panel.WARM;
    private static final Color GOOD = Panel.GOOD;
    private static final Color POLICE = Panel.POLICE;
    private static final Color LUST = Panel.LUST;

    /** Uid the inspector is locked to, or null while it follows the mouse. */
    private static String pinnedUid;

    // ------------------------------------------------------------------ input

    /** F5: lock the inspector onto whoever it is showing, or let go. */
    public static void togglePin() {
        if (pinnedUid != null) {
            pinnedUid = null;
            return;
        }
        EntityRLHuman focus = focus(visibleNpcs());
        pinnedUid = focus == null ? null : focus.get_uid();
    }

    /** F6: next NPC on screen, pinned. The no-mouse way through a crowd. */
    public static void cycleFocus() {
        List<EntityRLHuman> npcs = visibleNpcs();
        if (npcs.isEmpty()) {
            pinnedUid = null;
            return;
        }
        EntityRLHuman current = focus(npcs);
        int at = -1;
        for (int i = 0; i < npcs.size(); i++) {
            if (npcs.get(i) == current) {
                at = i;
                break;
            }
        }
        pinnedUid = npcs.get((at + 1) % npcs.size()).get_uid();
    }

    /**
     * F7: the whole brain to stdout and the replay, prompt included. The panel is a glance;
     * this is the thing you paste into a bug report.
     */
    public static void dumpFocus() {
        EntityRLHuman npc = focus(visibleNpcs());
        TownAI brain = brainOf(npc);
        if (brain == null) {
            dump("nothing focused - hover an NPC, or F6 to cycle");
            return;
        }
        dump("---- brain dump: " + npc.getName() + " (" + npc.get_uid() + ") ----");
        dump(brain.debugState());
        for (AI.ImpulseView impulse : brain.debugImpulses()) {
            dump("  impulse " + pad(String.valueOf(impulse.priority), 4)
                    + pad(impulse.name, 14) + verdict(impulse.verdict)
                    + (impulse.selected ? "  <- SELECTED" : ""));
        }
        Deliberation mind = brain.deliberation();
        if (mind != null) {
            for (String step : mind.plan()) {
                dump("  plan " + step);
            }
            dump("  last reply: " + mind.lastCompletion());
            dump("  last prompt:\n" + mind.lastPrompt());
        }
        dump("---- end dump ----");
        RLMessages.message("brain dump: " + npc.getName() + " -> stdout", Color.yellow);
    }

    /**
     * Print, and mirror into the replay. Deliberately not {@link LlmDebug#log} — that is
     * gated on {@code llm.debug}, so a key you pressed on purpose printed nothing at all
     * unless LLM tracing happened to be switched on.
     */
    private static void dump(String line) {
        System.out.println("[NPC] " + line);
        Replay.trace(line);
    }

    // ----------------------------------------------------------------- render

    public static void render() {
        if (!Input.key_state_alt && FORCED == null) {
            return;
        }
        List<EntityRLHuman> npcs = visibleNpcs();
        EntityRLHuman focus = focus(npcs);

        for (EntityRLHuman npc : npcs) {
            drawTag(npc, npc == focus);
        }
        drawPanel(focus);
    }

    /** Two lines over the head: who, and what is driving them right now. */
    private static void drawTag(EntityRLHuman npc, boolean focused) {
        float x = Grid.cellX(npc.x()) - WorldViewCamera.camera_x;
        float y = Grid.boxTop(npc.y()) - WorldViewCamera.camera_y;
        TrueTypeFont font = Panel.tagFont();
        int lh = font.getHeight() - 2;

        TownAI brain = brainOf(npc);
        String who = shortName(npc);
        String what = brain == null ? "no brain" : stateLabel(brain.getState()) + markers(brain);

        float w = Math.max(font.getWidth(who), font.getWidth(what)) + 6;
        float top = y - lh * 2 - 4;

        Draw.beginFlat();
        Draw.quad(x - 2, top, w, lh * 2 + 2, 0, 0, 0, focused ? 0.75f : 0.45f);
        Draw.endFlat();
        if (focused) {
            Panel.box(x - 2, top, w, lh * 2 + 2, HEADER);
            Panel.box(x, Grid.boxTop(npc.y()) - WorldViewCamera.camera_y,
                    RenderConfig.CELL, RenderConfig.spriteH(), HEADER);
        }

        font.drawString(x + 1, top, who, focused ? HEADER : nameColor(npc, brain));
        font.drawString(x + 1, top + lh, what, brain == null ? DIM : stateColor(brain));
    }

    /** Compact flags: what is loud, what is queued, and what has the body. */
    private static String markers(TownAI brain) {
        StringBuilder sb = new StringBuilder();
        Knowledge knowledge = brain.knowledge();
        if (knowledge != null) {
            int top = knowledge.stream().topSalience();
            if (top > 0) {
                sb.append(' ').append(Salience.label(top).charAt(0)).append(top);
            }
            if (knowledge.threat() != null) {
                sb.append(" !");
            }
        }
        Deliberation mind = brain.deliberation();
        if (mind != null) {
            if (mind.isBusy()) {
                sb.append(" ~");    // a request is in flight
            }
            if (!mind.isIdle()) {
                sb.append(" >");    // a plan is queued
            }
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------ panel

    private static void drawPanel(EntityRLHuman npc) {
        TrueTypeFont font = Panel.font();
        int x = WindowRender.get_window_w() - PANEL_W - MARGIN;
        int maxY = WindowRender.get_window_h() - PANEL_BOTTOM_GAP;

        TownAI brain = brainOf(npc);
        if (brain == null) {
            Panel.backdrop(x, PANEL_TOP, PANEL_W, font.getHeight() + 8);
            font.drawString(x + 6, PANEL_TOP + 4,
                    "no NPC under cursor - hover one, or F6 to cycle", DIM);
            return;
        }

        Panel.Text text = new Panel.Text(font, x + 6, PANEL_TOP + 4, PANEL_W - 12, maxY);

        header(text, npc, brain);
        body(text, npc);
        doing(text, brain);
        impulses(text, brain);
        mind(text, brain);
        beliefs(text, brain);
        planner(text, brain);
        talk(text, brain);
        text.blank();
        text.line("ALT hold  F5 pin  F6 cycle  F7 dump to log", DIM);

        // Background last: the layout decides its own height, and a panel sized to the
        // section list beats a fixed box with a ragged half-empty bottom.
        Panel.backdrop(x, PANEL_TOP, PANEL_W, text.height() + 8);
        text.flush();
    }

    private static void header(Panel.Text text, EntityRLHuman npc, TownAI brain) {
        String name = npc.getName() == null ? "?" : npc.getName();
        text.line(name + (npc.get_uid() != null && npc.get_uid().equals(pinnedUid)
                ? "   [PINNED]" : ""), HEADER);
        text.wrap(npc.getSex().name().toLowerCase() + " " + npc.age
                + " / " + npc.race.diplayName()
                + " / " + brain.getClass().getSimpleName()
                + " / " + shortUid(npc.get_uid()), KEY);
    }

    private static void body(Panel.Text text, EntityRLHuman npc) {
        text.section("BODY");
        StringBuilder sb = new StringBuilder();
        if (npc.get_combat() != null) {
            sb.append("hp ").append(npc.get_combat().get_hp())
              .append("/").append(npc.get_combat().get_max_hp());
            if (npc.get_combat() instanceof RLCombat) {
                sb.append("  fov ").append(((RLCombat) npc.get_combat()).getFovRadius());
            }
        }
        if (npc.origin != null) {
            sb.append("  at ").append(npc.origin.getX()).append(",").append(npc.origin.getY());
            Point player = Player.get_origin();
            if (player != null) {
                sb.append("  d").append(distance(npc.origin, player));
            }
        }
        text.wrap(sb.toString(), VALUE);

        List<String> flags = new ArrayList<String>();
        if (npc.get_combat() != null && !npc.get_combat().is_alive()) {
            flags.add("DEAD");
        }
        BodySimulation sim = npc.getBodysim();
        if (sim != null) {
            if (sim.isBleeding()) {
                flags.add("bleeding");
            }
            if (sim.isStunned()) {
                flags.add("stunned");
            }
            if (sim.isFainted()) {
                flags.add("fainted");
            }
            if (sim.isInfected()) {
                flags.add("infected");
            }
        }
        if (!npc.crimeRecords.isEmpty()) {
            StringBuilder crimes = new StringBuilder("crimes:");
            for (CrimeRecord record : npc.crimeRecords) {
                crimes.append(' ').append(record.crimeType.diplayName())
                      .append('x').append(record.count);
            }
            flags.add(crimes.toString());
        }
        if (!flags.isEmpty()) {
            text.wrap(join(flags, ", "), HOT);
        }
        drives(text, sim);
    }

    /**
     * The homeostatic needs, as bars. Libido is why this section exists: it now drives two
     * impulses of its own, so an NPC walking across town for no visible reason is explained
     * by a number that was previously readable nowhere at all.
     */
    private static void drives(Panel.Text text, BodySimulation sim) {
        if (sim == null) {
            return;
        }
        float libido = sim.getAttribute("libido");
        text.line("libido    " + Panel.bar(libido, 100, 20) + " " + Math.round(libido)
                + (libido >= Libido.FRENZY ? "  FRENZY"
                        : libido >= Libido.NEEDY ? "  needy" : ""),
                libido >= Libido.FRENZY ? HOT : libido >= Libido.NEEDY ? LUST : KEY);
        text.line("bloodlust " + Panel.bar(sim.getAttribute("bloodlust"), 100, 20)
                + " " + Math.round(sim.getAttribute("bloodlust")), KEY);
        text.line("hunger    " + Panel.bar(sim.getAttribute("hunger"), 100, 20)
                + " " + Math.round(sim.getAttribute("hunger")), KEY);
        text.line("stamina   " + Panel.bar(sim.getAttribute("stamina"), 100, 20)
                + " " + Math.round(sim.getAttribute("stamina")), KEY);
    }

    private static void doing(Panel.Text text, TownAI brain) {
        text.section("DOING");
        Object action = brain.debugActiveAction();
        text.wrap(stateLabel(brain.getState())
                + "   <- " + (action == null ? "no action" : action.getClass().getSimpleName()),
                stateColor(brain));

        String narration = brain.debugDoing();
        if (narration != null) {
            text.wrap("\"" + narration + "\"", VALUE);
        }
        if (brain.human() != null && brain.human().controller instanceof RLController) {
            List<Point> path = brain.ctrl().path;
            Point destination = brain.ctrl().destination;
            text.line("path " + (path == null ? "none" : path.size() + " steps")
                    + (destination == null ? ""
                            : " -> " + destination.getX() + "," + destination.getY()), KEY);
        }
    }

    /**
     * The decision itself. Every trigger, its verdict this turn, and the one that won —
     * an NPC that will not react is answered by whichever row above the one you expected
     * said yes.
     */
    private static void impulses(Panel.Text text, TownAI brain) {
        text.section("IMPULSES");
        List<AI.ImpulseView> walk = brain.debugImpulses();
        if (walk.isEmpty()) {
            text.line("not yet decided", DIM);
            return;
        }
        for (AI.ImpulseView impulse : walk) {
            String row = pad(String.valueOf(impulse.priority), 4)
                    + pad(impulse.name, 14)
                    + verdict(impulse.verdict);
            if (impulse.selected) {
                row += "   <- " + stateLabel(impulse.state);
            }
            text.line(row, impulse.selected ? GOOD
                    : (impulse.verdict == AI.ImpulseView.Verdict.NOT_ASKED ? DIM : VALUE));
        }
    }

    private static void mind(Panel.Text text, TownAI brain) {
        text.section("MIND");
        Knowledge knowledge = brain.knowledge();
        if (knowledge == null) {
            text.line("unwired", DIM);
            return;
        }
        StimulusMemory stream = knowledge.stream();
        int interruptAt = Tuning.priority().interruptAt;
        int top = stream.topSalience();
        text.wrap("top " + top + " / interrupt at " + interruptAt
                + "   memory " + stream.size() + "/" + stream.capacity()
                + "   turn " + GameTurn.current(),
                top >= interruptAt ? HOT : KEY);

        List<Stimulus> ranked = stream.ranked();
        if (ranked.isEmpty()) {
            text.line("nothing sensed", DIM);
            return;
        }
        int shown = 0;
        for (Stimulus stimulus : ranked) {
            if (shown++ >= MAX_STIMULI) {
                text.line("... " + (ranked.size() - MAX_STIMULI) + " more", DIM);
                break;
            }
            int effective = stream.effectiveSalience(stimulus);
            String head = pad(Salience.label(stimulus.salience).substring(0, 1) + effective, 5)
                    + pad(stimulus.channel.name(), 7)
                    + "t" + stimulus.turn + (stimulus.consumed ? " used " : " NEW  ");
            text.wrap(head + stimulus.text(),
                    stimulus.consumed ? DIM : (effective >= interruptAt ? HOT : VALUE));
        }
    }

    /** Who this NPC has an opinion about, and how sure it still is. */
    private static void beliefs(Panel.Text text, TownAI brain) {
        text.section("BELIEFS");
        Knowledge knowledge = brain.knowledge();
        if (knowledge == null || knowledge.beliefs().isEmpty()) {
            text.line("nobody", DIM);
        } else {
            long now = GameTurn.current();
            int ttl = Tuning.priority().pursueMaxTurns;
            int shown = 0;
            for (Percept percept : knowledge.beliefs().values()) {
                if (shown++ >= MAX_BELIEFS) {
                    text.line("... " + (knowledge.beliefs().size() - MAX_BELIEFS) + " more", DIM);
                    break;
                }
                StringBuilder row = new StringBuilder();
                row.append(pad(nameOf(brain, percept.uid), 16));
                row.append("seen t").append(percept.lastSeenTurn);
                row.append(" conf ").append(percentage(percept.confidence(now, ttl)));
                row.append(' ').append(percept.source);
                if (percept.lastKnownAt != null) {
                    row.append(" @").append(percept.lastKnownAt.getX())
                       .append(",").append(percept.lastKnownAt.getY());
                }
                boolean dangerous = percept.attackedUsTurn >= 0 || percept.crimeTurn >= 0;
                if (percept.attackedUsTurn >= 0) {
                    row.append(" ATTACKED t").append(percept.attackedUsTurn);
                }
                if (percept.crimeTurn >= 0) {
                    row.append(" CRIME t").append(percept.crimeTurn);
                }
                text.wrap(row.toString(), dangerous ? WARM : VALUE);
            }
        }
        if (knowledge == null) {
            return;
        }
        Percept threat = knowledge.threat();
        Percept suspect = knowledge.suspect();
        Point scene = knowledge.openCrimeScene();
        text.wrap("threat " + (threat == null ? "-" : nameOf(brain, threat.uid))
                + "   suspect " + (suspect == null ? "-" : nameOf(brain, suspect.uid))
                + "   scene " + (scene == null ? "-" : scene.getX() + "," + scene.getY()),
                threat != null || suspect != null ? WARM : KEY);

        reflections(text, knowledge);
    }

    /**
     * The director tier's output: durable second-order beliefs that do not decay. They go
     * into every later reactor prompt, so an NPC saying something inexplicable is usually
     * explained here rather than by anything it has just sensed.
     */
    private static void reflections(Panel.Text text, Knowledge knowledge) {
        text.section("REFLECTIONS");
        if (knowledge.reflections().isEmpty()) {
            text.line(LlmRuntime.director() == null ? "director tier off" : "nothing yet", DIM);
            return;
        }
        for (String belief : knowledge.reflections()) {
            text.wrap("\"" + belief + "\"", SECTION);
        }
    }

    /** The slow half: whether the model has the NPC, and what it last said back. */
    private static void planner(Panel.Text text, TownAI brain) {
        text.section("PLANNER");
        Deliberation mind = brain.deliberation();
        if (mind == null) {
            text.line(LlmRuntime.isEnabled() ? "not wired" : "llm off - reflexes only", DIM);
            return;
        }
        long since = mind.turnsSinceRequest();
        text.wrap("near " + yn(mind.isNearPlayer())
                + "  busy " + yn(mind.isBusy())
                + "  idle " + yn(mind.isIdle())
                + "  since " + (since < 0 ? "never" : String.valueOf(since))
                + "/" + mind.debugCadence()
                + "  attn " + (mind.hasAttention()
                        ? nameOf(brain, mind.attentionUid()) : "-"),
                mind.isNearPlayer() ? VALUE : DIM);
        if (mind.lastReason() != null) {
            text.line("asked because: " + mind.lastReason(), KEY);
        }
        if (mind.lastCompletion() != null) {
            text.wrap("reply t" + mind.lastCompletionTurn() + ": "
                    + mind.lastCompletion().replace('\n', ' '), SECTION);
        }
        List<String> plan = mind.plan();
        if (plan.isEmpty()) {
            text.line("plan: empty", DIM);
            return;
        }
        long planTurn = mind.planTurn();
        text.line("plan: composed t" + planTurn
                + (mind.planStarted() ? " (running)" : " (never ran)"),
                mind.planStarted() ? KEY : WARM);
        int shown = 0;
        for (String step : plan) {
            if (shown++ >= MAX_PLAN) {
                text.line("  ... " + (plan.size() - MAX_PLAN) + " more", DIM);
                break;
            }
            text.wrap(step, step.startsWith(">") ? GOOD : VALUE);
        }
    }

    private static void talk(Panel.Text text, TownAI brain) {
        if (brain.voice() == null || brain.voice().log().isEmpty()) {
            return;
        }
        text.section("TALK");
        List<DialogueLog.Line> lines = brain.voice().log().lines();
        int from = Math.max(0, lines.size() - MAX_TALK);
        for (int i = from; i < lines.size(); i++) {
            DialogueLog.Line line = lines.get(i);
            boolean self = DialogueLog.isSelf(line);
            text.wrap((self ? "you" : line.speaker) + (line.overheard ? " (overheard)" : "")
                    + ": \"" + line.text + "\"", self ? GOOD : VALUE);
        }
    }

    // ----------------------------------------------------------------- picking

    /** Every human on screen except the player, front row last so picking prefers the front. */
    private static List<EntityRLHuman> visibleNpcs() {
        List<EntityRLHuman> out = new ArrayList<EntityRLHuman>();
        if (ClientGameEnvironment.getEntityManager() == null) {
            return out;
        }
        List<Entity> all = ClientGameEnvironment.getEntityManager()
                .getList(WorldView.get_zindex());
        if (all == null) {
            return out;
        }
        // defensive copy: the entity list is mutated by the simulation
        Entity[] snapshot = all.toArray(new Entity[0]);
        for (Entity ent : snapshot) {
            if (!(ent instanceof EntityRLHuman) || ent == Player.get_ent() || ent.origin == null) {
                continue;
            }
            if (Grid.onScreen(ent.x(), ent.y())) {
                out.add((EntityRLHuman) ent);
            }
        }
        out.sort((a, b) -> a.y() == b.y() ? a.x() - b.x() : a.y() - b.y());
        return out;
    }

    /** Pinned wins, then whoever the mouse is over, then the forced or nearest NPC. */
    private static EntityRLHuman focus(List<EntityRLHuman> npcs) {
        if (pinnedUid != null) {
            for (EntityRLHuman npc : npcs) {
                if (pinnedUid.equals(npc.get_uid())) {
                    return npc;
                }
            }
        }
        EntityRLHuman hovered = hovered(npcs);
        if (hovered != null) {
            return hovered;
        }
        if (FORCED != null && !"true".equalsIgnoreCase(FORCED)) {
            String needle = FORCED.toLowerCase();
            for (EntityRLHuman npc : npcs) {
                String name = npc.getName() == null ? "" : npc.getName().toLowerCase();
                String uid = npc.get_uid() == null ? "" : npc.get_uid().toLowerCase();
                if (name.contains(needle) || uid.contains(needle)) {
                    return npc;
                }
            }
        }
        return FORCED == null ? null : nearestToPlayer(npcs);
    }

    /**
     * Hit-test against the box the sprite was actually drawn into, not the tile under the
     * cursor: a person is a 2:3 box bottom-aligned to their cell, so the head of anyone in
     * row j hangs over row j-1 and picking by tile misses them by a whole row.
     */
    private static EntityRLHuman hovered(List<EntityRLHuman> npcs) {
        float mx = Mouse.getX() + WorldViewCamera.camera_x;
        float my = (WindowRender.get_window_h() - Mouse.getY()) + WorldViewCamera.camera_y;
        EntityRLHuman best = null;
        for (EntityRLHuman npc : npcs) {
            float x0 = Grid.cellX(npc.x());
            float y0 = Grid.boxTop(npc.y());
            if (mx >= x0 && mx <= x0 + RenderConfig.CELL
                    && my >= y0 && my <= Grid.boxBottom(npc.y())) {
                best = npc;   // list is sorted back to front, so the last hit is the front one
            }
        }
        return best;
    }

    private static EntityRLHuman nearestToPlayer(List<EntityRLHuman> npcs) {
        Point player = Player.get_origin();
        if (player == null) {
            return npcs.isEmpty() ? null : npcs.get(0);
        }
        EntityRLHuman best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (EntityRLHuman npc : npcs) {
            int distance = distance(npc.origin, player);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = npc;
            }
        }
        return best;
    }

    // ----------------------------------------------------------------- helpers

    private static TownAI brainOf(EntityRLHuman npc) {
        if (npc == null || !(npc.getAI() instanceof TownAI)) {
            return null;
        }
        return (TownAI) npc.getAI();
    }

    /** Resolve a uid we hold a belief about to a readable name, or a stub of the uid. */
    private static String nameOf(TownAI brain, String uid) {
        if (uid == null) {
            return "-";
        }
        Entity ent = brain.entity(uid);
        if (ent == Player.get_ent() && ent != null) {
            return ent.getName() + " (you)";
        }
        return ent == null || ent.getName() == null ? shortUid(uid) : ent.getName();
    }

    private static String shortName(EntityRLHuman npc) {
        String name = npc.getName();
        if (name == null || name.isEmpty()) {
            return shortUid(npc.get_uid());
        }
        int space = name.indexOf(' ');
        return space > 0 ? name.substring(0, space) : name;
    }

    /** A UUID is not an identity you can read. Enough of it to tell two NPCs apart. */
    private static String shortUid(String uid) {
        if (uid == null) {
            return "?";
        }
        return uid.length() <= 8 ? uid : uid.substring(0, 8);
    }

    /** "ai_state_GOING_HOME" -> "GOING_HOME". */
    private static String stateLabel(String state) {
        if (state == null) {
            return "none";
        }
        return state.startsWith("ai_state_") ? state.substring("ai_state_".length()) : state;
    }

    private static Color stateColor(TownAI brain) {
        String state = stateLabel(brain.getState());
        if (state.equals("FLEEING") || state.equals("PURSUING")) {
            return HOT;
        }
        if (state.equals("INVESTIGATING") || state.equals("DELIBERATE")) {
            return WARM;
        }
        if (state.equals("SLEEPING")) {
            return DIM;
        }
        return VALUE;
    }

    private static Color nameColor(EntityRLHuman npc, TownAI brain) {
        if (npc.get_combat() != null && !npc.get_combat().is_alive()) {
            return DIM;
        }
        return brain != null && brain.getClass().getSimpleName().startsWith("Police")
                ? POLICE : VALUE;
    }

    private static int distance(Point a, Point b) {
        if (a == null || b == null) {
            return 0;
        }
        int dx = a.getX() - b.getX();
        int dy = a.getY() - b.getY();
        return (int) Math.sqrt(dx * dx + dy * dy);
    }

    private static String percentage(float value) {
        return String.valueOf((int) (value * 100)) + "%";
    }

    /** "-" for a trigger the walk never reached: it was not asked, which is not a "no". */
    private static String verdict(AI.ImpulseView.Verdict value) {
        if (value == AI.ImpulseView.Verdict.YES) {
            return "yes";
        }
        return value == AI.ImpulseView.Verdict.NO ? "no" : "-";
    }

    private static String yn(boolean value) {
        return value ? "yes" : "no";
    }

    private static String pad(String text, int width) {
        return Panel.pad(text, width);
    }

    private static String join(List<String> parts, String separator) {
        return Panel.join(parts, separator);
    }

}
