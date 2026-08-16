package com.nuclearunicorn.libroguelike.utils.pathfinder;

import com.nuclearunicorn.libroguelike.utils.pathfinder.astar.Mover;
import com.nuclearunicorn.libroguelike.utils.pathfinder.astar.TileBasedMap;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.Point;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The two properties in {@link PathCheck}, tested against the shapes that actually turned up:
 * a milestone polyline walked as if it were a route, and a route through a window.
 */
class PathCheckTest {

    private static List<Point> path(int... xy) {
        List<Point> out = new ArrayList<>();
        for (int i = 0; i < xy.length; i += 2) {
            out.add(new Point(xy[i], xy[i + 1]));
        }
        return out;
    }

    /** A map where a named set of tiles is impassable and everything else is open. */
    private static TileBasedMap mapBlocking(Point... blocked) {
        final Set<String> wall = new HashSet<>();
        for (Point p : blocked) {
            wall.add(p.getX() + "," + p.getY());
        }
        return new TileBasedMap() {
            public boolean blocked(Mover mover, int x, int y) { return wall.contains(x + "," + y); }
            public float getCost(Mover mover, int sx, int sy, int tx, int ty) { return 1; }
            public int getWidthInTiles() { return 128; }
            public int getHeightInTiles() { return 128; }
            public int getScaleFactor() { return 1; }
            public void pathFinderVisited(int x, int y) { }
        };
    }

    @Test
    void contiguousPathHasNoGap() {
        assertEquals(-1, PathCheck.firstGap(path(1,1, 2,1, 3,1, 3,2)));
    }

    @Test
    void emptyAndSingleStepPathsAreTrivial() {
        assertEquals(-1, PathCheck.firstGap(new ArrayList<>()));
        assertEquals(-1, PathCheck.firstGap(path(4,4)));
        assertEquals(-1, PathCheck.firstGap(null));
    }

    /** The milestone polyline: two points far apart, presented as consecutive steps. */
    @Test
    void jumpBetweenDistantPointsIsAGap() {
        assertEquals(1, PathCheck.firstGap(path(1,1, 40,60)));
    }

    /** A diagonal is adjacent, so it is not a gap - the milestone legs take them. */
    @Test
    void diagonalStepIsNotAGap() {
        assertEquals(-1, PathCheck.firstGap(path(1,1, 2,1, 3,2)));
    }

    /** But a route out of A* has no business containing one: diagonal movement is off. */
    @Test
    void diagonalStepIsReportedSeparately() {
        assertEquals(2, PathCheck.firstDiagonal(path(1,1, 2,1, 3,2)));
        assertEquals(-1, PathCheck.firstDiagonal(path(1,1, 2,1, 3,1)));
        assertEquals(-1, PathCheck.firstDiagonal(path(1,1)));
        assertEquals(-1, PathCheck.firstDiagonal(null));
    }

    @Test
    void repeatedPointIsAGap() {
        assertEquals(1, PathCheck.firstGap(path(1,1, 1,1, 2,1)));
    }

    @Test
    void clearPathIsNotBlocked() {
        assertEquals(-1, PathCheck.firstBlocked(path(1,1, 2,1, 3,1),
                mapBlocking(new Point(9, 9)), null));
    }

    /** A route through a window: contiguous, and still impassable. */
    @Test
    void stepOntoABlockedTileIsReported() {
        List<Point> route = path(1,1, 2,1, 3,1);
        assertEquals(-1, PathCheck.firstGap(route), "the shape is fine");
        assertEquals(1, PathCheck.firstBlocked(route, mapBlocking(new Point(2, 1)), null));
    }

    @Test
    void firstBlockedReportsTheEarliestOne() {
        assertEquals(1, PathCheck.firstBlocked(path(0,0, 1,0, 2,0),
                mapBlocking(new Point(1, 0), new Point(2, 0)), null));
    }

    @Test
    void nullsAreTolerated() {
        assertEquals(-1, PathCheck.firstBlocked(null, mapBlocking(), null));
        assertEquals(-1, PathCheck.firstBlocked(Arrays.asList(new Point(0, 0)), null, null));
    }
}
