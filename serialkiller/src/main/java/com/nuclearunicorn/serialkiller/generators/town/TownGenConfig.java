package com.nuclearunicorn.serialkiller.generators.town;

/**
 * Single home for town-generation tuning constants. See TOWN_GENERATION_DESIGN.md
 * section 8. Kept as plain static fields (no enums/streams) for TeaVM.
 */
public class TownGenConfig {
    public static int   DISTRICT_MIN_AREA   = 1000;  // was 1800
    public static int   ROAD_SIZE           = 3;     // unchanged
    public static int   LOT_MIN_STRIP       = 12;
    public static int   LOT_MAX_STRIP       = 20;
    public static int   MIN_LOT_DIM         = 12;   // no lot smaller than this on either axis
    public static int   MIN_ROOM_DIM        = 3;     // interior tiles
    public static int   MAX_ROOM_AREA       = 50;    // interior tiles (BSP clamp)
    public static float MAX_ROOM_ASPECT     = 2.5f;
    public static int   CORRIDOR_WIDTH      = 2;
    public static int   WINDOW_SPACING      = 3;
    public static int   LAMPPOST_SPACING    = 9;
    public static int   PARK_DISTRICT_PCT   = 20;
    public static int   NPC_PER_ROAD_RATE   = 35;    // unchanged
    public static int   MAX_POLICEMAN_COUNT = 4;     // unchanged
}
