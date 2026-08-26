package com.nuclearunicorn.serialkiller.render.map;

import com.nuclearunicorn.serialkiller.game.world.RLTile;
import com.nuclearunicorn.serialkiller.generators.TownFixture;
import com.nuclearunicorn.serialkiller.generators.town.Building;
import com.nuclearunicorn.serialkiller.generators.town.BuildingType;
import com.nuclearunicorn.serialkiller.generators.town.GridMask;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The map invariants, §F of INVARIANTS.md, checked against real generated towns.
 *
 * <pre>
 *   F1  every building the generator raised is on the map, in its own type's colour
 *   F2  a house nobody has walked into is not on the map at all
 *   F3  ...unless it is a landmark or the player's own home, which are known from the start
 *   F4  the raster covers the chunk, and the labels sit on the buildings they name
 * </pre>
 *
 * <p>F1 and F2 pull against each other, which is the whole reason they are written down. The
 * map is the one place in the game with a licence to read the world model directly, so a
 * change to how buildings are stamped or how fog is read has nothing to stop it turning the
 * map into either a blank plate or a wallhack, and both look plausible on the screenshot the
 * change was made for. Neither survives being asserted on six towns.
 */
class TownMapTest {

    static long[] seeds() {
        return new long[]{1L, 7L, 42L, 1234L, 987654321L};
    }

    /** F1: reveal the fog and every building is there, painted in the colour of its type. */
    @ParameterizedTest
    @MethodSource("seeds")
    void everyBuildingIsPaintedInItsOwnColour(long seed) {
        TownFixture.Town town = TownFixture.town(seed);
        TownMap map = new TownMap();
        assertTrue(map.build(town.layer, town.buildings, town.playerStart.getX(),
                town.playerStart.getY(), true), "seed " + seed + ": nothing to map");
        assertFalse(town.buildings.isEmpty(), "seed " + seed + ": town has no buildings");

        for (Building building : town.buildings) {
            int tint = building.isPlayerHome
                    ? MapPalette.HOME_WALL : MapPalette.wall(building.type);
            int floor = MapPalette.shade(tint, MapPalette.FLOOR_AMT);
            boolean painted = false;
            for (int[] at : tiles(building)) {
                int color = map.colorAt(at[0], at[1]);
                if (color == tint || color == floor) {
                    painted = true;
                    break;
                }
            }
            assertTrue(painted, "seed " + seed + ": " + describe(building)
                    + " is on the ground but not on the map");
        }
    }

    /**
     * F2/F3: with the fog as the generator left it — only the safehouse walked — an ordinary
     * flat is blank and a landmark is not.
     */
    @ParameterizedTest
    @MethodSource("seeds")
    void fogHidesHousesAndSparesLandmarks(long seed) {
        TownFixture.Town town = TownFixture.town(seed);
        TownMap map = new TownMap();
        assertTrue(map.build(town.layer, town.buildings, town.playerStart.getX(),
                town.playerStart.getY(), false));

        int hidden = 0;
        int shown = 0;
        for (Building building : town.buildings) {
            List<int[]> tiles = tiles(building);
            if (walked(town, tiles)) {
                continue;       //the safehouse and whatever shares a wall with it
            }
            boolean landmark = building.isPlayerHome
                    || MapPalette.isLandmark(building.type);
            for (int[] at : tiles) {
                int color = map.colorAt(at[0], at[1]);
                if (landmark) {
                    continue;
                }
                assertEquals(MapPalette.UNMAPPED, color, "seed " + seed + ": "
                        + describe(building) + " has never been entered but is drawn at "
                        + at[0] + "," + at[1]);
            }
            if (landmark) {
                shown++;
                assertTrue(anyPainted(map, tiles), "seed " + seed + ": " + describe(building)
                        + " is a landmark and should be on the map before it is visited");
            } else {
                hidden++;
            }
        }
        assertTrue(hidden > 0, "seed " + seed + ": no unvisited houses, fog proves nothing");
        assertTrue(shown > 0, "seed " + seed + ": no unvisited landmarks to check");
    }

    /** F3: exactly the places worth a name get one, and the player's own flat is one of them. */
    @ParameterizedTest
    @MethodSource("seeds")
    void landmarksAreNamedAndNothingElseIs(long seed) {
        TownFixture.Town town = TownFixture.town(seed);
        TownMap map = new TownMap();
        map.build(town.layer, town.buildings, town.playerStart.getX(),
                town.playerStart.getY(), false);

        int expected = 0;
        for (Building building : town.buildings) {
            if (building.isPlayerHome || MapPalette.isLandmark(building.type)) {
                expected++;
            }
        }
        assertEquals(expected, map.landmarks().size(),
                "seed " + seed + ": a label for every landmark and for nothing else");

        int homes = 0;
        for (TownMap.Landmark place : map.landmarks()) {
            assertTrue(map.contains(place.x, place.y),
                    "seed " + seed + ": " + place.label + " is labelled off the map");
            if (place.home) {
                homes++;
                assertEquals("HOME", place.label);
            }
        }
        assertEquals(1, homes, "seed " + seed + ": the player has exactly one home");
    }

    /** F4: the raster is the chunk, so nothing the player can walk to is off the edge of it. */
    @ParameterizedTest
    @MethodSource("seeds")
    void rasterCoversTheChunk(long seed) {
        TownFixture.Town town = TownFixture.town(seed);
        TownMap map = new TownMap();
        map.build(town.layer, town.buildings, town.playerStart.getX(),
                town.playerStart.getY(), true);

        assertEquals(town.size, map.width(), "seed " + seed);
        assertEquals(town.size, map.height(), "seed " + seed);
        assertTrue(map.contains(town.playerStart.getX(), town.playerStart.getY()),
                "seed " + seed + ": the player starts off their own map");
        assertEquals(town.size * town.size, map.pixels().length, "seed " + seed);
    }

    // ------------------------------------------------------------------ helpers

    /** Every world tile the building occupies, from its footprint mask. */
    private static List<int[]> tiles(Building building) {
        List<int[]> out = new ArrayList<int[]>();
        GridMask mask = building.footprint;
        if (mask == null) {
            return out;
        }
        for (int ly = 0; ly < mask.h; ly++) {
            for (int lx = 0; lx < mask.w; lx++) {
                if (mask.get(lx, ly)) {
                    out.add(new int[]{mask.ox + lx, mask.oy + ly});
                }
            }
        }
        return out;
    }

    private static boolean walked(TownFixture.Town town, List<int[]> tiles) {
        for (int[] at : tiles) {
            RLTile tile = town.tile(at[0], at[1]);
            if (tile != null && tile.isExplored()) {
                return true;
            }
        }
        return false;
    }

    private static boolean anyPainted(TownMap map, List<int[]> tiles) {
        for (int[] at : tiles) {
            if (map.colorAt(at[0], at[1]) != MapPalette.UNMAPPED) {
                return true;
            }
        }
        return false;
    }

    private static String describe(Building building) {
        BuildingType type = building.type;
        return (building.isPlayerHome ? "the player's home (" + type + ")" : type.toString())
                + " at " + building.getX() + "," + building.getY();
    }
}
