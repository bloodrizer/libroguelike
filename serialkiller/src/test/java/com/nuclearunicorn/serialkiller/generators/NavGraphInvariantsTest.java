package com.nuclearunicorn.serialkiller.generators;

import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.utils.pathfinder.PathCheck;
import com.nuclearunicorn.serialkiller.game.controllers.RLController;
import com.nuclearunicorn.serialkiller.game.world.RLTile;
import com.nuclearunicorn.serialkiller.game.world.RLWorldChunk;
import com.nuclearunicorn.serialkiller.game.world.entities.EntityRLHuman;
import com.nuclearunicorn.serialkiller.utils.pathfinder.adaptive.AdaptiveNode;
import com.nuclearunicorn.serialkiller.utils.pathfinder.adaptive.AdaptivePathfinder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.lwjgl.util.Point;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The pathfinding invariants, §E of INVARIANTS.md, checked against real generated towns.
 *
 * <pre>
 *   E1  every milestone is a node of the nav graph
 *   E2  every milestone is routable from every other
 *   E3  an assembled route is contiguous - no step is a leap
 * </pre>
 *
 * <p>All three describe the same failure from further and further away, and it is the one in
 * the screenshot: a policeman who walks diagonally across town through the buildings. E3 is
 * what you see. E2 is why it happened — the milestone route came back empty-handed and the
 * two ends of the trip were joined to each other over open country. E1 is the actual defect:
 * a front door is a hole in a wall, so every straight line out of it runs along that wall, so
 * it linked to nothing and never entered the graph — while remaining the nearest milestone to
 * everyone in the house.
 *
 * <p>Each test builds its own town rather than sharing {@link TownFixture#town}: the graph
 * lives in a static, so only the most recently generated town's graph is the one loaded.
 */
class NavGraphInvariantsTest {

    /** Routes sampled per town for E3. Enough to hit the awkward milestone, small enough to be quick. */
    private static final int ROUTES = 60;

    static long[] seeds() {
        return TownFixture.seeds();
    }

    /**
     * E1. Every milestone is in the graph.
     *
     * <p>A milestone that is not is worse than one that does not exist: it is still what
     * {@code getNearestMilestone} hands back, so it is still where routes are planned
     * through — a waypoint with no way in and no way out.
     */
    @ParameterizedTest(name = "seed {0}")
    @MethodSource("seeds")
    void everyMilestoneIsANodeOfTheGraph(long seed) {
        TownFixture.Town town = TownFixture.generate(seed);
        List<String> found = new ArrayList<String>();

        for (Point milestone : milestones(town)) {
            AdaptiveNode node = node(milestone);
            if (node == null) {
                found.add(milestone + " " + town.describe(milestone.getX(), milestone.getY())
                        + " is not in the graph at all");
            } else if (node.nb.isEmpty()) {
                found.add(milestone + " " + town.describe(milestone.getX(), milestone.getY())
                        + " is in the graph with no edges");
            }
        }

        assertTrue(found.isEmpty(), () -> "seed " + seed + ": " + found.size()
                + " unroutable milestone(s) -" + list(found));
    }

    /** E2. The nav graph is one place: from any milestone, Dijkstra reaches all the others. */
    @ParameterizedTest(name = "seed {0}")
    @MethodSource("seeds")
    void everyMilestoneIsRoutableFromEveryOther(long seed) {
        TownFixture.Town town = TownFixture.generate(seed);
        List<Point> milestones = milestones(town);
        List<String> found = new ArrayList<String>();

        for (Point from : milestones) {
            AdaptivePathfinder.resetState();
            AdaptivePathfinder.calculateAdaptiveRoutes(from);
            for (Point to : milestones) {
                if (from.equals(to)) {
                    continue;
                }
                List<AdaptiveNode> route = AdaptivePathfinder.getShortestPathTo(to);
                if (route.isEmpty() && found.size() < 40) {
                    found.add(from + " -> " + to);
                }
            }
        }

        assertTrue(found.isEmpty(), () -> "seed " + seed + ": " + found.size()
                + " milestone pair(s) with no route between them, so the town is not one"
                + " place and half of it is unreachable -" + list(found));
    }

    /**
     * E3. What the walker is actually handed. Every route an NPC plans across this town is a
     * route: consecutive steps are neighbours, all the way from where they stand to where
     * they are going.
     *
     * <p>A gap here is not a cosmetic defect. {@code follow_path} walks toward the next point
     * in a straight line whatever stands in between, so a route with a hole in it is a
     * cross-country diagonal — the one on screen, with an officer bouncing off every wall
     * along it.
     */
    @ParameterizedTest(name = "seed {0}")
    @MethodSource("seeds")
    void everyRouteAnNpcPlansIsContiguous(long seed) {
        TownFixture.Town town = TownFixture.generate(seed);
        List<RLController> walkers = walkers(town);
        assertTrue(!walkers.isEmpty(), "seed " + seed + ": a town with nobody in it to route");

        Random rng = new Random(seed);
        List<String> found = new ArrayList<String>();
        int planned = 0;
        int refused = 0;

        for (int i = 0; i < ROUTES; i++) {
            RLController ctrl = walkers.get(i % walkers.size());
            Entity npc = ((EntityRLHuman) walkerOwner(town, ctrl));
            Point target = randomWalkable(town, rng);
            if (target == null) {
                break;
            }

            ctrl.calculateAdaptivePath(new Point(npc.origin), target);
            List<Point> route = ctrl.path;
            if (route == null || route.isEmpty()) {
                refused++;     //no route is an honest answer; a wrong one is not
                continue;
            }
            planned++;

            int gap = PathCheck.firstGap(route);
            if (gap >= 0 && found.size() < 8) {
                found.add(npc.getName() + " at " + npc.origin + " -> " + target
                        + ": step " + gap + " leaps from " + route.get(gap - 1)
                        + " to " + route.get(gap) + " (" + route.size() + " steps)");
            }
            Point last = route.get(route.size() - 1);
            if (!last.equals(target) && found.size() < 8) {
                found.add(npc.getName() + " at " + npc.origin + " -> " + target
                        + ": the route ends at " + last + " instead");
            }
        }

        final int routed = planned;
        final int gaveUp = refused;
        assertTrue(found.isEmpty(), () -> "seed " + seed + ": " + found.size() + " of " + routed
                + " route(s) are not walkable routes -" + list(found));
        //a town where nothing can be routed would pass the check above vacuously
        assertTrue(routed > gaveUp, "seed " + seed + ": only " + routed + " of "
                + (routed + gaveUp) + " trips could be planned at all");
    }

    /**
     * E4. An NPC told to walk to the far side of town gets a route to it.
     *
     * <p>{@code set_destination} is what every behaviour actually calls, and on its own it is
     * A*, which gives up past {@code MAX_SEARCH_DISTANCE} — about a fifth of long trips came
     * back with a route before the milestone fallback was put behind it. The rest were not
     * refusals anyone noticed: {@link com.nuclearunicorn.serialkiller.game.ai.behavior.PursueAction}
     * answers "no path" by walking straight at the suspect, so a chase across town was an
     * officer pressed against the near wall of a building for as long as it lasted.
     *
     * <p>Four in five rather than all: a locked shop and the odd tile behind furniture are
     * genuinely unreachable, and refusing those is correct. The number is a floor under a
     * regression, not a target — it measures nearer 19 in 20.
     */
    @ParameterizedTest(name = "seed {0}")
    @MethodSource("seeds")
    void anNpcCanBeSentAcrossTown(long seed) {
        TownFixture.Town town = TownFixture.generate(seed);
        List<RLController> walkers = walkers(town);
        Random rng = new Random(seed);

        int trips = 0;
        int planned = 0;
        for (int i = 0; i < ROUTES && trips < 20; i++) {
            RLController ctrl = walkers.get(i % walkers.size());
            Entity npc = walkerOwner(town, ctrl);
            Point target = randomWalkable(town, rng);
            if (target == null || distance(npc.origin, target) <= 100) {
                continue;   //only the long trips: A* handles the short ones by itself
            }
            trips++;
            ctrl.set_destination(new Point(target));
            if (ctrl.hasPath()) {
                planned++;
            }
        }

        final int made = planned;
        final int asked = trips;
        assertTrue(asked > 0, "seed " + seed + ": no trip was long enough to be a test");
        assertTrue(made * 5 >= asked * 4, () -> "seed " + seed + ": only " + made + " of "
                + asked + " cross-town trips could be planned, and an NPC with no route walks"
                + " straight at its target through whatever is in the way");
    }

    private static int distance(Point a, Point b) {
        return Math.abs(a.getX() - b.getX()) + Math.abs(a.getY() - b.getY());
    }

    private static List<Point> milestones(TownFixture.Town town) {
        return ((RLWorldChunk) town.layer.get_cached_chunk(0, 0)).getMilestones();
    }

    /** The graph's node for a milestone, or null if the builder never linked it to anything. */
    private static AdaptiveNode node(Point milestone) {
        for (AdaptiveNode node : AdaptivePathfinder.nodes) {
            if (node.isNodeOf(milestone)) {
                return node;
            }
        }
        return null;
    }

    private static List<RLController> walkers(TownFixture.Town town) {
        List<RLController> found = new ArrayList<RLController>();
        for (Entity ent : town.entities) {
            if (ent instanceof EntityRLHuman && ent.controller instanceof RLController) {
                found.add((RLController) ent.controller);
            }
        }
        return found;
    }

    private static Entity walkerOwner(TownFixture.Town town, RLController ctrl) {
        for (Entity ent : town.entities) {
            if (ent.controller == ctrl) {
                return ent;
            }
        }
        throw new IllegalStateException("controller with no owner");
    }

    /** Somewhere in town a person could stand. */
    private static Point randomWalkable(TownFixture.Town town, Random rng) {
        for (int i = 0; i < 500; i++) {
            int x = rng.nextInt(town.size);
            int y = rng.nextInt(town.size);
            RLTile tile = town.tile(x, y);
            if (tile != null && !tile.isPathBlocked()) {
                return new Point(x, y);
            }
        }
        return null;
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
