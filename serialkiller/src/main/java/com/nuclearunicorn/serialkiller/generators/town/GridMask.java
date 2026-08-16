package com.nuclearunicorn.serialkiller.generators.town;

import com.nuclearunicorn.serialkiller.generators.Block;

/**
 * Boolean occupancy grid local to one lot (TOWN_GENERATION_DESIGN.md 4).
 * Cell (lx,ly) maps to world tile (ox+lx, oy+ly). A set cell means "this tile
 * belongs to the building"; an edge cell (set, with an unset/out-of-bounds
 * 4-neighbour) becomes a wall; a set non-edge cell is interior floor.
 *
 * Plain arrays only, TeaVM-safe.
 */
public class GridMask {
    public final int ox, oy, w, h;   // world-space origin + size (cells)
    private final boolean[] cells;   // row-major, w*h

    public GridMask(int ox, int oy, int w, int h) {
        this.ox = ox;
        this.oy = oy;
        this.w = Math.max(0, w);
        this.h = Math.max(0, h);
        this.cells = new boolean[this.w * this.h];
    }

    public boolean get(int lx, int ly) {
        if (lx < 0 || ly < 0 || lx >= w || ly >= h) {
            return false;
        }
        return cells[ly * w + lx];
    }

    public void set(int lx, int ly, boolean v) {
        if (lx < 0 || ly < 0 || lx >= w || ly >= h) {
            return;
        }
        cells[ly * w + lx] = v;
    }

    /** Set a rectangle of {@code rw x rh} cells starting at (lx,ly). */
    public void fillRect(int lx, int ly, int rw, int rh, boolean v) {
        for (int j = 0; j < rh; j++) {
            for (int i = 0; i < rw; i++) {
                set(lx + i, ly + j, v);
            }
        }
    }

    /**
     * Union a footprint part (a Block, inclusive-wall convention) into this
     * mask. A Block of size (bw,bh) occupies (bw+1)x(bh+1) tiles.
     */
    public void fillPart(Block part, boolean v) {
        fillRect(part.getX() - ox, part.getY() - oy,
                 part.getW() + 1, part.getH() + 1, v);
    }

    /** True if the cell is set and any 4-neighbour is unset/out of bounds. */
    public boolean isEdge(int lx, int ly) {
        if (!get(lx, ly)) {
            return false;
        }
        return !get(lx - 1, ly) || !get(lx + 1, ly)
            || !get(lx, ly - 1) || !get(lx, ly + 1);
    }

    /** Interior floor: set but not on the boundary. */
    public boolean isInterior(int lx, int ly) {
        return get(lx, ly) && !isEdge(lx, ly);
    }

    /** Convenience: WORLD-coord occupancy. */
    public boolean getWorld(int wx, int wy) {
        return get(wx - ox, wy - oy);
    }

    /** Convenience: is the WORLD tile a building-boundary (wall) tile? */
    public boolean isEdgeWorld(int wx, int wy) {
        return isEdge(wx - ox, wy - oy);
    }

    /** Convenience: is the WORLD tile interior floor? */
    public boolean isInteriorWorld(int wx, int wy) {
        return isInterior(wx - ox, wy - oy);
    }

    /** Count of set cells (footprint tile count). */
    public int filledCount() {
        int n = 0;
        for (int i = 0; i < cells.length; i++) {
            if (cells[i]) n++;
        }
        return n;
    }

    /**
     * True if all set cells form a single 4-connected region. Empty masks are
     * trivially connected. Used to validate carved footprints.
     */
    public boolean isConnected() {
        int start = -1;
        int total = 0;
        for (int i = 0; i < cells.length; i++) {
            if (cells[i]) {
                total++;
                if (start < 0) start = i;
            }
        }
        if (total == 0) {
            return true;
        }
        boolean[] seen = new boolean[cells.length];
        int[] stack = new int[cells.length];
        int sp = 0;
        stack[sp++] = start;
        seen[start] = true;
        int visited = 0;
        while (sp > 0) {
            int idx = stack[--sp];
            visited++;
            int cx = idx % w;
            int cy = idx / w;
            if (canPush(seen, cx - 1, cy)) { sp = doPush(stack, seen, sp, cx - 1, cy); }
            if (canPush(seen, cx + 1, cy)) { sp = doPush(stack, seen, sp, cx + 1, cy); }
            if (canPush(seen, cx, cy - 1)) { sp = doPush(stack, seen, sp, cx, cy - 1); }
            if (canPush(seen, cx, cy + 1)) { sp = doPush(stack, seen, sp, cx, cy + 1); }
        }
        return visited == total;
    }

    private boolean canPush(boolean[] seen, int x, int y) {
        if (x < 0 || y < 0 || x >= w || y >= h) return false;
        int idx = y * w + x;
        return cells[idx] && !seen[idx];
    }

    private int doPush(int[] stack, boolean[] seen, int sp, int x, int y) {
        int idx = y * w + x;
        seen[idx] = true;
        stack[sp++] = idx;
        return sp;
    }
}
