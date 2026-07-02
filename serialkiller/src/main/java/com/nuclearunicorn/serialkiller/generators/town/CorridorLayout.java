package com.nuclearunicorn.serialkiller.generators.town;

import com.nuclearunicorn.serialkiller.generators.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Stage 4A (TOWN_GENERATION_DESIGN.md 5.4A): a double-loaded corridor spine down
 * a rectangular part's long axis, with room bands sliced perpendicular on both
 * sides. Pure geometry (Block/Room only) so a standalone harness can exercise it.
 *
 * Under the module's inclusive-wall convention three stacked blocks of thickness
 * a, cbh, b with a+cbh+b == crossSize share their dividing wall lines, so each
 * band room ends up wall-adjacent to the corridor and doors can be punched on the
 * shared line. Returns null when the part is too narrow for two 3-tile bands plus
 * the corridor — the caller then falls back to constrained BSP.
 */
public class CorridorLayout {

    private static final int MIN_ROOM_STRIP = 4;   // block width -> interior 3
    private static final int MAX_ROOM_STRIP = 7;   // block width -> interior 6

    public static class Result {
        public final List<Room> rooms = new ArrayList<Room>();
        public Room corridor;
    }

    public static Result layoutPart(Block part, int corridorWidth, Random rng) {
        int px = part.getX(), py = part.getY();
        int w = part.getW(), h = part.getH();

        boolean horizontal = w >= h;          // corridor runs along the long axis
        int cross = horizontal ? h : w;       // short-axis block size
        int cbh = corridorWidth + 1;          // corridor block thickness (interior = corridorWidth)

        //two side bands each need block thickness >= 4 (interior >= 3)
        if (cross < cbh + 2 * MIN_ROOM_STRIP) {
            return null;
        }

        int bandSpan = cross - cbh;           // a + b
        int a = bandSpan / 2;
        if (a < MIN_ROOM_STRIP) a = MIN_ROOM_STRIP;
        if (bandSpan - a < MIN_ROOM_STRIP) a = bandSpan - MIN_ROOM_STRIP;
        int b = bandSpan - a;

        Result res = new Result();

        if (horizontal) {
            sliceAlongX(new Block(px, py, w, a), res.rooms, rng);
            sliceAlongX(new Block(px, py + a + cbh, w, b), res.rooms, rng);
            res.corridor = new Room(new Block(px, py + a, w, cbh));
        } else {
            sliceAlongY(new Block(px, py, a, h), res.rooms, rng);
            sliceAlongY(new Block(px + a + cbh, py, b, h), res.rooms, rng);
            res.corridor = new Room(new Block(px + a, py, cbh, h));
        }
        res.corridor.type = RoomType.CORRIDOR;
        res.rooms.add(res.corridor);
        return res;
    }

    /** Slice a band into rooms along X (rooms share vertical walls, full band height). */
    private static void sliceAlongX(Block band, List<Room> out, Random rng) {
        int x = band.getX(), y = band.getY(), w = band.getW(), h = band.getH();
        int[] cuts = slice(w, rng);
        for (int i = 0; i + 1 < cuts.length; i++) {
            addStrip(new Block(x + cuts[i], y, cuts[i + 1] - cuts[i], h), out, rng);
        }
    }

    /** Slice a band into rooms along Y (rooms share horizontal walls, full band width). */
    private static void sliceAlongY(Block band, List<Room> out, Random rng) {
        int x = band.getX(), y = band.getY(), w = band.getW(), h = band.getH();
        int[] cuts = slice(h, rng);
        for (int i = 0; i + 1 < cuts.length; i++) {
            addStrip(new Block(x, y + cuts[i], w, cuts[i + 1] - cuts[i]), out, rng);
        }
    }

    /**
     * A band strip is one room, unless it is deep enough to exceed the room-size
     * cap — then the constrained splitter cuts it perpendicular to the corridor so
     * every room stays human-scale. The near sub-room still touches the corridor;
     * far ones connect through it in the connectivity pass.
     */
    private static void addStrip(Block strip, List<Room> out, Random rng) {
        List<Block> pieces = new ArrayList<Block>();
        RoomSplitter.split(strip, rng, pieces);
        for (int i = 0; i < pieces.size(); i++) {
            out.add(new Room(pieces.get(i)));
        }
    }

    /**
     * Cumulative cut offsets partitioning [0,total] into strips of block-width
     * MIN..MAX (interior 3..6). The final strip absorbs the remainder, clamped so
     * it never falls below the minimum.
     */
    private static int[] slice(int total, Random rng) {
        List<Integer> offs = new ArrayList<Integer>();
        offs.add(0);
        int pos = 0;
        while (pos < total) {
            int remaining = total - pos;
            if (remaining <= MAX_ROOM_STRIP) {
                pos = total;
                offs.add(pos);
                break;
            }
            int len = MIN_ROOM_STRIP + rng.nextInt(MAX_ROOM_STRIP - MIN_ROOM_STRIP + 1);
            if (remaining - len < MIN_ROOM_STRIP) {
                len = remaining;   // don't strand a sliver
            }
            pos += len;
            offs.add(pos);
        }
        int[] arr = new int[offs.size()];
        for (int i = 0; i < offs.size(); i++) {
            arr[i] = offs.get(i);
        }
        return arr;
    }
}
