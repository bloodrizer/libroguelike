package com.nuclearunicorn.serialkiller.generators.town;

import com.nuclearunicorn.serialkiller.generators.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Stage 2 (TOWN_GENERATION_DESIGN.md 5.2): subdivide a district into building
 * lots, each of which touches at least one district edge (street frontage).
 *
 * Slice the long axis into strips of width LOT_MIN_STRIP..LOT_MAX_STRIP, then
 * halve any strip whose cross dimension is large (> 24) so lots stay plot-sized.
 */
public class LotSplitter {

    private static final int SPLIT_THRESHOLD = 14;   // districts smaller than this stay one lot

    public static List<Lot> splitIntoLots(Block district, Random rng) {
        List<Lot> lots = new ArrayList<Lot>();

        if (district.getW() < SPLIT_THRESHOLD || district.getH() < SPLIT_THRESHOLD) {
            lots.add(makeLot(district, district.getX(), district.getY(),
                              district.getW(), district.getH()));
            return lots;
        }

        boolean longX = district.getW() >= district.getH();
        int along = longX ? district.getW() : district.getH();
        int cross = longX ? district.getH() : district.getW();

        List<int[]> segs = sliceSegments(along, rng);   // [offset, length]

        int min = TownGenConfig.MIN_LOT_DIM;
        boolean canHalve = cross >= 2 * min;   // only halve if BOTH halves clear the floor

        for (int[] seg : segs) {
            int off = seg[0];
            int len = seg[1];

            if (canHalve) {
                int half = cross / 2 + rng.nextInt(3) - 1;   // ~50% with a little jitter
                if (half < min) half = min;
                if (cross - half < min) half = cross - min;
                if (longX) {
                    lots.add(makeLot(district, district.getX() + off, district.getY(), len, half));
                    lots.add(makeLot(district, district.getX() + off, district.getY() + half, len, cross - half));
                } else {
                    lots.add(makeLot(district, district.getX(), district.getY() + off, half, len));
                    lots.add(makeLot(district, district.getX() + half, district.getY() + off, cross - half, len));
                }
            } else {
                if (longX) {
                    lots.add(makeLot(district, district.getX() + off, district.getY(), len, cross));
                } else {
                    lots.add(makeLot(district, district.getX(), district.getY() + off, cross, len));
                }
            }
        }

        return lots;
    }

    /** Partition [0,total) into contiguous strips of length MIN..MAX (last strip absorbs the remainder). */
    private static List<int[]> sliceSegments(int total, Random rng) {
        int min = TownGenConfig.LOT_MIN_STRIP;
        int max = TownGenConfig.LOT_MAX_STRIP;

        List<int[]> segs = new ArrayList<int[]>();
        int pos = 0;
        while (pos < total) {
            int remaining = total - pos;
            if (remaining <= max) {
                segs.add(new int[]{pos, remaining});
                break;
            }
            int len = min + rng.nextInt(max - min + 1);
            if (remaining - len < min) {
                len = remaining;   // don't strand a sliver; take the rest
            }
            segs.add(new int[]{pos, len});
            pos += len;
            if (len == remaining) {
                break;
            }
        }
        return segs;
    }

    private static Lot makeLot(Block district, int x, int y, int w, int h) {
        Lot lot = new Lot(x, y, w, h);
        lot.street[Lot.N] = (y == district.getY());
        lot.street[Lot.W] = (x == district.getX());
        lot.street[Lot.S] = (y + h == district.getY() + district.getH());
        lot.street[Lot.E] = (x + w == district.getX() + district.getW());
        return lot;
    }
}
