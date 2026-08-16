package com.nuclearunicorn.serialkiller.game.sound;

import org.lwjgl.util.Point;

/**
 * The result of one flood: how much of a sound survives to each tile, and which way it got
 * there (SOUND_DESIGN.md 5).
 *
 * <p>A dense square window centred on the source, sized to the audible envelope, so a
 * footstep allocates 81 cells and a gunshot 6561. Unreachable and out-of-budget cells hold
 * {@link #INAUDIBLE}.
 */
public class SoundField {

    public static final int INAUDIBLE = Integer.MAX_VALUE;

    /** Source level, before any loss. */
    public final int loudness;
    public final int originX;
    public final int originY;
    public final int layerId;

    /** Window origin (top-left) in world tile coords, and its side length. */
    public final int x0;
    public final int y0;
    public final int size;

    /** Accumulated loss per cell, {@link #INAUDIBLE} where the flood never reached. */
    final int[] loss;

    /** Direction the wavefront entered each cell from; -1 at the source and where unset. */
    final byte[] dir;

    SoundField(int loudness, int originX, int originY, int layerId,
               int x0, int y0, int size, int[] loss, byte[] dir) {
        this.loudness = loudness;
        this.originX = originX;
        this.originY = originY;
        this.layerId = layerId;
        this.x0 = x0;
        this.y0 = y0;
        this.size = size;
        this.loss = loss;
        this.dir = dir;
    }

    private int index(int x, int y) {
        int lx = x - x0;
        int ly = y - y0;
        if (lx < 0 || ly < 0 || lx >= size || ly >= size) {
            return -1;
        }
        return ly * size + lx;
    }

    /** Level arriving at this tile in dB, or {@link Integer#MIN_VALUE} if nothing does. */
    public int received(int x, int y) {
        int at = index(x, y);
        if (at < 0 || loss[at] == INAUDIBLE) {
            return Integer.MIN_VALUE;
        }
        return loudness - loss[at];
    }

    public int received(Point p) {
        return received(p.getX(), p.getY());
    }

    /** Step back along the path toward the source, as an index into {@link Acoustics#DX}. */
    public int directionAt(int x, int y) {
        int at = index(x, y);
        return at < 0 ? -1 : dir[at];
    }

    public boolean contains(int x, int y) {
        return index(x, y) >= 0;
    }

    /** Cells the flood actually reached — the cost measure the budget test asserts on. */
    public int visitedCells() {
        int n = 0;
        for (int i = 0; i < loss.length; i++) {
            if (loss[i] != INAUDIBLE) {
                n++;
            }
        }
        return n;
    }

    /**
     * Walk the direction field from a tile back to the source.
     *
     * <p>This is the investigate-path, and it comes free with the flood: it is by
     * construction the route the sound took, so following it leads through the doorway the
     * noise came out of rather than into the wall it happened behind.
     *
     * @return tiles from {@code (x,y)} to the source inclusive, or null if not audible here
     */
    public java.util.List<Point> pathToSource(int x, int y) {
        if (received(x, y) == Integer.MIN_VALUE) {
            return null;
        }
        java.util.List<Point> path = new java.util.ArrayList<Point>();
        int cx = x;
        int cy = y;
        // The field is a shortest-path tree, so this cannot cycle; the bound is belt and
        // braces against a corrupt dir[] rather than an expected case.
        for (int guard = 0; guard <= size * size; guard++) {
            path.add(new Point(cx, cy));
            if (cx == originX && cy == originY) {
                return path;
            }
            int d = directionAt(cx, cy);
            if (d < 0) {
                return path;
            }
            cx += Acoustics.DX[d];
            cy += Acoustics.DY[d];
        }
        return path;
    }
}
