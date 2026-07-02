package com.nuclearunicorn.serialkiller.generators.town;

import com.nuclearunicorn.serialkiller.generators.Block;

/**
 * A single interior room (TOWN_GENERATION_DESIGN.md 4). Reuses {@link Block}'s
 * inclusive-wall rect in WORLD coordinates; {@link #type} is null until the
 * template assignment pass tags it. {@code Room extends Block} so rooms can live
 * in {@code Apartment.rooms} (a {@code List<Block>}) untouched.
 */
public class Room extends Block {
    public RoomType type;   // null == not yet typed (apartments leave it null)

    public Room(int x, int y, int w, int h) {
        super(x, y, w, h);
    }

    public Room(Block b) {
        super(b.getX(), b.getY(), b.getW(), b.getH());
    }

    public boolean isCorridor() {
        return type == RoomType.CORRIDOR;
    }

    /** Interior floor area under the inclusive-wall convention. */
    public int interiorArea() {
        return Math.max(0, getW() - 1) * Math.max(0, getH() - 1);
    }
}
