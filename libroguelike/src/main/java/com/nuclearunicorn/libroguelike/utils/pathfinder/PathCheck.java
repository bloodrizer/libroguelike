package com.nuclearunicorn.libroguelike.utils.pathfinder;

import com.nuclearunicorn.libroguelike.utils.pathfinder.astar.Mover;
import com.nuclearunicorn.libroguelike.utils.pathfinder.astar.TileBasedMap;
import org.lwjgl.util.Point;

import java.util.List;

/**
 * What it means for a route to be a route. Two properties, both of which a real path has and
 * neither of which the code checked:
 *
 * <ol>
 *   <li><b>Contiguous</b> — consecutive steps are neighbours. A path with a gap in it is not
 *       walked, it is <i>flown</i>: the mover heads for the next point in a straight line and
 *       goes through whatever is in between. Every wild cross-country route came from here.</li>
 *   <li><b>Clear</b> — no step stands on a tile the pathfinder itself calls blocked. When
 *       this fails the pathfinder is lying to itself, which is worth knowing immediately
 *       rather than after the NPC has broken a window to make the step legal.</li>
 * </ol>
 *
 * Pure functions on purpose: they are the two assertions worth having in a unit test, and
 * the two worth running live behind {@code -Ddebug.path=validate}.
 */
public final class PathCheck {

    private PathCheck() {}

    /**
     * Index of the first step that is not adjacent to the one before it, or -1 if the path is
     * contiguous. A repeated point counts: a step to where you already stand is a wasted turn,
     * not a move.
     *
     * <p>Diagonals are allowed here because the milestone legs are straight lines and take
     * them. Whether <i>this</i> path is entitled to one is a separate question — see
     * {@link #firstDiagonal}.
     */
    public static int firstGap(List<Point> path) {
        if (path == null || path.size() < 2) {
            return -1;
        }
        for (int i = 1; i < path.size(); i++) {
            Point a = path.get(i - 1);
            Point b = path.get(i);
            int dx = Math.abs(a.getX() - b.getX());
            int dy = Math.abs(a.getY() - b.getY());
            if (Math.max(dx, dy) != 1) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Index of the first diagonal step, or -1. A path that came out of A* must not contain
     * one — the pathfinder runs with diagonal movement off, so a diagonal in its output means
     * the route was assembled by something else and never checked against the map.
     */
    public static int firstDiagonal(List<Point> path) {
        if (path == null || path.size() < 2) {
            return -1;
        }
        for (int i = 1; i < path.size(); i++) {
            Point a = path.get(i - 1);
            Point b = path.get(i);
            if (a.getX() != b.getX() && a.getY() != b.getY()) {
                return i;
            }
        }
        return -1;
    }

    /** Index of the first step the map calls blocked, or -1 if every step is clear. */
    public static int firstBlocked(List<Point> path, TileBasedMap map, Mover mover) {
        if (path == null || map == null) {
            return -1;
        }
        for (int i = 0; i < path.size(); i++) {
            Point p = path.get(i);
            if (map.blocked(mover, p.getX(), p.getY())) {
                return i;
            }
        }
        return -1;
    }
}
