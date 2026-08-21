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
import com.nuclearunicorn.serialkiller.game.character.CharacterSetup;
import com.nuclearunicorn.serialkiller.game.world.RLWorldModel;
import com.nuclearunicorn.serialkiller.generators.Apartment;
import com.nuclearunicorn.serialkiller.generators.town.Building;
import com.nuclearunicorn.serialkiller.generators.town.Room;
import com.nuclearunicorn.serialkiller.render.AsciiEntRenderer;
import com.nuclearunicorn.serialkiller.game.world.RLTile;
import com.nuclearunicorn.serialkiller.game.world.entities.EntityBed;
import com.nuclearunicorn.serialkiller.game.world.entities.EntityDoor;
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
        if (DebugFlags.dumpMapAtReady()) {
            dumpMap();
        }
        if (DebugFlags.dumpTownAtReady()) {
            dumpTown();
        }
    }

    /**
     * What this town was built out of, and where it put the player.
     *
     * <p>The building mix is rolled per chunk under caps (at most one brothel, at least 60%
     * apartments) and filtered by which templates fit the lots the splitter happened to cut,
     * so a town with no shop in it at all is a normal outcome and not a bug. Which matters
     * the moment a character preset asks to start behind a counter: without this you cannot
     * tell "the spawn lookup is broken" from "this town has no shops", and the two want
     * opposite fixes.
     */
    public static void dumpTown() {
        RLWorldModel model = (RLWorldModel) ClientGameEnvironment.getEnvironment().getWorld();

        Map<String,Integer> buildings = new TreeMap<String,Integer>();
        Map<String,Integer> rooms = new TreeMap<String,Integer>();

        for (Apartment apt : model.getApartments()) {
            String type = (apt instanceof Building)
                    ? ((Building) apt).type.toString() : "APARTMENT(legacy)";
            tally(buildings, type);

            if (!(apt instanceof Building)) {
                continue;
            }
            for (Room room : ((Building) apt).roomList) {
                tally(rooms, type + "." + (room.type == null ? "untyped" : room.type.toString()));
            }
        }

        System.err.println("DEBUG-TOWN buildings " + buildings);
        System.err.println("DEBUG-TOWN rooms " + rooms);
        System.err.println("DEBUG-TOWN preset " + CharacterSetup.current().getId()
                + " wanted " + CharacterSetup.current().getSpawn()
                + ", home " + RLWorldModel.playerSafeHouseLocation
                + ", spawn " + RLWorldModel.playerSpawnLocation);
    }

    private static void tally(Map<String,Integer> counts, String key) {
        Integer seen = counts.get(key);
        counts.put(key, seen == null ? 1 : seen + 1);
    }

    /**
     * Print the finished town as ASCII, one line per row. A connectivity count says a town is
     * wrong; only the picture says <i>how</i> — a room ringed with doors or a house of nothing
     * but beds reads at a glance here and is invisible in any tally.
     */
    public static void dumpMap() {
        WorldLayer layer = ClientGameEnvironment.getWorldLayer(Player.get_zindex());
        int size = WorldCluster.CLUSTER_SIZE * WorldChunk.CHUNK_SIZE;

        StringBuilder out = new StringBuilder();
        for (int y = 0; y < size; y++) {
            StringBuilder row = new StringBuilder();
            for (int x = 0; x < size; x++) {
                row.append(glyph(tileAt(layer, x, y)));
            }
            out.append("DEBUG-MAP ").append(row).append('\n');
        }
        System.err.print(out);
    }

    /** One character per tile: the topmost entity's own render symbol, else wall/floor. */
    private static char glyph(RLTile tile) {
        if (tile == null) {
            return ' ';
        }
        for (int i = tile.ent_list.size() - 1; i >= 0; i--) {
            Entity ent = tile.ent_list.get(i);
            if (ent.get_render() instanceof AsciiEntRenderer) {
                String s = ((AsciiEntRenderer) ent.get_render()).symbol;
                if (s != null && s.length() > 0) {
                    return s.charAt(0);
                }
            }
        }
        if (tile.isWall()) { return '#'; }
        return tile.isIndoor() ? '.' : ',';
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
                //a door is an opening, shut or locked - "a door opens, there is nothing to
                //break". The question here is whether the generator walled something off in
                //masonry or furniture; a locked bank is locked, and that is not the same bug
                open[x][y] = tile != null
                        && (!tile.isPathBlocked() || tile.has_ent(EntityDoor.class));
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
                //a bare count says someone is stuck but not who or where, which is the half
                //that costs an afternoon: name them, and the tile usually explains itself
                if (!main) {
                    System.err.println("DEBUG-WORLD stranded " + ent.getName() + " at "
                            + ent.origin.getX() + "," + ent.origin.getY()
                            + " component=" + component[ent.origin.getX()][ent.origin.getY()]);
                }
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
