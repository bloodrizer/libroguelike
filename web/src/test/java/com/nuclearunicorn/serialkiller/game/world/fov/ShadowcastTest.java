package com.nuclearunicorn.serialkiller.game.world.fov;

import org.junit.jupiter.api.Test;
import rlforj.los.ILosBoard;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Shadowcast stands in for rlforj's PrecisePermissive in the browser build, so
 * it has to behave like a field of view and not merely avoid throwing. These run
 * on the JVM: the class is plain arithmetic with no TeaVM dependency.
 */
class ShadowcastTest {

    private static final int N = 41;
    private static final int C = 20;

    /** Square board; walls are whatever the caller marks. */
    private static final class Board implements ILosBoard {
        final boolean[][] wall = new boolean[N][N];
        final boolean[][] seen = new boolean[N][N];

        @Override
        public boolean contains(int x, int y) {
            return x >= 0 && y >= 0 && x < N && y < N;
        }

        @Override
        public boolean isObstacle(int x, int y) {
            return contains(x, y) && wall[x][y];
        }

        @Override
        public void visit(int x, int y) {
            if (contains(x, y)) {
                seen[x][y] = true;
            }
        }
    }

    private static Board run(int radius, java.util.function.Consumer<Board> setup) {
        Board b = new Board();
        setup.accept(b);
        new Shadowcast().visitFieldOfView(b, C, C, radius);
        return b;
    }

    @Test
    void seesItsOwnTile() {
        Board b = run(5, x -> { });
        assertTrue(b.seen[C][C], "the origin is always visible");
    }

    @Test
    void staysWithinRadius() {
        int r = 6;
        Board b = run(r, x -> { });
        for (int x = 0; x < N; x++) {
            for (int y = 0; y < N; y++) {
                if (b.seen[x][y]) {
                    int dx = x - C;
                    int dy = y - C;
                    assertTrue(dx * dx + dy * dy <= r * r,
                            "visited (" + x + "," + y + ") outside radius " + r);
                }
            }
        }
    }

    @Test
    void openGroundIsBroadlyVisible() {
        int r = 6;
        Board b = run(r, x -> { });
        int count = 0;
        for (boolean[] row : b.seen) {
            for (boolean v : row) {
                if (v) {
                    count++;
                }
            }
        }
        // A disc of radius 6 holds ~113 cells; allow slack for edge rounding.
        assertTrue(count > 90, "open ground should be widely visible, saw " + count);
    }

    @Test
    void wallCastsAShadowBehindIt() {
        // A wall segment directly east of the viewer hides what is behind it.
        Board b = run(10, x -> {
            for (int y = C - 2; y <= C + 2; y++) {
                x.wall[C + 3][y] = true;
            }
        });
        assertTrue(b.seen[C + 3][C], "the wall itself is lit");
        assertFalse(b.seen[C + 6][C], "the tile straight behind the wall is hidden");
    }

    @Test
    void sealedRoomShowsOnlyItsOwnWalls() {
        // Viewer inside a closed 5x5 box: nothing outside the box may be seen.
        Board b = run(12, x -> {
            for (int i = C - 2; i <= C + 2; i++) {
                x.wall[i][C - 2] = true;
                x.wall[i][C + 2] = true;
                x.wall[C - 2][i] = true;
                x.wall[C + 2][i] = true;
            }
        });
        for (int x = 0; x < N; x++) {
            for (int y = 0; y < N; y++) {
                boolean insideBox = x >= C - 2 && x <= C + 2 && y >= C - 2 && y <= C + 2;
                if (b.seen[x][y]) {
                    assertTrue(insideBox,
                            "saw (" + x + "," + y + ") through a sealed wall");
                }
            }
        }
    }

    @Test
    void doorwayLetsSightThrough() {
        // Same box with one gap: the tile beyond the gap is reachable by sight.
        Board b = run(12, x -> {
            for (int i = C - 2; i <= C + 2; i++) {
                x.wall[i][C - 2] = true;
                x.wall[i][C + 2] = true;
                x.wall[C - 2][i] = true;
                x.wall[C + 2][i] = true;
            }
            x.wall[C + 2][C] = false;   // doorway due east
        });
        assertTrue(b.seen[C + 4][C], "sight should pass through the doorway");
    }

    @Test
    void zeroRadiusSeesOnlyTheOrigin() {
        Board b = run(0, x -> { });
        assertTrue(b.seen[C][C]);
        assertFalse(b.seen[C + 1][C], "radius 0 sees nothing but its own tile");
    }
}
