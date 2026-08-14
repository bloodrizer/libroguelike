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
        int openDoors = 0;
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
                    if (door.isOpen()) {
                        openDoors++;
                    }
                    assertEquals(door.isOpen() ? SoundConfig.TL_DOOR_OPEN
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
        // and against the open-door branch above never being taken: most interior doors
        // are unlocked, which in this game means standing open
        assertTrue(openDoors > 0, "expected a town with open doors, found none of " + doors);
    }

    /**
     * An open door is a hole in a wall. Talking in a room with one must reach further than
     * the same conversation with that door shut.
     *
     * <p>The regression this exists for: door tiles were baked as {@code TL_DOOR_SHUT}
     * regardless of state, so every doorway in town — nearly all of which stand open —
     * muffled 14dB and rooms that are physically connected were acoustically sealed.
     */
    @ParameterizedTest
    @MethodSource("seeds")
    void openDoorsLetSoundThrough(long seed) {
        TownFixture.Town town = TownFixture.town(seed);

        int[] door = firstOpenDoorway(town);
        assertNotNull(door, "no open doorway with passable tiles either side");
        int dx = door[0];
        int dy = door[1];
        int sx = door[2];       // speaker, one side of the doorway
        int sy = door[3];
        int lx = door[4];       // listener, directly opposite
        int ly = door[5];

        SoundField open = Acoustics.propagate(town.layer, sx, sy, SoundKind.TALK.db(), 0);
        // two air steps and the doorway itself: 32 - (2+1) - 2. Nothing can be cheaper,
        // so this is exact regardless of what else the building is shaped like.
        assertEquals(27, open.received(lx, ly),
                "talking beside an open doorway must carry through it");

        town.tile(dx, dy).setSoundLoss(SoundConfig.TL_DOOR_SHUT);
        SoundField shut = Acoustics.propagate(town.layer, sx, sy, SoundKind.TALK.db(), 0);
        assertTrue(shut.received(lx, ly) < 27,
                "shutting the door must cost something, got "
                        + shut.received(lx, ly) + " either way");
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

    /**
     * An open door with open floor directly opposite each other across it, as
     * {@code {doorX, doorY, aX, aY, bX, bY}}. Doors sit in walls, so the pair straddles
     * the wall and there is no two-step route between them that avoids the doorway.
     */
    private static int[] firstOpenDoorway(TownFixture.Town town) {
        for (int y = 10; y < town.size - 10; y++) {
            for (int x = 10; x < town.size - 10; x++) {
                RLTile tile = town.tile(x, y);
                EntityDoor d = tile == null ? null : door(tile);
                if (d == null || !d.isOpen()) {
                    continue;
                }
                if (passable(town, x - 1, y) && passable(town, x + 1, y)) {
                    return new int[]{x, y, x - 1, y, x + 1, y};
                }
                if (passable(town, x, y - 1) && passable(town, x, y + 1)) {
                    return new int[]{x, y, x, y - 1, x, y + 1};
                }
            }
        }
        return null;
    }

    private static boolean passable(TownFixture.Town town, int x, int y) {
        RLTile tile = town.tile(x, y);
        return tile != null && !tile.isWall() && tile.getSoundLoss() == SoundConfig.TL_OPEN;
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
