package com.nuclearunicorn.serialkiller.generators.town;

import com.nuclearunicorn.serialkiller.generators.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Stage 3 (TOWN_GENERATION_DESIGN.md 5.3): turn a lot into an organic building
 * footprint. The footprint is a union of 1-3 rectangular parts:
 *
 *   - rectangle   : base rect only
 *   - L-shape     : base minus one corner  -> 2 parts
 *   - T/U-shape   : base minus two corners on one side -> 2 parts
 *
 * Carving (rather than additive wings) keeps the shape inside the lot and lets
 * each part stay a clean rectangle for the constrained-BSP room splitter, while
 * adjacent parts always share a wall segment >= 5 tiles so the connectivity
 * pass can join them.
 */
public class FootprintGenerator {

    private static final int CARVE_MIN_DIM = 10;   // base must be this big to carve
    private static final int LIMB_MIN      = 5;    // remaining limb thickness (block size)

    public static Footprint generate(Lot lot, Random rng) {
        int insX = lot.getW() >= 8 ? 1 : 0;   // small side yards; keeps buildings apart
        int insY = lot.getH() >= 8 ? 1 : 0;

        int bx = lot.getX() + insX;
        int by = lot.getY() + insY;
        int bw = lot.getW() - 2 * insX;
        int bh = lot.getH() - 2 * insY;

        Block base = new Block(bx, by, bw, bh);
        List<Block> parts = new ArrayList<Block>();

        boolean canCarve = bw >= CARVE_MIN_DIM && bh >= CARVE_MIN_DIM;
        int roll = rng.nextInt(100);

        if (canCarve && roll < 45) {
            carveCorner(base, rng, parts);          // L
        } else if (canCarve && roll < 80) {
            carveNotch(base, rng, parts);           // T / U
        } else {
            parts.add(base);                        // rectangle
        }

        Footprint fp = build(lot, parts);
        if (fp != null && fp.mask.isConnected() && fp.mask.filledCount() >= 16) {
            return fp;
        }

        // fallback: plain rectangle
        List<Block> single = new ArrayList<Block>();
        single.add(base);
        return build(lot, single);
    }

    private static Footprint build(Lot lot, List<Block> parts) {
        for (Block p : parts) {
            if (p.getW() < LIMB_MIN - 1 || p.getH() < LIMB_MIN - 1) {
                return null;   // degenerate part -> caller falls back
            }
        }
        GridMask mask = new GridMask(lot.getX(), lot.getY(), lot.getW() + 1, lot.getH() + 1);
        for (Block p : parts) {
            mask.fillPart(p, true);
        }
        return new Footprint(mask, parts);
    }

    /** Carve one corner -> L-shape (a full band + a shortened band sharing a wall). */
    private static void carveCorner(Block base, Random rng, List<Block> parts) {
        int bx = base.getX(), by = base.getY(), bw = base.getW(), bh = base.getH();
        int cw = pct(bw, rng, LIMB_MIN, bw - LIMB_MIN);
        int ch = pct(bh, rng, LIMB_MIN, bh - LIMB_MIN);
        int corner = rng.nextInt(4);   // 0=TL 1=TR 2=BL 3=BR

        boolean top = (corner == 0 || corner == 1);
        boolean left = (corner == 0 || corner == 2);

        if (top) {
            // full band along the bottom, shortened band along the top
            parts.add(new Block(bx, by + ch, bw, bh - ch));
            parts.add(left ? new Block(bx + cw, by, bw - cw, ch)
                           : new Block(bx,      by, bw - cw, ch));
        } else {
            // full band along the top, shortened band along the bottom
            parts.add(new Block(bx, by, bw, bh - ch));
            parts.add(left ? new Block(bx + cw, by + bh - ch, bw - cw, ch)
                           : new Block(bx,      by + bh - ch, bw - cw, ch));
        }
    }

    /** Carve two corners on one side -> T/U-shape (a full band + a centred limb). */
    private static void carveNotch(Block base, Random rng, List<Block> parts) {
        int bx = base.getX(), by = base.getY(), bw = base.getW(), bh = base.getH();
        boolean horizontal = rng.nextBoolean();   // notch on a horizontal edge (top/bottom)

        if (horizontal) {
            int ch = pct(bh, rng, LIMB_MIN, bh - LIMB_MIN);
            int side = Math.min(bw / 3, (bw - LIMB_MIN) / 2);
            if (side < 3) { parts.add(base); return; }
            boolean top = rng.nextBoolean();
            if (top) {
                parts.add(new Block(bx, by + ch, bw, bh - ch));            // full bottom band
                parts.add(new Block(bx + side, by, bw - 2 * side, ch));    // centred top limb
            } else {
                parts.add(new Block(bx, by, bw, bh - ch));                 // full top band
                parts.add(new Block(bx + side, by + bh - ch, bw - 2 * side, ch));
            }
        } else {
            int cw = pct(bw, rng, LIMB_MIN, bw - LIMB_MIN);
            int side = Math.min(bh / 3, (bh - LIMB_MIN) / 2);
            if (side < 3) { parts.add(base); return; }
            boolean left = rng.nextBoolean();
            if (left) {
                parts.add(new Block(bx + cw, by, bw - cw, bh));            // full right band
                parts.add(new Block(bx, by + side, cw, bh - 2 * side));    // centred left limb
            } else {
                parts.add(new Block(bx, by, bw - cw, bh));                 // full left band
                parts.add(new Block(bx + bw - cw, by + side, cw, bh - 2 * side));
            }
        }
    }

    /** A value at 25-50% of {@code dim}, clamped to [lo,hi]. */
    private static int pct(int dim, Random rng, int lo, int hi) {
        int v = dim * (25 + rng.nextInt(26)) / 100;
        if (hi < lo) hi = lo;
        if (v < lo) v = lo;
        if (v > hi) v = hi;
        return v;
    }
}
