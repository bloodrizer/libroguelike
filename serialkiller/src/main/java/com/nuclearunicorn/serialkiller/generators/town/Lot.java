package com.nuclearunicorn.serialkiller.generators.town;

import com.nuclearunicorn.serialkiller.generators.Block;

/**
 * A single building plot carved from a district. Tracks which of its four sides
 * face a street (== lie on the district perimeter) so the footprint stage can
 * orient the entrance and street-facing windows.
 *
 * Side indices: 0=N (y-min), 1=E (x-max), 2=S (y-max), 3=W (x-min).
 */
public class Lot extends Block {
    public static final int N = 0, E = 1, S = 2, W = 3;

    public final boolean[] street = new boolean[4];

    public Lot(int x, int y, int w, int h) {
        super(x, y, w, h);
    }

    public boolean anyStreet() {
        return street[N] || street[E] || street[S] || street[W];
    }
}
