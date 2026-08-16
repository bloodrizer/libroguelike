package com.nuclearunicorn.serialkiller.generators.town;

import com.nuclearunicorn.serialkiller.generators.Block;
import org.lwjgl.util.Point;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Constrained BSP room subdivision (TOWN_GENERATION_DESIGN.md 5.4B) plus the
 * shared-wall door-candidate helper used by the connectivity pass (5.5).
 *
 * All geometry uses the module's inclusive-wall convention: a Block of size
 * (w,h) occupies tiles [x..x+w] x [y..y+h] and adjacent blocks from a split
 * share the dividing wall line. Interior (floor) area is therefore
 * (w-1)*(h-1); a room needs interior dim >= MIN_ROOM_DIM on both axes.
 *
 * Pure geometry only (Block + Point + java.util) so it can be exercised by a
 * standalone harness without booting the engine.
 */
public class RoomSplitter {

    /** Interior floor area of a room rect. */
    public static int interiorArea(Block b) {
        return Math.max(0, b.getW() - 1) * Math.max(0, b.getH() - 1);
    }

    /** Long/short interior aspect ratio (>= 1). */
    public static float aspect(Block b) {
        int iw = Math.max(1, b.getW() - 1);
        int ih = Math.max(1, b.getH() - 1);
        return (float) Math.max(iw, ih) / (float) Math.min(iw, ih);
    }

    /**
     * Recursively split {@code room} until every leaf fits MAX_ROOM_AREA and
     * MAX_ROOM_ASPECT, appending leaves to {@code out}. Never produces a room
     * whose interior dimension drops below MIN_ROOM_DIM.
     */
    public static void split(Block room, Random rng, List<Block> out) {
        if (interiorArea(room) <= TownGenConfig.MAX_ROOM_AREA
                && aspect(room) <= TownGenConfig.MAX_ROOM_ASPECT) {
            out.add(room);
            return;
        }

        // Prefer splitting the longer axis. A "vertical" split divides width.
        boolean splitVertical = room.getW() >= room.getH();

        Block[] halves = trySplit(room, rng, splitVertical);
        if (halves == null) {
            halves = trySplit(room, rng, !splitVertical);
        }
        if (halves == null) {
            out.add(room);
            return;
        }
        split(halves[0], rng, out);
        split(halves[1], rng, out);
    }

    /**
     * Split at 40-60%, clamped so both halves keep interior dim >= MIN_ROOM_DIM.
     * Returns null if the room is too small to split on this axis.
     */
    private static Block[] trySplit(Block room, Random rng, boolean vertical) {
        int min = TownGenConfig.MIN_ROOM_DIM;   // interior tiles per half
        // A half of block-size s has interior (s-1); we need s-1 >= min => s >= min+1.
        // Split offset `off` yields halves of size `off` and `size-off`, so
        // off must lie in [min+1, size-(min+1)].
        int lo = min + 1;

        if (vertical) {
            int w = room.getW();
            int hi = w - (min + 1);
            if (hi < lo) return null;
            int off = clampToRange(pct40to60(w, rng), lo, hi);
            Block left  = new Block(room.getX(),       room.getY(), off,     room.getH());
            Block right = new Block(room.getX() + off,  room.getY(), w - off, room.getH());
            return new Block[]{left, right};
        } else {
            int h = room.getH();
            int hi = h - (min + 1);
            if (hi < lo) return null;
            int off = clampToRange(pct40to60(h, rng), lo, hi);
            Block top    = new Block(room.getX(), room.getY(),       room.getW(), off);
            Block bottom = new Block(room.getX(), room.getY() + off, room.getW(), h - off);
            return new Block[]{top, bottom};
        }
    }

    private static int pct40to60(int size, Random rng) {
        return (int) (size * ((40 + rng.nextInt(21)) / 100.0f));
    }

    private static int clampToRange(int v, int lo, int hi) {
        if (v < lo) return lo;
        if (v > hi) return hi;
        return v;
    }

    /**
     * Candidate door tiles on the wall shared by two adjacent rooms, excluding
     * the corner tiles (a door must have a room-interior tile on both sides).
     * Empty if the rooms are not wall-adjacent or share too short a segment.
     */
    public static List<Point> sharedWallDoors(Block a, Block b) {
        List<Point> res = new ArrayList<Point>();

        int ax0 = a.getX(), ay0 = a.getY();
        int ax1 = a.getX() + a.getW(), ay1 = a.getY() + a.getH();
        int bx0 = b.getX(), by0 = b.getY();
        int bx1 = b.getX() + b.getW(), by1 = b.getY() + b.getH();

        // Vertical shared wall (a common column).
        int wallX = -1;
        if (ax1 == bx0)      wallX = ax1;
        else if (bx1 == ax0) wallX = ax0;
        if (wallX != -1) {
            int ylo = Math.max(ay0, by0);
            int yhi = Math.min(ay1, by1);
            for (int y = ylo + 1; y <= yhi - 1; y++) {
                res.add(new Point(wallX, y));
            }
            return res;
        }

        // Horizontal shared wall (a common row).
        int wallY = -1;
        if (ay1 == by0)      wallY = ay1;
        else if (by1 == ay0) wallY = ay0;
        if (wallY != -1) {
            int xlo = Math.max(ax0, bx0);
            int xhi = Math.min(ax1, bx1);
            for (int x = xlo + 1; x <= xhi - 1; x++) {
                res.add(new Point(x, wallY));
            }
            return res;
        }

        return res;
    }
}
