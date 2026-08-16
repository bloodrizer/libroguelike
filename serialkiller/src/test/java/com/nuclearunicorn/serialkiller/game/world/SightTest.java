package com.nuclearunicorn.serialkiller.game.world;

import com.nuclearunicorn.libroguelike.game.world.layers.WorldLayer;
import com.nuclearunicorn.serialkiller.generators.TileGrid;
import com.nuclearunicorn.serialkiller.generators.TownFixture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Line of sight, which the codebase did not have.
 *
 * <p>Everything that wanted it used {@code Fov.in_range}, a squared-distance test wearing the
 * name of a field-of-view class. The visible consequence was NPCs greeting and answering
 * people through walls, because the "Nearby:" line in their prompt was a radius query.
 */
class SightTest {

    @Test
    void aWallBetweenTwoPeopleStopsTheView() {
        WorldLayer l = grid(
                "=======",
                "=a=b..=",
                "=======");
        assertFalse(Sight.clearLine(l, 1, 1, 3, 1), "there is a wall between them");
        assertTrue(Sight.clearLine(l, 3, 1, 4, 1), "same room, one tile apart");
        assertTrue(Sight.clearLine(l, 3, 1, 5, 1));
    }

    @Test
    void aDoorwayIsSomethingYouCanSeeThrough() {
        WorldLayer l = grid(
                "=======",
                "=a=b..=",
                "=======");
        WorldLayer open = grid(
                "=======",
                "=a/b..=",
                "=======");
        assertFalse(Sight.clearLine(l, 1, 1, 3, 1));
        assertTrue(Sight.clearLine(open, 1, 1, 3, 1),
                "an open door is a hole in a wall for eyes as much as for sound");
    }

    @Test
    void youCanAlwaysSeeYourOwnTile() {
        assertTrue(Sight.clearLine(grid("=====", "=a..=", "====="), 1, 1, 1, 1));
    }

    @Test
    void offTheMapIsNotVisible() {
        assertFalse(Sight.clearLine(grid("=====", "=a..=", "====="), 1, 1, 40, 1));
        assertFalse(Sight.clearLine(null, 0, 0, 1, 1));
    }

    /**
     * The property that matters in play, checked against real generated buildings rather than
     * a hand-drawn one: standing on either side of a solid wall, nobody sees anybody.
     *
     * <p>A town is the only place this can be asserted honestly. The hand-built grids above
     * say the ray works; only a generated street says the ray is being fired at the tiles the
     * game actually builds — soundLoss, wall flags and doorways all come from the generator.
     */
    @ParameterizedTest
    @MethodSource("seeds")
    void nobodySeesThroughAGeneratedWall(long seed) {
        TownFixture.Town town = TownFixture.town(seed);
        int checked = 0;
        for (int y = 1; y < town.size - 1 && checked < 200; y++) {
            for (int x = 1; x < town.size - 1 && checked < 200; x++) {
                RLTile wall = town.tile(x, y);
                if (wall == null || !wall.isWall() || wall.isWallGap()) {
                    continue;
                }
                //a wall with clear floor directly opposite on both sides: inside and out
                if (!open(town, x - 1, y) || !open(town, x + 1, y)) {
                    continue;
                }
                assertFalse(Sight.clearLine(town.layer, x - 1, y, x + 1, y),
                        "saw straight through the wall at " + town.describe(x, y));
                checked++;
            }
        }
        if (checked == 0) {
            fail("seed " + seed + " produced no wall with floor on both sides");
        }
    }

    private static boolean open(TownFixture.Town town, int x, int y) {
        RLTile tile = town.tile(x, y);
        return tile != null && !tile.isWall() && !tile.isWallGap() && !tile.isBlocked();
    }

    static long[] seeds() {
        return new long[]{1L, 7L, 42L};
    }

    /** The same hand-built grid the acoustic tests use: a {@code =} really is a wall in it. */
    private static WorldLayer grid(String... rows) {
        return TileGrid.parse(rows);
    }
}
