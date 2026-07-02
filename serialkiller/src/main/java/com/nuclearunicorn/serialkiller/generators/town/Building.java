package com.nuclearunicorn.serialkiller.generators.town;

import com.nuclearunicorn.serialkiller.generators.Apartment;
import com.nuclearunicorn.serialkiller.generators.Block;
import org.lwjgl.util.Point;

import java.util.ArrayList;
import java.util.List;

/**
 * A generated building (TOWN_GENERATION_DESIGN.md 4). Extends {@link Apartment}
 * so every existing call site — ownership stamping, NPC.setApartment, sleep AI,
 * bed lists, safehouse flow — keeps compiling. Its Block rect is the lot bounds;
 * the actual (possibly non-rectangular) shape lives in {@link #footprint}.
 */
public class Building extends Apartment {
    public BuildingType type = BuildingType.APARTMENT;
    public GridMask footprint;
    public List<Block> parts = new ArrayList<Block>();   // rects composing the footprint
    public Point entrance;                                // world coords of main door
    public int residentCount = 0;                         // owners stamped in populateMap

    public Building(Block lot) {
        super(lot);
    }
}
