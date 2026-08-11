package com.nuclearunicorn.serialkiller.utils.pathfinder.adaptive;

import com.nuclearunicorn.serialkiller.game.world.RLTile;
import com.nuclearunicorn.serialkiller.game.world.RLWorldChunk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.Point;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The milestone graph, on a map small enough to draw.
 *
 * <p>Two things went wrong here and they compounded. A front door never entered the graph,
 * because every straight line out of a doorway runs along the wall the door is cut into — and
 * a door is the <i>nearest</i> milestone to everyone in that house. Then routing through a
 * milestone that had no node quietly invented one, so Dijkstra returned a one-element path
 * naming the far end of the trip, which reads exactly like "you are already there". The
 * caller spliced it into a route and the walker flew the diagonal.
 *
 * <pre>
 *     0 1 2 3 4 5 6 7 8
 *  0  M . . . . . . . .     M, B, D, I, J are milestones; D is a door
 *  1  # # # # # D # # #     every line out of D that is not through it runs into this wall
 *  2  . . . . . . . . .
 *  3  B . . . . . . . .     M and B are one doorway apart and nothing else connects them
 *  4  # # # # # # # # #
 *  5  . . J . I . . . .     a strip nothing above can reach
 * </pre>
 */
class AdaptivePathfinderTest {

    private static final int W = 9;
    private static final int H = 6;

    private static final Point M = new Point(0, 0);
    private static final Point D = new Point(5, 1);   //the door
    private static final Point B = new Point(0, 3);
    private static final Point I = new Point(4, 5);
    private static final Point J = new Point(2, 5);

    private RLWorldChunk chunk;

    @BeforeEach
    void buildMap() {
        AdaptivePathfinder.reset();
        chunk = new RLWorldChunk(0, 0);

        for (int x = 0; x < W; x++) {
            for (int y = 0; y < H; y++) {
                RLTile tile = new RLTile();
                boolean wall = y == 1 || y == 4;
                tile.setWall(wall);
                chunk.tile_data.put(new Point(x, y), tile);
            }
        }
        //the door: a hole in the y=1 wall, and the only way between the street and the yard
        RLTile door = (RLTile) chunk.tile_data.get(D);
        door.setWall(false);
        door.setWallGap(true);

        for (Point ms : new Point[]{M, D, B, I, J}) {
            chunk.addMilestone(ms);
        }
        for (Point ms : chunk.getMilestones()) {
            AdaptivePathfinder.addPoint(chunk, ms);
        }
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

    /** The whole point of a door milestone: it is how you get into the building. */
    @Test
    void aDoorwayIsPartOfTheGraph() {
        AdaptiveNode door = node(D);
        assertTrue(door != null && !door.nb.isEmpty(),
                "the door at " + D + " has no edges, so everyone whose nearest milestone it is"
                        + " - which is everyone in that house - can route nowhere");
    }

    /** A route through the door, from the street outside to the yard behind it. */
    @Test
    void routesRunThroughTheDoorway() {
        AdaptivePathfinder.resetState();
        AdaptivePathfinder.calculateAdaptiveRoutes(M);
        List<AdaptiveNode> path = AdaptivePathfinder.getShortestPathTo(B);

        assertFalse(path.isEmpty(), "no route from " + M + " to " + B);
        assertEquals(M, path.get(0).point, "a route has to start where it was asked to");
        assertEquals(B, path.get(path.size() - 1).point);
    }

    /**
     * The bug this file exists for. {@code I} is a perfectly good milestone in a perfectly
     * good component that {@code M} simply cannot reach, and the old answer was the
     * one-element path {@code [I]} — indistinguishable from having arrived.
     */
    @Test
    void anUnreachableMilestoneGivesNoRouteAtAll() {
        assertTrue(node(I) != null, "fixture: " + I + " should be routable from " + J);

        AdaptivePathfinder.resetState();
        AdaptivePathfinder.calculateAdaptiveRoutes(M);

        assertTrue(AdaptivePathfinder.getShortestPathTo(I).isEmpty(),
                "there is a wall between " + M + " and " + I + ", and no route is the only"
                        + " honest answer - a path naming the far end reads as 'already there'");
    }

    /** Asking about somewhere that is not a milestone must not make it one. */
    @Test
    void lookingUpAStrangerDoesNotAddItToTheGraph() {
        int before = AdaptivePathfinder.nodes.size();

        AdaptivePathfinder.resetState();
        AdaptivePathfinder.calculateAdaptiveRoutes(new Point(7, 2));
        assertTrue(AdaptivePathfinder.getShortestPathTo(new Point(8, 2)).isEmpty());

        assertEquals(before, AdaptivePathfinder.nodes.size(),
                "a query grew the graph, so the next query is answered against a different map");
    }
}
