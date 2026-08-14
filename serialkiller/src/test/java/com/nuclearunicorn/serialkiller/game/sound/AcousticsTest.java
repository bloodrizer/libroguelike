package com.nuclearunicorn.serialkiller.game.sound;

import com.nuclearunicorn.libroguelike.game.world.WorldTile;
import com.nuclearunicorn.libroguelike.game.world.layers.WorldLayer;
import com.nuclearunicorn.serialkiller.game.world.RLTile;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.Point;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The acoustic model checked against the numbers it was designed to produce
 * (SOUND_DESIGN.md 4.5, 7.1).
 *
 * <p>These assert exact dB values, not ranges, and that is the point. The cost tables were
 * chosen by working backwards from a dozen scenarios — "you can hear a conversation with your
 * ear against the door but not from across the landing" — and a table that is one dB out
 * silently deletes the mechanic. If retuning breaks one of these, the fix is to decide which
 * behaviour you want and re-derive the constant, not to relax the assertion.
 *
 * <p>Everything here asserts on {@code received()} rather than on "was it heard", which keeps
 * the geometry separable from ambient masking and time of day.
 */
class AcousticsTest {

    private static final int SILENT = Integer.MIN_VALUE;

    // ------------------------------------------------------------ 4.5 the table

    @Test
    void talkCarriesNineTilesInOpenAir() {
        SoundField f = talk(open(25, 5), 0, 2);
        // 32 - 2*9 = 14, clears the threshold of 12; one tile further is exactly 12, which
        // does not, because "heard" is strict. This is today's earshotRadius of 10, derived.
        assertEquals(14, f.received(9, 2));
        assertEquals(12, f.received(10, 2));
    }

    @Test
    void speechIsDirectedOnlyWithinThreeTiles() {
        SoundField f = talk(open(25, 5), 0, 2);
        assertTrue(f.received(3, 2) >= SoundConfig.DIRECTED_LEVEL);
        assertTrue(f.received(4, 2) < SoundConfig.DIRECTED_LEVEL);
    }

    @Test
    void interiorWallStopsConversationDead() {
        // a solid interior wall, no gap anywhere
        SoundField f = talk(barrier(25, 5, 1, -1, 0, SoundConfig.TL_WALL_INNER), 0, 2);
        assertEquals(SILENT, f.received(2, 2), "talk should not cross a wall at all");
    }

    @Test
    void eavesdropThroughShutDoor() {
        SoundField f = talk(doorway(SoundConfig.TL_DOOR_SHUT), 0, 2);
        // ear against the door: 32 - (2+14) - 2 = 14, audible
        assertEquals(14, f.received(2, 2));
        // one step back off it: exactly 12, inaudible. A one-tile knife edge, deliberately.
        assertEquals(12, f.received(3, 2));
    }

    /**
     * The regression: every door in town was baked as shut, so two people either side of an
     * open doorway could not hear each other. An open door is a hole in a wall.
     */
    @Test
    void conversationCarriesThroughAnOpenDoor() {
        SoundField f = talk(doorway(SoundConfig.TL_DOOR_OPEN), 0, 2);
        assertEquals(27, f.received(2, 2), "the next room over hears you clearly");
        assertEquals(13, f.received(9, 2), "and it stays audible seven tiles beyond");
        // the same wall with the door shut kills it inside two tiles - that is the mechanic
        SoundField shut = talk(doorway(SoundConfig.TL_DOOR_SHUT), 0, 2);
        assertEquals(14, shut.received(2, 2));
        assertEquals(SILENT, shut.received(9, 2));
    }

    @Test
    void screamPoursThroughAnOpenDoor() {
        SoundField f = field(doorway(SoundConfig.TL_DOOR_OPEN, 35), 0, 2, SoundKind.SCREAM);
        assertEquals(65, f.received(2, 2));
        // 27 street tiles of it - the whole road hears a murder through an open door
        assertEquals(13, f.received(28, 2));
        assertEquals(SILENT, f.received(29, 2));
    }

    @Test
    void screamGetsThroughAnExteriorWallButBarely() {
        SoundField f = field(wall(SoundConfig.TL_WALL_OUTER), 0, 2, SoundKind.SCREAM);
        assertEquals(20, f.received(2, 2), "70 - (2+46) - 2 at the first street tile");
        assertEquals(14, f.received(5, 2), "still audible three tiles further");
        assertEquals(12, f.received(6, 2), "and not beyond that");
    }

    @Test
    void gunshotPunchesThroughAnExteriorWall() {
        SoundField f = field(wall(SoundConfig.TL_WALL_OUTER), 0, 2, SoundKind.GUNSHOT);
        assertEquals(42, f.received(2, 2));
        assertEquals(14, f.received(16, 2), "fourteen tiles of it on the far side");
        assertEquals(12, f.received(17, 2));
    }

    @Test
    void walkingIsInaudibleThroughAShutDoor() {
        SoundField f = field(doorway(SoundConfig.TL_DOOR_SHUT), 0, 2, SoundKind.FOOTSTEP_WALK);
        // the door alone costs more than the whole budget: the flood never even enters it
        assertEquals(SILENT, f.received(1, 2));
        assertEquals(SILENT, f.received(2, 2));
    }

    @Test
    void sneakingAllocatesNothingAtAll() {
        // below FLOOR by construction, so this is an early-out and not an empty flood
        assertNull(field(open(9, 9), 4, 4, SoundKind.FOOTSTEP_SNEAK));
    }

    // -------------------------------------------------------------- diffraction

    /**
     * The test that fails if anyone reimplements this on top of shadowcasting.
     *
     * <p>An L-shaped corridor puts a wall on the straight line between speaker and listener,
     * so a line-of-sight model hears nothing. Sound bends; a flood over connected space bends
     * with it for free, and this is most of what "can he hear me through the doorway" means.
     */
    @Test
    void soundBendsRoundACorner() {
        WorldLayer l = parse(
                "#####",
                "#S..#",
                "###.#",
                "###L#",
                "#####");
        SoundField f = field(l, 1, 1, SoundKind.SHOUT);
        assertEquals(48, f.received(3, 3),
                "a shout must turn the corner of an open corridor");

        // and it genuinely went round rather than through: (2,2) is solid wall, and the
        // route the field records for the listener does not touch it. The wall tile is
        // still reachable — sound does enter masonry, just 29dB down — so the check that
        // means anything is which way the wavefront actually came.
        assertEquals(20, f.received(2, 2));
        assertTrue(!contains(f.pathToSource(3, 3), 2, 2),
                "the cheap path is the corridor, not the wall");
    }

    // ----------------------------------------------------------- direction field

    @Test
    void directionFieldLeadsBackThroughTheDoor() {
        WorldLayer l = parse(
                "######",
                "#..S.#",
                "##+###",
                "......",
                "......");
        SoundField f = field(l, 3, 1, SoundKind.SCREAM);

        List<Point> path = f.pathToSource(2, 4);
        assertNotNull(path, "the scream should reach the street");
        assertTrue(contains(path, 2, 2),
                "an investigator following the field must route through the doorway, "
                        + "not at the wall the scream happened behind");
        Point end = path.get(path.size() - 1);
        assertEquals(3, end.getX());
        assertEquals(1, end.getY());
    }

    @Test
    void receivedNeverRisesAlongTheDirectionField() {
        WorldLayer l = parse(
                "######",
                "#..S.#",
                "##+###",
                "......",
                "......");
        SoundField f = field(l, 3, 1, SoundKind.SCREAM);

        for (int y = f.y0; y < f.y0 + f.size; y++) {
            for (int x = f.x0; x < f.x0 + f.size; x++) {
                int here = f.received(x, y);
                if (here == SILENT) {
                    continue;
                }
                int d = f.directionAt(x, y);
                if (d < 0) {
                    continue;
                }
                int back = f.received(x + Acoustics.DX[d], y + Acoustics.DY[d]);
                assertTrue(back >= here,
                        "walking toward the source must never get quieter, at " + x + "," + y);
            }
        }
    }

    // ---------------------------------------------------------------- the budget

    @Test
    void theLoudestSoundInTheGameStaysCheap() {
        SoundField f = field(open(85, 85), 42, 42, SoundKind.GUNSHOT);
        assertNotNull(f);
        assertTrue(f.visitedCells() < 6000,
                "a gunshot in the open visited " + f.visitedCells() + " cells");
    }

    @Test
    void floodIsDeterministic() {
        SoundField a = field(open(25, 5), 0, 2, SoundKind.SCREAM);
        SoundField b = field(open(25, 5), 0, 2, SoundKind.SCREAM);
        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 25; x++) {
                assertEquals(a.received(x, y), b.received(x, y));
                assertEquals(a.directionAt(x, y), b.directionAt(x, y));
            }
        }
    }

    // ------------------------------------------------------------------ helpers

    private static boolean contains(List<Point> path, int x, int y) {
        for (int i = 0; i < path.size(); i++) {
            if (path.get(i).getX() == x && path.get(i).getY() == y) {
                return true;
            }
        }
        return false;
    }

    private static SoundField talk(WorldLayer layer, int x, int y) {
        return field(layer, x, y, SoundKind.TALK);
    }

    private static SoundField field(WorldLayer layer, int x, int y, SoundKind kind) {
        return Acoustics.propagate(layer, x, y, kind.db(), 0);
    }

    /** All-open grid. Everything outside it is sealed, so nothing leaks round the edges. */
    private static WorldLayer open(int w, int h) {
        return new TestLayer(w, h);
    }

    /** A full-height barrier at {@code wallX}, optionally with one gap punched in it. */
    private static WorldLayer barrier(int w, int h, int wallX, int gapY, int gapLoss,
                                      int wallLoss) {
        TestLayer layer = new TestLayer(w, h);
        for (int y = 0; y < h; y++) {
            layer.set(wallX, y, y == gapY ? gapLoss : wallLoss);
        }
        return layer;
    }

    private static WorldLayer wall(int loss) {
        return barrier(25, 5, 1, -1, 0, loss);
    }

    private static WorldLayer doorway(int gapLoss) {
        return doorway(gapLoss, 25);
    }

    private static WorldLayer doorway(int gapLoss, int w) {
        return barrier(w, 5, 1, 2, gapLoss, SoundConfig.TL_WALL_OUTER);
    }

    /**
     * A grid from ASCII. {@code .} open, {@code #} interior wall, {@code =} exterior wall,
     * {@code +} shut door, {@code S}/{@code L} open floor markers for readability.
     */
    private static WorldLayer parse(String... rows) {
        TestLayer layer = new TestLayer(rows[0].length(), rows.length);
        for (int y = 0; y < rows.length; y++) {
            for (int x = 0; x < rows[y].length(); x++) {
                layer.set(x, y, lossOf(rows[y].charAt(x)));
            }
        }
        return layer;
    }

    private static int lossOf(char c) {
        switch (c) {
            case '#': return SoundConfig.TL_WALL_INNER;
            case '=': return SoundConfig.TL_WALL_OUTER;
            case '+': return SoundConfig.TL_DOOR_SHUT;
            case '/': return SoundConfig.TL_DOOR_OPEN;
            case 'w': return SoundConfig.TL_WINDOW;
            default:  return SoundConfig.TL_OPEN;
        }
    }

    /**
     * A bare grid of tiles with no chunks, no generator and no environment.
     *
     * <p>{@link TownFixture} exists for properties of a whole finished town; these are
     * properties of the arithmetic, and a synthetic grid is the only way to state them
     * exactly. Out of bounds returns null, which the flood treats as sealed — so a barrier
     * spanning the grid really is a barrier and sound cannot creep round its ends.
     */
    private static final class TestLayer extends WorldLayer {
        private final int w;
        private final int h;
        private final RLTile[] tiles;

        TestLayer(int w, int h) {
            this.w = w;
            this.h = h;
            this.tiles = new RLTile[w * h];
            for (int i = 0; i < tiles.length; i++) {
                tiles[i] = new RLTile();
            }
        }

        void set(int x, int y, int loss) {
            tiles[y * w + x].setSoundLoss(loss);
        }

        @Override
        public WorldTile get_tile(int x, int y) {
            if (x < 0 || y < 0 || x >= w || y >= h) {
                return null;
            }
            return tiles[y * w + x];
        }
    }
}
