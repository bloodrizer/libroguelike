package com.nuclearunicorn.serialkiller.generators.town;

/**
 * One row in a {@link BuildingTemplate}: how many rooms of a given type a
 * building wants, their target interior area, and layout hints
 * (TOWN_GENERATION_DESIGN.md 4). Plain data, no logic.
 */
public class RoomSpec {
    public final RoomType type;
    public final int minCount, maxCount;
    public final int minArea, maxArea;   // interior tiles
    public final boolean wantsWindow;    // prefer an exterior wall
    public final boolean onCorridor;     // must open onto the corridor/lobby

    public RoomSpec(RoomType type, int minCount, int maxCount,
                    int minArea, int maxArea, boolean wantsWindow, boolean onCorridor) {
        this.type = type;
        this.minCount = minCount;
        this.maxCount = maxCount;
        this.minArea = minArea;
        this.maxArea = maxArea;
        this.wantsWindow = wantsWindow;
        this.onCorridor = onCorridor;
    }

    /** Midpoint of the target interior-area band; used for best-fit assignment. */
    public int midArea() {
        return (minArea + maxArea) / 2;
    }
}
