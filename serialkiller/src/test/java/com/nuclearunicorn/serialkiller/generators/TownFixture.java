package com.nuclearunicorn.serialkiller.generators;

import com.nuclearunicorn.libroguelike.core.client.ClientGameEnvironment;
import com.nuclearunicorn.libroguelike.events.EventManager;
import com.nuclearunicorn.libroguelike.game.GameEnvironment;
import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.game.player.Player;
import com.nuclearunicorn.libroguelike.game.world.WorldChunk;
import com.nuclearunicorn.libroguelike.game.world.WorldCluster;
import com.nuclearunicorn.libroguelike.game.world.layers.WorldLayer;
import com.nuclearunicorn.libroguelike.utils.Rng;
import com.nuclearunicorn.libroguelike.utils.Timer;
import com.nuclearunicorn.serialkiller.game.world.RLTile;
import com.nuclearunicorn.serialkiller.game.world.RLWorldModel;
import com.nuclearunicorn.serialkiller.generators.layerGenerators.TownChunkGenerator;
import com.nuclearunicorn.serialkiller.render.RLMessages;
import com.nuclearunicorn.serialkiller.utils.pathfinder.adaptive.AdaptivePathfinder;
import org.lwjgl.util.Point;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds a whole town in a unit test, with no window, no game loop and no player input.
 *
 * <p>The world-gen invariants are properties of a <i>finished</i> town — "no crate in this
 * doorway", "no wall made of doors" — and there is no smaller thing to test them on. A
 * hand-built fixture grid would only assert that the assertion works; the defects worth
 * catching all come from placement rules interacting across a full chunk, which is exactly
 * what a synthetic room cannot reproduce. So the generator is run for real.
 *
 * <p>What that needs turns out to be small: an environment with an event manager, an
 * {@link RLWorldModel} to hold the layers, and a one-chunk cluster. Everything the generator
 * touches beyond that — renderers, items, AI brains — is plain object construction. Nothing
 * here opens a GL context.
 */
public final class TownFixture {

    private TownFixture() {}

    /** A generated chunk, and the queries the invariant checks need to ask of it. */
    public static final class Town {
        public final long seed;
        public final WorldLayer layer;
        public final int size;
        public final List<Entity> entities;
        /** Where the generator wants the player put, copied out of the static it lands in. */
        public final Point playerStart;

        Town(long seed, WorldLayer layer, int size, List<Entity> entities, Point playerStart) {
            this.seed = seed;
            this.layer = layer;
            this.size = size;
            this.entities = entities;
            this.playerStart = playerStart;
        }

        public RLTile tile(int x, int y) {
            if (x < 0 || y < 0 || x >= size || y >= size) {
                return null;
            }
            return (RLTile) layer.get_tile(x, y);
        }

        /** True if a door or window occupies this tile — a hole in a wall, either way. */
        public boolean isGap(int x, int y) {
            RLTile tile = tile(x, y);
            return tile != null && tile.isWallGap();
        }

        /** Chunk-local coordinates of everything on the map, for "where did this go wrong". */
        public String describe(int x, int y) {
            RLTile tile = tile(x, y);
            if (tile == null) {
                return "(" + x + "," + y + ") off-map";
            }
            StringBuilder names = new StringBuilder();
            for (Entity ent : tile.ent_list) {
                names.append(names.length() == 0 ? "" : "+").append(ent.getName());
            }
            return "(" + x + "," + y + ") " + (tile.isWall() ? "wall " : "")
                    + (tile.isWallGap() ? "gap " : "") + names;
        }
    }

    private static final Map<Long,Town> CACHE = new HashMap<Long,Town>();

    /**
     * The town for one seed, built once per JVM. Deterministic: the same seed gives the same
     * town, which is what makes a failure here reproducible in the game itself with
     * {@code -Dreplay.seed=<n>}.
     */
    public static synchronized Town town(long seed) {
        Town cached = CACHE.get(seed);
        if (cached == null) {
            cached = generate(seed);
            CACHE.put(seed, cached);
        }
        return cached;
    }

    /** Build a town from scratch. Prefer {@link #town} unless a fresh one is the point. */
    public static Town generate(long seed) {
        WorldChunk.CHUNK_SIZE = 128;
        WorldCluster.CLUSTER_SIZE = 1;
        WorldCluster.origin.setLocation(0, 0);
        Timer.init();
        Rng.seed(seed);
        //the nav graph is a static, and the generator only ever adds to it: without this every
        //town after the first is routed against the union of itself and its predecessors
        AdaptivePathfinder.reset();

        final EventManager events = new EventManager();
        GameEnvironment env = new GameEnvironment("town-fixture-" + seed) {
            @Override
            public EventManager getEventManager() {
                return events;
            }
        };
        //the generator spawns the player through InGameMode, which reads the client-side
        //environment; without this it would spawn into whichever town a previous test built
        ClientGameEnvironment.setEnvironment(env);
        RLMessages.setEventManager(events);

        RLWorldModel model = new RLWorldModel(1);
        env.setWorld(model);

        WorldLayer layer = model.getWorldLayer(0);
        layer.registerGenerator(new TownChunkGenerator());
        layer.get_cached_chunk(0, 0);   //lazily builds - and so generates - the chunk

        List<Entity> entities = new ArrayList<Entity>(
                env.getEntityManager().getList(Player.get_zindex()));
        //RLWorldModel keeps this in a static, so it belongs to whichever town was built last;
        //copy it now or a cached town answers with a neighbouring seed's front room
        Point start = RLWorldModel.playerSafeHouseLocation;
        return new Town(seed, layer, WorldChunk.CHUNK_SIZE, entities,
                start == null ? null : new Point(start));
    }

    /**
     * The seeds every invariant is checked against. One town is a sample, not a property:
     * every world-gen bug so far has been a rule that holds on most layouts and not on the
     * one in front of you, so a single seed reads as a clean bill of health right up until
     * someone plays a different game. The last is the seed off the replay that produced the
     * house full of beds and the wall of doors.
     */
    static long[] seeds() {
        return new long[]{1L, 7L, 42L, 1234L, 987654321L, 350429078576728L};
    }
}
