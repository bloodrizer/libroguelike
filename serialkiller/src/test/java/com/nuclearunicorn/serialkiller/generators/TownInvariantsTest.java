package com.nuclearunicorn.serialkiller.generators;

import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.serialkiller.game.ai.PoliceAI;
import com.nuclearunicorn.serialkiller.game.world.RLTile;
import com.nuclearunicorn.serialkiller.game.world.RLWorldModel;
import com.nuclearunicorn.serialkiller.game.world.entities.EntityDoor;
import com.nuclearunicorn.serialkiller.game.world.entities.EntityFurniture;
import com.nuclearunicorn.serialkiller.game.world.entities.EntityRLHuman;
import com.nuclearunicorn.serialkiller.game.world.entities.EntityRLPlayer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.lwjgl.util.Point;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The world-gen invariants, §C of INVARIANTS.md, checked against real generated towns.
 *
 * <pre>
 *   C1  furniture may not spawn adjacent to a window or a door
 *   C2  ...nor may the doors and windows themselves be adjacent to each other
 *   C3  a non-police NPC spawns in their home rooms
 * </pre>
 *
 * <p>All three are about the same failure, seen from three sides: a town that looks plausible
 * in a screenshot and does not work. A crate against the only doorway seals a room; a wall
 * knocked through into a row of doors is what a room ringed by pockets looks like after the
 * connectivity repair has had a go at it; a resident who starts on the pavement outside a
 * house they cannot enter stands there all night. None of it throws, nothing logs an error,
 * and the first symptom is an NPC who never arrives — hours later, three subsystems away.
 *
 * <p>Every check runs across {@link TownFixture#seeds()} rather than one town, because each
 * of these bugs held on most layouts and failed on a particular one. A failure names the seed
 * and the tile, so the town can be walked in the game with {@code -Dreplay.seed=<n>} and
 * looked at with {@code DEBUG="world=map"}.
 */
class TownInvariantsTest {

    static long[] seeds() {
        return TownFixture.seeds();
    }

    /**
     * C1. Nothing you would have to walk around stands in a doorway or under a window.
     *
     * <p>Furniture here means a prop on the floor: the doors are the openings themselves, and
     * a window is furniture standing <i>in</i> a wall gap rather than beside one.
     */
    @ParameterizedTest(name = "seed {0}")
    @MethodSource("seeds")
    void furnitureNeverStandsInADoorway(long seed) {
        TownFixture.Town town = TownFixture.town(seed);
        List<String> found = new ArrayList<String>();

        for (int x = 0; x < town.size; x++) {
            for (int y = 0; y < town.size; y++) {
                RLTile tile = town.tile(x, y);
                if (tile == null || tile.isWallGap()) {
                    continue;   //the opening itself: that is where a door or window belongs
                }
                if (!touchesGap(town, x, y)) {
                    continue;
                }
                for (Entity ent : tile.ent_list) {
                    if (ent instanceof EntityFurniture && !(ent instanceof EntityDoor)) {
                        found.add(ent.getName() + " at " + town.describe(x, y));
                    }
                }
            }
        }

        assertTrue(found.isEmpty(), () -> "seed " + seed + ": " + found.size()
                + " prop(s) blocking a door or window, which is a room nobody can get into"
                + " once one more thing lands beside it -" + list(found));
    }

    /**
     * C2. No two openings touch. A wall is a wall; where every other tile of it has been
     * knocked through it stops reading as one, and the way that happened before was a repair
     * pass punching a fresh door per tile of the same sealed pocket.
     */
    @ParameterizedTest(name = "seed {0}")
    @MethodSource("seeds")
    void doorsAndWindowsNeverTouch(long seed) {
        TownFixture.Town town = TownFixture.town(seed);
        List<String> found = new ArrayList<String>();

        //east and south only: every adjacent pair is then reported once, not twice
        for (int x = 0; x < town.size; x++) {
            for (int y = 0; y < town.size; y++) {
                if (!town.isGap(x, y)) {
                    continue;
                }
                if (town.isGap(x + 1, y)) {
                    found.add(town.describe(x, y) + " | " + town.describe(x + 1, y));
                }
                if (town.isGap(x, y + 1)) {
                    found.add(town.describe(x, y) + " | " + town.describe(x, y + 1));
                }
            }
        }

        assertTrue(found.isEmpty(), () -> "seed " + seed + ": " + found.size()
                + " adjacent opening(s) - a wall made of doors -" + list(found));
    }

    /**
     * C3. Everyone who is not on duty starts the game indoors, at home.
     *
     * <p>The clock opens at 21:00, so this is what the town should look like: police out, the
     * rest in bed. It is also the check that catches a resident housed somewhere they cannot
     * reach — spawned inside, they are on the right side of their own front door by
     * construction, and an NPC in the wrong place at turn zero is a generator bug rather than
     * a pathfinding one.
     *
     * <p>The player is excluded because the generator does not place them: it records
     * {@link RLWorldModel#playerSafeHouseLocation} and {@code InGameMode} moves them there
     * once the world exists, so that location is asserted instead.
     */
    @ParameterizedTest(name = "seed {0}")
    @MethodSource("seeds")
    void everyCivilianStartsInTheirOwnHome(long seed) {
        TownFixture.Town town = TownFixture.town(seed);
        List<String> found = new ArrayList<String>();
        int civilians = 0;

        for (Entity ent : town.entities) {
            if (!(ent instanceof EntityRLHuman) || ent instanceof EntityRLPlayer) {
                continue;
            }
            EntityRLHuman human = (EntityRLHuman) ent;
            if (human.getAI() instanceof PoliceAI) {
                continue;   //on duty: the station lobby and the street are where they belong
            }
            civilians++;

            Apartment home = human.getApartment();
            if (home == null) {
                found.add(human.getName() + " has no home at all, and every route home they"
                        + " ever plan will resolve to UNRESOLVED, at "
                        + town.describe(ent.origin.getX(), ent.origin.getY()));
            } else if (!inRooms(home, ent.origin.getX(), ent.origin.getY())) {
                found.add(human.getName() + " is at "
                        + town.describe(ent.origin.getX(), ent.origin.getY())
                        + " but lives at " + home);
            }
        }

        final int counted = civilians;
        assertTrue(counted > 0, "seed " + seed + ": a town with nobody living in it");
        assertTrue(found.isEmpty(), () -> "seed " + seed + ": " + found.size() + " of "
                + counted + " civilian(s) did not start at home -" + list(found));
    }

    /** The player's half of C3: the tile the safehouse hands to InGameMode is a room of it. */
    @ParameterizedTest(name = "seed {0}")
    @MethodSource("seeds")
    void thePlayerStartsInsideTheSafehouse(long seed) {
        TownFixture.Town town = TownFixture.town(seed);

        Apartment home = null;
        for (Entity ent : town.entities) {
            if (ent instanceof EntityRLPlayer) {
                home = ((EntityRLPlayer) ent).getApartment();
            }
        }
        assertTrue(home != null, "seed " + seed + ": the player was never given a safehouse");

        Point start = town.playerStart;
        assertTrue(start != null, "seed " + seed + ": no start position was recorded");

        final Apartment safehouse = home;
        assertTrue(inRooms(safehouse, start.getX(), start.getY()),
                () -> "seed " + seed + ": the game starts the player at "
                        + town.describe(start.getX(), start.getY())
                        + ", which is not a room of the safehouse " + safehouse
                        + " - the lot rect includes the front yard");
    }

    /** True if a door or window opens onto this tile from any of the four sides. */
    private static boolean touchesGap(TownFixture.Town town, int x, int y) {
        return town.isGap(x - 1, y) || town.isGap(x + 1, y)
            || town.isGap(x, y - 1) || town.isGap(x, y + 1);
    }

    /** Inside one of the home's rooms — walls excluded, so a doorway does not count. */
    private static boolean inRooms(Apartment home, int x, int y) {
        if (home.rooms == null) {
            return false;
        }
        for (Block room : home.rooms) {
            if (x > room.getX() && x < room.getX() + room.getW()
                    && y > room.getY() && y < room.getY() + room.getH()) {
                return true;
            }
        }
        return false;
    }

    /** The offenders, a few at a time: a hundred coordinates in a failure message help nobody. */
    private static String list(List<String> found) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < found.size() && i < 8; i++) {
            out.append("\n  ").append(found.get(i));
        }
        if (found.size() > 8) {
            out.append("\n  ...and ").append(found.size() - 8).append(" more");
        }
        return out.toString();
    }
}
