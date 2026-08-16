package com.nuclearunicorn.serialkiller.game.sound;

import com.nuclearunicorn.libroguelike.game.world.WorldTimer;
import com.nuclearunicorn.serialkiller.game.world.RLTile;

/**
 * The local noise floor a sound has to beat to be noticed (SOUND_DESIGN.md 4.4).
 *
 * <p>This is the Splinter Cell idea and the reason the whole system is worth building: you
 * can be loud where the world is loud. A knife on the high street at noon has to clear an
 * ambient of 22 and reaches nobody; the same knife in a stairwell at 3am has to clear 4 and
 * carries six tiles. Time of day and choice of place become acoustic decisions with no
 * special-casing anywhere — it falls out of one subtraction.
 *
 * <p>Resolution is coarser than the spec's table wants: {@code RoomType} would separate a
 * shop floor from a bedroom, but rooms are discarded once the generator has stamped tiles
 * (SOUND_DESIGN.md 11), so indoors is currently one value. The public/brothel constants in
 * {@link SoundConfig} are waiting on room retention.
 */
public final class Ambient {

    private Ambient() {}

    public static int at(RLTile tile) {
        boolean night = WorldTimer.is_night();
        if (tile == null) {
            return night ? SoundConfig.AMB_PARK_NIGHT : SoundConfig.AMB_PARK_DAY;
        }
        if (tile.isIndoor()) {
            return night ? SoundConfig.AMB_INDOOR_NIGHT : SoundConfig.AMB_INDOOR_DAY;
        }
        if (tile.getTileType() == RLTile.TileType.ROAD) {
            return night ? SoundConfig.AMB_STREET_NIGHT : SoundConfig.AMB_STREET_DAY;
        }
        return night ? SoundConfig.AMB_PARK_NIGHT : SoundConfig.AMB_PARK_DAY;
    }

    /**
     * The level a sound must exceed at this tile for this listener to register it: their own
     * threshold, or the ambient plus a just-noticeable difference, whichever is higher.
     */
    public static int threshold(RLTile tile, int listenerThreshold) {
        return Math.max(listenerThreshold, at(tile) + SoundConfig.JND);
    }
}
