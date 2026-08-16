package com.nuclearunicorn.serialkiller.generators.town;

import com.nuclearunicorn.serialkiller.generators.Block;

import java.util.List;

/** Result of the footprint stage: the occupancy mask plus the rectangular parts
 *  that compose it (each part is independently room-split, then connected). */
public class Footprint {
    public final GridMask mask;
    public final List<Block> parts;

    public Footprint(GridMask mask, List<Block> parts) {
        this.mask = mask;
        this.parts = parts;
    }

    public boolean isRectangular() {
        return parts.size() == 1;
    }
}
