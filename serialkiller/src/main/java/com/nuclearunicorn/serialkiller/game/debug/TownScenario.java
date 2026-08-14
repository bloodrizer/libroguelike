package com.nuclearunicorn.serialkiller.game.debug;

import com.nuclearunicorn.libroguelike.core.client.ClientGameEnvironment;
import com.nuclearunicorn.libroguelike.core.replay.Scenario;
import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.game.ent.EntityActor;
import com.nuclearunicorn.libroguelike.game.player.Player;
import com.nuclearunicorn.libroguelike.game.world.WorldTimer;
import com.nuclearunicorn.serialkiller.game.ai.PedestrianAI;
import com.nuclearunicorn.serialkiller.game.ai.PoliceAI;
import com.nuclearunicorn.libroguelike.utils.Rng;
import com.nuclearunicorn.serialkiller.game.combat.RLCombat;
import com.nuclearunicorn.serialkiller.game.controllers.RLController;
import com.nuclearunicorn.serialkiller.render.AsciiEntRenderer;
import com.nuclearunicorn.serialkiller.game.world.entities.EntityRLHuman;
import com.nuclearunicorn.serialkiller.generators.NPCGenerator;
import org.lwjgl.util.Point;

import java.util.Calendar;

/**
 * The scenario verbs, in the module that knows what a policeman is.
 *
 * <pre>
 *   tp &lt;x&gt; &lt;y&gt;              put the player here
 *   hurt &lt;who&gt; [times]      the player hits them; "who" is nearest, a name, or a uid prefix
 *   say &lt;words...&gt;          the player speaks aloud, as pressing 't' does
 *   spawn &lt;kind&gt; &lt;x&gt; &lt;y&gt;   kind is pedestrian|police
 *   settime &lt;hour&gt;          0-23, so daytime is testable without waiting out the night
 *   tick [n]                advance n turns
 * </pre>
 *
 * <p>{@code hurt} is the one that earns its keep. Panic, flight, the police report and the
 * whole crime pipeline hang off one NPC being struck by the player, and reaching that state
 * through the keyboard means navigating to someone and connecting — which, scripted blind,
 * mostly means swinging at empty air. Here it is one line and it is the player who lands it,
 * so the victim blames the right person.
 */
public class TownScenario implements Scenario {

    /** Called once the world exists. Wiring it here keeps the game mode free of the details. */
    public static void install() {
        Scenario.register(new TownScenario());
    }

    @Override
    public boolean run(String verb, String[] args) {
        if ("tp".equals(verb)) {
            teleport(intArg(args, 0), intArg(args, 1));
            return true;
        }
        if ("hurt".equals(verb)) {
            hurt(args.length > 0 ? args[0] : "nearest",
                 args.length > 1 ? Integer.parseInt(args[1]) : 1);
            return true;
        }
        if ("spawn".equals(verb)) {
            spawn(args.length > 0 ? args[0] : "pedestrian", intArg(args, 1), intArg(args, 2));
            return true;
        }
        if ("say".equals(verb)) {
            say(String.join(" ", args));
            return true;
        }
        if ("settime".equals(verb)) {
            WorldTimer.datetime.set(Calendar.HOUR_OF_DAY, intArg(args, 0));
            WorldTimer.datetime.set(Calendar.MINUTE, 0);
            return true;
        }
        if ("tick".equals(verb)) {
            int n = args.length > 0 ? Integer.parseInt(args[0]) : 1;
            for (int i = 0; i < n; i++) {
                TurnPump.advance();
            }
            return true;
        }
        return false;
    }

    private void teleport(int x, int y) {
        Player.get_ent().move_to(new Point(x, y));
    }

    /**
     * Land a blow on someone, attributed to the player. Deliberately routed through the same
     * {@code inflict_damage} a real swing uses, so the pain sensor fires and the victim ends
     * up believing the player did it — a scenario that damaged them quietly would test the
     * health bar and nothing else.
     */
    private void hurt(String who, int times) {
        Entity target = find(who);
        if (target == null) {
            throw new IllegalArgumentException("no such entity: " + who);
        }
        for (int i = 0; i < times; i++) {
            Player.get_ent().get_combat().inflict_damage(target);
        }
    }

    /**
     * The player says something out loud, exactly as pressing {@code t} does.
     *
     * <p>Worth a verb of its own: "walk up to someone and talk to them" is the single most
     * common thing to want to test and the hardest to script through the keyboard, because
     * it needs a text buffer typed one keycode at a time and an NPC who has not wandered off
     * by the time you finish. It is also the path that was silently dropping every line the
     * player said, which nothing could reproduce until this existed.
     */
    private void say(String text) {
        ((EntityActor) Player.get_ent()).say_message(text);
        TurnPump.advance();     //speaking costs a turn, so the listener gets to think
    }

    /** Mirrors the police spawn in TownChunkGenerator - brain, body, controller, then place. */
    private void spawn(String kind, int x, int y) {
        boolean police = "police".equalsIgnoreCase(kind);
        EntityRLHuman npc = new EntityRLHuman();
        npc.setName(police ? "Policeman" : "NPC");
        npc.setEnvironment(ClientGameEnvironment.getEnvironment());
        npc.setRenderer(new AsciiEntRenderer(police ? "P" : "@"));
        npc.set_blocking(true);
        npc.setLayerId(Player.get_zindex());
        npc.spawn(new Point(x, y));

        npc.set_combat(new RLCombat());
        npc.set_ai(police ? new PoliceAI() : new PedestrianAI());
        npc.set_controller(new RLController());
        NPCGenerator.generateNPCStats(Rng.derive(Rng.WORLDGEN), npc);
        if (police) {
            npc.setName("Policeman");
        }
    }

    /** "nearest", a uid (or a unique prefix of one), or a name — whichever the scenario used. */
    private Entity find(String who) {
        if ("nearest".equalsIgnoreCase(who)) {
            Entity best = null;
            int bestDistance = Integer.MAX_VALUE;
            Point from = Player.get_ent().origin;
            for (Entity ent : ClientGameEnvironment.getEnvironment()
                    .getEntityManager().getList(Player.get_zindex())) {
                if (!(ent instanceof EntityRLHuman) || ent == Player.get_ent()) {
                    continue;
                }
                int d = Math.abs(ent.origin.getX() - from.getX())
                      + Math.abs(ent.origin.getY() - from.getY());
                if (d < bestDistance) {
                    bestDistance = d;
                    best = ent;
                }
            }
            return best;
        }
        for (Entity ent : ClientGameEnvironment.getEnvironment()
                .getEntityManager().getList(Player.get_zindex())) {
            if (ent.get_uid() != null && ent.get_uid().startsWith(who)) {
                return ent;
            }
            if (who.equalsIgnoreCase(ent.getName())) {
                return ent;
            }
        }
        return null;
    }

    private static int intArg(String[] args, int at) {
        if (at >= args.length) {
            throw new IllegalArgumentException("missing argument " + (at + 1));
        }
        return Integer.parseInt(args[at]);
    }

    /** Set by the game mode so {@code tick} can reach the real turn, not a copy of it. */
    public static final class TurnPump {
        private static Runnable advance;
        private TurnPump() {}
        public static void bind(Runnable r) {
            advance = r;
        }
        static void advance() {
            if (advance != null) {
                advance.run();
            }
        }
    }
}
