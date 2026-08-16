package com.nuclearunicorn.serialkiller.game.world.fov;

import rlforj.los.IFovAlgorithm;
import rlforj.los.ILosBoard;

/**
 * Recursive shadowcasting field of view, in the shape rlforj's IFovAlgorithm expects.
 *
 * Written for the browser build because rlforj's own PrecisePermissive does not
 * survive TeaVM (see {@link FovFactory}). Self-contained and allocation-free per
 * call: eight octant sweeps over slope spans, no collections, and recursion no
 * deeper than the radius.
 */
public final class Shadowcast implements IFovAlgorithm {

    /** Octant transforms: xx, xy, yx, yy. */
    private static final int[][] OCTANTS = {
            {1, 0, 0, 1}, {0, 1, 1, 0},
            {0, -1, 1, 0}, {-1, 0, 0, 1},
            {-1, 0, 0, -1}, {0, -1, -1, 0},
            {0, 1, -1, 0}, {1, 0, 0, -1}
    };

    @Override
    public void visitFieldOfView(ILosBoard board, int x, int y, int distance) {
        if (board == null || distance < 0) {
            return;
        }
        if (board.contains(x, y)) {
            board.visit(x, y);
        }
        for (int[] o : OCTANTS) {
            scan(board, x, y, distance, 1, 1.0, 0.0, o[0], o[1], o[2], o[3]);
        }
    }

    /**
     * Sweeps one octant row by row, narrowing the visible slope span each time a
     * wall is crossed and recursing for the strip a wall's near edge cuts off.
     */
    private void scan(ILosBoard board, int cx, int cy, int radius, int row,
                      double startSlope, double endSlope,
                      int xx, int xy, int yx, int yy) {
        if (startSlope < endSlope) {
            return;
        }
        int radius2 = radius * radius;
        boolean blockedRun = false;
        double nextStart = startSlope;

        for (int i = row; i <= radius && !blockedRun; i++) {
            int dy = -i;
            for (int dx = -i; dx <= 0; dx++) {
                // Slopes of this cell's leading and trailing edges.
                double lSlope = (dx - 0.5) / (dy + 0.5);
                double rSlope = (dx + 0.5) / (dy - 0.5);

                if (rSlope > nextStart) {
                    continue;
                }
                if (lSlope < endSlope) {
                    break;
                }

                int mx = cx + dx * xx + dy * xy;
                int my = cy + dx * yx + dy * yy;

                if (dx * dx + dy * dy <= radius2 && board.contains(mx, my)) {
                    board.visit(mx, my);
                }

                boolean wall = !board.contains(mx, my) || board.isObstacle(mx, my);

                if (blockedRun) {
                    if (wall) {
                        nextStart = rSlope;
                    } else {
                        blockedRun = false;
                        startSlope = nextStart;
                    }
                } else if (wall && i < radius) {
                    blockedRun = true;
                    scan(board, cx, cy, radius, i + 1, startSlope, lSlope, xx, xy, yx, yy);
                    nextStart = rSlope;
                }
            }
        }
    }
}
