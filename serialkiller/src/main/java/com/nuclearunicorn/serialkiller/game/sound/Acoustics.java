package com.nuclearunicorn.serialkiller.game.sound;

import com.nuclearunicorn.libroguelike.core.client.ClientGameEnvironment;
import com.nuclearunicorn.libroguelike.game.ai.AI;
import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.game.world.WorldTile;
import com.nuclearunicorn.libroguelike.game.world.layers.WorldLayer;
import com.nuclearunicorn.serialkiller.game.ai.behavior.SleepAction;
import com.nuclearunicorn.serialkiller.game.world.RLTile;
import com.nuclearunicorn.serialkiller.game.world.entities.EntityRLHuman;
import com.nuclearunicorn.serialkiller.render.RLMessages;
import org.lwjgl.util.Point;
import org.newdawn.slick.Color;

/**
 * Sound propagation: a bucket-queue Dijkstra flood over the tile grid (SOUND_DESIGN.md 5).
 *
 * <p>The old model was a Euclidean radius, so a policeman heard a beating through a brick
 * wall exactly as well as across an open street. This one floods <i>connected space</i>: the
 * wave goes wherever it can get, paying air for distance and a transmission loss for every
 * tile it enters. Walls are not binary — they are just very expensive — so a gunshot punches
 * through one and a conversation does not, which is the behaviour we actually wanted and
 * could not express before.
 *
 * <p>Diffraction is the reason this is a flood and not a line-of-sight sweep like
 * {@link com.nuclearunicorn.serialkiller.render.LightMap}. Light does not bend round corners;
 * sound does, and an NPC who cannot hear you around a bend in an open corridor is a worse bug
 * than the one being fixed.
 *
 * <h2>Why the arithmetic is linear</h2>
 * Real spreading loss is {@code 20*log10(d)}. Using it here would make the search state
 * two-dimensional — {@code (distance, accumulated transmission loss)} — because a longer path
 * through less wall can beat a shorter path through more, and a scalar priority over a 2-D
 * state is a resource-constrained shortest path, which plain Dijkstra does not solve
 * optimally. With a constant dB-per-tile the priority key <i>is</i> the state, the algorithm
 * is textbook, every edge weight is a small integer (so the queue is buckets, not a heap),
 * and the numbers stay legible enough to tune by hand.
 */
public final class Acoustics {

    private Acoustics() {}

    /** Neighbour offsets, N clockwise to NW. Even indices are cardinal, odd are diagonal. */
    public static final int[] DX = { 0,  1, 1, 1, 0, -1, -1, -1};
    public static final int[] DY = {-1, -1, 0, 1, 1,  1,  0, -1};

    private static final String[] BEARINGS = {
        "north", "north-east", "east", "south-east",
        "south", "south-west", "west", "north-west"
    };

    /** The most recent field, kept for the debug overlay only. */
    private static SoundField last;

    public static SoundField lastField() {
        return last;
    }

    public static void forgetLastField() {
        last = null;
    }

    /** Compass word for a direction index, as stored in {@link SoundField#directionAt}. */
    public static String bearing(int dir) {
        return dir < 0 || dir > 7 ? "somewhere close" : BEARINGS[dir];
    }

    // ------------------------------------------------------------------ flood

    public static SoundField propagate(Point origin, int loudness, int layerId) {
        return propagate(ClientGameEnvironment.getWorldLayer(layerId),
                origin.getX(), origin.getY(), loudness, layerId);
    }

    /**
     * Flood outward from a source until nothing audible is left.
     *
     * @return the field, or null when the sound is too quiet to be heard anywhere at all
     */
    public static SoundField propagate(WorldLayer layer, int ox, int oy, int loudness,
                                       int layerId) {
        int budget = loudness - SoundConfig.FLOOR;
        if (layer == null || budget < 0) {
            // A whisper across a room, or a sneaking footstep, never allocates anything.
            return null;
        }

        int maxRange = budget / SoundConfig.AIR_CARD;
        int size = 2 * maxRange + 1;
        int x0 = ox - maxRange;
        int y0 = oy - maxRange;

        int[] loss = new int[size * size];
        byte[] dir = new byte[size * size];
        for (int i = 0; i < loss.length; i++) {
            loss[i] = SoundField.INAUDIBLE;
            dir[i] = -1;
        }

        // Dial's algorithm: one bucket per possible cost. Legal because every edge weight is
        // a small non-negative integer, and O(cells + budget) with no heap and no boxing.
        int[][] bucket = new int[budget + 1][];
        int[] bucketCount = new int[budget + 1];

        int src = maxRange * size + maxRange;
        loss[src] = 0;
        bucket[0] = new int[8];
        bucket[0][0] = src;
        bucketCount[0] = 1;

        for (int cost = 0; cost <= budget; cost++) {
            int[] b = bucket[cost];
            if (b == null) {
                continue;
            }
            // Safe to read the count once: every relaxation costs at least AIR_CARD, so a
            // push always lands in a strictly higher bucket and never grows this one.
            int n = bucketCount[cost];
            for (int i = 0; i < n; i++) {
                int cell = b[i];
                if (loss[cell] != cost) {
                    continue;   // superseded by a cheaper path found later; lazy deletion
                }
                int cx = x0 + (cell % size);
                int cy = y0 + (cell / size);

                for (int k = 0; k < 8; k++) {
                    int nx = cx + DX[k];
                    int ny = cy + DY[k];
                    int lx = nx - x0;
                    int ly = ny - y0;
                    if (lx < 0 || ly < 0 || lx >= size || ly >= size) {
                        continue;
                    }
                    int tl = tileLoss(layer, nx, ny);
                    if (tl < 0) {
                        continue;   // no tile there: unloaded chunk, treat as sealed
                    }
                    int next = cost + ((k & 1) == 0 ? SoundConfig.AIR_CARD
                                                    : SoundConfig.AIR_DIAG) + tl;
                    if (next > budget) {
                        continue;
                    }
                    int ncell = ly * size + lx;
                    if (next >= loss[ncell]) {
                        continue;
                    }
                    loss[ncell] = next;
                    // store the way back toward the source, not the way we were going
                    dir[ncell] = (byte) ((k + 4) & 7);

                    if (bucket[next] == null) {
                        bucket[next] = new int[8];
                    } else if (bucketCount[next] == bucket[next].length) {
                        int[] grown = new int[bucket[next].length * 2];
                        System.arraycopy(bucket[next], 0, grown, 0, bucket[next].length);
                        bucket[next] = grown;
                    }
                    bucket[next][bucketCount[next]++] = ncell;
                }
            }
        }

        return new SoundField(loudness, ox, oy, layerId, x0, y0, size, loss, dir);
    }

    /** Transmission loss of entering a tile, or -1 if there is no tile there. */
    private static int tileLoss(WorldLayer layer, int x, int y) {
        WorldTile tile = layer.get_tile(x, y);
        if (tile == null) {
            return -1;
        }
        return tile instanceof RLTile ? ((RLTile) tile).getSoundLoss() : SoundConfig.TL_OPEN;
    }

    // --------------------------------------------------------------- delivery

    /**
     * Flood, then hand a {@link SoundHeard} to everyone the field says can hear it.
     *
     * <p>Replaces {@code SocialController}'s radius sweep. Note that the listener is given
     * their own received level and arrival direction rather than the raw origin — they learn
     * that something happened over there, not where it happened.
     */
    public static void emit(SoundEvent sound) {
        SoundField field = propagate(sound.getOrigin(), sound.loudness, sound.layerId);
        if (field == null) {
            return;
        }
        last = field;

        WorldLayer layer = ClientGameEnvironment.getWorldLayer(sound.layerId);
        Entity[] ents = ClientGameEnvironment.getEnvironment()
                .getEntityManager().getEntities(sound.layerId);

        for (int i = 0; i < ents.length; i++) {
            Entity ent = ents[i];
            if (ent == null || ent == sound.source || ent.origin == null) {
                continue;
            }
            if (!(ent instanceof EntityRLHuman)) {
                continue;
            }
            int received = field.received(ent.x(), ent.y());
            if (received == Integer.MIN_VALUE) {
                continue;
            }
            RLTile tile = tileAt(layer, ent.x(), ent.y());
            if (received <= Ambient.threshold(tile, threshold(ent))) {
                continue;
            }

            SoundHeard heard = new SoundHeard(sound, received,
                    field.directionAt(ent.x(), ent.y()));

            if (ent.isPlayerEnt()) {
                tellPlayer(heard);
                continue;
            }
            AI ai = ent.getAI();
            if (ai != null) {
                ai.e_on_event(heard);
            }
        }
    }

    private static RLTile tileAt(WorldLayer layer, int x, int y) {
        if (layer == null) {
            return null;
        }
        WorldTile tile = layer.get_tile(x, y);
        return tile instanceof RLTile ? (RLTile) tile : null;
    }

    /**
     * How loud something has to be before this person notices it.
     *
     * <p>Only states we can actually read are applied. A sleeper is the interesting one: it
     * is what makes a night-time break-in a different problem from a daytime one.
     */
    public static int threshold(Entity ent) {
        int threshold = SoundConfig.HEAR_BASE;
        AI ai = ent.getAI();
        if (ai != null && SleepAction.STATE.equals(ai.getState())) {
            threshold += SoundConfig.HEAR_ASLEEP;
        }
        if (ent instanceof EntityRLHuman) {
            EntityRLHuman human = (EntityRLHuman) ent;
            if (human.getBodysim() != null
                    && (human.getBodysim().isFainted() || human.getBodysim().isStunned())) {
                threshold += SoundConfig.HEAR_STUNNED;
            }
        }
        return threshold;
    }

    /** The player's own ears. Only things worth turning your head for get a line. */
    private static void tellPlayer(SoundHeard heard) {
        if (!heard.kind().isSuspicious()) {
            return;
        }
        String how = heard.muffling() > 0.6f ? ", muffled"
                : (heard.muffling() < 0.2f ? ", close by" : "");
        RLMessages.message("You hear " + describe(heard.kind()) + " to the "
                + heard.bearing() + how, new Color(200, 200, 120));
    }

    private static String describe(SoundKind kind) {
        switch (kind) {
            case SCREAM:      return "a scream";
            case GUNSHOT:     return "a gunshot";
            case GLASS_BREAK: return "breaking glass";
            case BONE_BREAK:  return "something snap";
            case BODY_FALL:   return "something heavy fall";
            case DOOR_KICK:   return "a door give way";
            case KNIFE:       return "a scuffle";
            case PUNCH:       return "a scuffle";
            default:          return "a noise";
        }
    }
}
