package com.nuclearunicorn.serialkiller.generators.town;

import java.util.List;

/**
 * A data-driven recipe for one {@link BuildingType} (TOWN_GENERATION_DESIGN.md 4
 * & 6): minimum lot size to fit, spawn weight, whether it uses a corridor-spine
 * interior, the room specs, and the filler type used for leftover rooms.
 */
public class BuildingTemplate {
    public final BuildingType type;
    public final int minLotW, minLotH;   // reject lots smaller than this
    public final float weight;           // weighted-pick probability
    public final boolean hasCorridor;    // corridor-spine (true) vs open/BSP (false)
    public final List<RoomSpec> rooms;
    public final RoomType filler;        // leftover rooms get this type

    public BuildingTemplate(BuildingType type, int minLotW, int minLotH, float weight,
                            boolean hasCorridor, List<RoomSpec> rooms, RoomType filler) {
        this.type = type;
        this.minLotW = minLotW;
        this.minLotH = minLotH;
        this.weight = weight;
        this.hasCorridor = hasCorridor;
        this.rooms = rooms;
        this.filler = filler;
    }

    public boolean fitsLot(int lotW, int lotH) {
        //allow either orientation to satisfy the min footprint
        boolean direct = lotW >= minLotW && lotH >= minLotH;
        boolean rotated = lotW >= minLotH && lotH >= minLotW;
        return direct || rotated;
    }
}
