package com.nuclearunicorn.serialkiller.game.debug;

import com.nuclearunicorn.libroguelike.core.client.ClientGameEnvironment;
import com.nuclearunicorn.libroguelike.core.debug.DebugFlags;
import com.nuclearunicorn.libroguelike.core.replay.Replay;
import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.game.ent.controller.NpcController;
import com.nuclearunicorn.libroguelike.game.player.Player;
import com.nuclearunicorn.libroguelike.game.world.WorldChunk;
import com.nuclearunicorn.libroguelike.game.world.WorldCluster;
import com.nuclearunicorn.libroguelike.game.world.layers.WorldLayer;
import com.nuclearunicorn.serialkiller.game.ai.llm.sense.GameTurn;
import com.nuclearunicorn.serialkiller.game.world.RLTile;
import com.nuclearunicorn.serialkiller.game.world.entities.EntityBed;
import com.nuclearunicorn.serialkiller.game.world.entities.EntityRLHuman;
import org.lwjgl.util.Point;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Town-wide instrumentation, as opposed to the {@code npc} replay record, which only covers
 * humans within twelve tiles of the player.
 *
 * <p>That distinction is not academic. "Does this NPC react to me" and "does the town work"
 * are different questions, and answering the second with a near-player sample gives the wrong
 * answer confidently: a commute measured that way reads as broken whenever the player has
 * simply walked somewhere quiet.
 *
 * <ul>
 *   <li>{@link #dumpAtReady()} — is the town one connected place, and can everyone get to a
 *       bed? A generator that seals a room behind a fridge is invisible until an NPC needs to
 *       walk through it, three subsystems away from the cause.</li>
 *   <li>{@link #census()} — what is everybody doing, and are they getting anywhere? The
 *       telling number is <i>moved</i>: NPCs holding a route but not advancing along it are
 *       queued behind something that will never shift.</li>
 * </ul>
 */
public final class WorldProbe {

    private static final int[][] DIRS = {{1,0},{-1,0},{0,1},{0,-1}};

    private static long lastCensusTurn = -1;
    /** Where each NPC stood at the previous census, to tell walking from standing still. */
    private static final Map<String,String> lastSeenAt = new TreeMap<String,String>();

    private WorldProbe() {}

    /** Called once the world is built and playable. */
    public static void atReady() {
        if (DebugFlags.dumpWorldAtReady()) {
            dumpAtReady();
        }
    }

    /** Called every frame; does its work only on census turns. */
    public static void tick() {
        int every = DebugFlags.censusEvery();
        if (every <= 0) {
            return;
        }
        long turn = GameTurn.current();
        if (turn == lastCensusTurn || turn % every != 0) {
            return;
        }
        lastCensusTurn = turn;
        census();
    }

    /**
     * Flood the walkable map and report its connected components, then say which of them the
     * beds and the people are in. One component is the healthy answer.
     */
    public static void dumpAtReady() {
        WorldLayer layer = ClientGameEnvironment.getWorldLayer(Player.get_zindex());
        int size = WorldCluster.CLUSTER_SIZE * WorldChunk.CHUNK_SIZE;

        boolean[][] open = new boolean[size][size];
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                RLTile tile = tileAt(layer, x, y);
                open[x][y] = tile != null && !tile.isPathBlocked();
            }
        }

        int[][] component = new int[size][size];
        List<Integer> sizes = new ArrayList<Integer>();
        sizes.add(0);   //component ids start at 1
        int biggest = 0;
        int biggestId = 0;
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                if (!open[x][y] || component[x][y] != 0) {
                    continue;
                }
                int id = sizes.size();
                int filled = flood(open, component, x, y, id, size);
                sizes.add(filled);
                if (filled > biggest) {
                    biggest = filled;
                    biggestId = id;
                }
            }
        }

        int beds = 0, bedsReachable = 0, people = 0, peopleReachable = 0;
        for (Entity ent : ClientGameEnvironment.getEnvironment()
                .getEntityManager().getList(Player.get_zindex())) {
            if (ent.origin == null || !inBounds(ent.origin, size)) {
                continue;
            }
            boolean main = component[ent.origin.getX()][ent.origin.getY()] == biggestId;
            if (ent instanceof EntityBed) {
                beds++;
                if (main) { bedsReachable++; }
            } else if (ent instanceof EntityRLHuman) {
                people++;
                if (main) { peopleReachable++; }
            }
        }

        String summary = "components=" + (sizes.size() - 1) + " largest=" + biggest
                + " beds=" + bedsReachable + "/" + beds
                + " people=" + peopleReachable + "/" + people;
        System.err.println("DEBUG-WORLD " + summary);
        Replay.observe("world", "components", sizes.size() - 1, "largest", biggest,
                "bedsReachable", bedsReachable, "beds", beds,
                "peopleReachable", peopleReachable, "people", people);

        if (sizes.size() - 1 > 1) {
            DebugFlags.violation("world-split",
                    "the town is not one place - " + summary + "; a route into a sealed room"
                    + " cannot be found and the NPC who lives there will never get home");
        }
    }

    /** What everyone is doing, and how many of them are actually getting anywhere. */
    public static void census() {
        Map<String,Integer> states = new TreeMap<String,Integer>();
        int inBed = 0, housed = 0, routed = 0, moving = 0, people = 0;

        for (Entity ent : ClientGameEnvironment.getEnvironment()
                .getEntityManager().getList(Player.get_zindex())) {
            if (!(ent instanceof EntityRLHuman) || ent.getAI() == null) {
                continue;
            }
            EntityRLHuman human = (EntityRLHuman) ent;
            people++;
            String state = String.valueOf(human.getAI().getState());
            states.put(state, states.containsKey(state) ? states.get(state) + 1 : 1);

            if (human.getApartment() != null) { housed++; }
            if (human.tile != null && human.tile.has_ent(EntityBed.class)) { inBed++; }

            if (ent.controller instanceof NpcController && ((NpcController) ent.controller).hasPath()) {
                routed++;
                String now = ent.origin.getX() + "," + ent.origin.getY();
                String before = lastSeenAt.get(ent.get_uid());
                if (before != null && !before.equals(now)) { moving++; }
            }
            lastSeenAt.put(ent.get_uid(), ent.origin.getX() + "," + ent.origin.getY());
        }

        long turn = GameTurn.current();
        System.err.println("DEBUG-CENSUS turn=" + turn + " people=" + people
                + " inBed=" + inBed + "/" + housed
                + " routed=" + routed + " moving=" + moving + " " + states);
        Replay.observe("census", "turn", turn, "people", people, "inBed", inBed,
                "housed", housed, "routed", routed, "moving", moving,
                "states", states.toString());
    }

    private static int flood(boolean[][] open, int[][] component, int sx, int sy, int id, int size) {
        Deque<int[]> queue = new ArrayDeque<int[]>();
        queue.add(new int[]{sx, sy});
        component[sx][sy] = id;
        int filled = 0;
        while (!queue.isEmpty()) {
            int[] at = queue.poll();
            filled++;
            for (int[] d : DIRS) {
                int nx = at[0] + d[0];
                int ny = at[1] + d[1];
                if (nx < 0 || ny < 0 || nx >= size || ny >= size) { continue; }
                if (!open[nx][ny] || component[nx][ny] != 0) { continue; }
                component[nx][ny] = id;
                queue.add(new int[]{nx, ny});
            }
        }
        return filled;
    }

    private static RLTile tileAt(WorldLayer layer, int x, int y) {
        return (RLTile) layer.get_tile(x, y);
    }

    private static boolean inBounds(Point p, int size) {
        return p.getX() >= 0 && p.getY() >= 0 && p.getX() < size && p.getY() < size;
    }
}
