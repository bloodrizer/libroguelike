package com.nuclearunicorn.serialkiller.game.sound;

import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.serialkiller.game.world.RLTile;
import com.nuclearunicorn.serialkiller.game.world.entities.EntityDoor;
import com.nuclearunicorn.serialkiller.generators.TownFixture;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Acoustic properties of real generated towns (SOUND_DESIGN.md 7.1).
 *
 * <p>The arithmetic is covered by {@link AcousticsTest} on synthetic grids. What cannot be
 * tested there is whether the <i>town</i> agrees with it: {@code RLTile.soundLoss} is baked
 * at generation time so the flood's inner loop stays a flat array read, and a baked value is
 * a value that can drift out of sync with the thing it describes. That drift is invisible in
 * play — sound simply behaves slightly wrongly forever — so it gets an invariant.
 */
class SoundInvariantsTest {

    static long[] seeds() {
        return new long[]{1L, 7L, 42L, 1234L, 987654321L};
    }

    /**
     * Every wall in town is expensive, every door carries its lock state, every window is
     * glass, and open floor is free.
     */
    @ParameterizedTest
    @MethodSource("seeds")
    void bakedLossMatchesWhatIsActuallyThere(long seed) {
        TownFixture.Town town = TownFixture.town(seed);

        int walls = 0;
        int doors = 0;
        int windows = 0;

        for (int y = 0; y < town.size; y++) {
            for (int x = 0; x < town.size; x++) {
                RLTile tile = town.tile(x, y);
                if (tile == null) {
                    continue;
                }
                int loss = tile.getSoundLoss();

                if (tile.isWall()) {
                    walls++;
                    assertTrue(loss >= SoundConfig.TL_WALL_INNER,
                            "wall at " + town.describe(x, y) + " has loss " + loss);
                    continue;
                }

                EntityDoor door = door(tile);
                if (door != null) {
                    doors++;
                    assertEquals(door.isLocked() ? SoundConfig.TL_DOOR_LOCKED
                                                 : SoundConfig.TL_DOOR_SHUT,
                            loss, "door at " + town.describe(x, y));
                    continue;
                }

                if (hasWindow(tile)) {
                    windows++;
                    assertEquals(SoundConfig.TL_WINDOW, loss,
                            "window at " + town.describe(x, y));
                }
            }
        }

        // guard against the assertions above passing because the town was empty
        assertTrue(walls > 100, "expected a town with walls, found " + walls);
        assertTrue(doors > 0, "expected a town with doors");
        assertTrue(windows > 0, "expected a town with windows");
    }

    /**
     * A building contains sound. Talking indoors must reach fewer places than talking on the
     * street outside it — which is the whole point of the exercise, stated as a property
     * rather than as a hand-picked pair of coordinates.
     */
    @ParameterizedTest
    @MethodSource("seeds")
    void buildingsContainConversation(long seed) {
        TownFixture.Town town = TownFixture.town(seed);

        int indoorReach = reach(town, true);
        int outdoorReach = reach(town, false);

        assertTrue(indoorReach > 0, "found no indoor tile to speak from");
        assertTrue(outdoorReach > 0, "found no outdoor tile to speak from");
        assertTrue(indoorReach < outdoorReach,
                "talking indoors reached " + indoorReach + " tiles, outdoors " + outdoorReach
                        + " - walls are not containing anything");
    }

    /** Following the direction field from anywhere audible must arrive at the source. */
    @ParameterizedTest
    @MethodSource("seeds")
    void everyPathLeadsHome(long seed) {
        TownFixture.Town town = TownFixture.town(seed);
        int[] src = firstTile(town, false);
        assertNotNull(src);

        SoundField f = Acoustics.propagate(town.layer, src[0], src[1],
                SoundKind.SCREAM.db(), 0);
        assertNotNull(f);

        for (int y = f.y0; y < f.y0 + f.size; y += 3) {
            for (int x = f.x0; x < f.x0 + f.size; x += 3) {
                if (f.received(x, y) == Integer.MIN_VALUE) {
                    continue;
                }
                java.util.List<org.lwjgl.util.Point> path = f.pathToSource(x, y);
                assertNotNull(path);
                org.lwjgl.util.Point end = path.get(path.size() - 1);
                assertEquals(src[0], end.getX(), "path from " + x + "," + y + " went astray");
                assertEquals(src[1], end.getY(), "path from " + x + "," + y + " went astray");
            }
        }
    }

    // ------------------------------------------------------------------ helpers

    /** Audible-cell count for a normal conversation from the first indoor/outdoor tile. */
    private static int reach(TownFixture.Town town, boolean indoor) {
        int[] at = firstTile(town, indoor);
        if (at == null) {
            return 0;
        }
        SoundField f = Acoustics.propagate(town.layer, at[0], at[1], SoundKind.TALK.db(), 0);
        return f == null ? 0 : f.visitedCells();
    }

    /** A walkable tile of the requested kind, taken from the middle of the map outward. */
    private static int[] firstTile(TownFixture.Town town, boolean indoor) {
        for (int y = 10; y < town.size - 10; y++) {
            for (int x = 10; x < town.size - 10; x++) {
                RLTile tile = town.tile(x, y);
                if (tile == null || tile.isWall() || tile.isWallGap() || tile.isBlocked()) {
                    continue;
                }
                if (tile.isIndoor() == indoor) {
                    return new int[]{x, y};
                }
            }
        }
        return null;
    }

    private static EntityDoor door(RLTile tile) {
        for (Entity ent : tile.ent_list) {
            if (ent instanceof EntityDoor) {
                return (EntityDoor) ent;
            }
        }
        return null;
    }

    private static boolean hasWindow(RLTile tile) {
        for (Entity ent : tile.ent_list) {
            if ("window".equals(ent.getName())) {
                return true;
            }
        }
        return false;
    }
}
