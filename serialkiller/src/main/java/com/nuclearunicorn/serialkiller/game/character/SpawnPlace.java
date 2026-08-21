package com.nuclearunicorn.serialkiller.game.character;

import com.nuclearunicorn.serialkiller.generators.town.BuildingType;
import com.nuclearunicorn.serialkiller.generators.town.RoomType;

/**
 * Where a preset starts the game, expressed as something the town generator can look up
 * once the town exists: a room of a building of some type, or a piece of open ground.
 *
 * <p>{@link #HOME} is the odd one out and the reason this enum is not just a
 * (building, room) pair — the player's own flat is not found after the fact, it is
 * <i>built</i> for them by the generator (the safehouse, with the family in it), so all this
 * has to say is "wherever that turned out to be".
 *
 * <p>A place a given town happens not to contain — the brothel cap is one per chunk, and a
 * small chunk may hold none — resolves back to HOME rather than failing. See
 * {@code TownChunkGenerator.resolvePlayerSpawn}.
 */
public enum SpawnPlace {
    HOME("in your own bed", null, null),
    STREET("out on the street", null, null),
    PARK("in the park", null, null),
    BROTHEL_ROOM("in a room at the brothel", BuildingType.BROTHEL, RoomType.PRIVATE_ROOM),
    SHOP_FLOOR("behind the counter", BuildingType.SHOP, RoomType.SHOP_FLOOR),
    OFFICE_DESK("at your desk", BuildingType.OFFICE, RoomType.OFFICE_ROOM),
    POLICE_LOBBY("in the station lobby", BuildingType.POLICE_STATION, RoomType.LOBBY);

    private final String displayName;
    /** The building to look for, or null when the place is not indoors. */
    public final BuildingType building;
    /** Which of its rooms; only meaningful with {@link #building}. */
    public final RoomType room;

    SpawnPlace(String displayName, BuildingType building, RoomType room) {
        this.displayName = displayName;
        this.building = building;
        this.room = room;
    }

    public String displayName() {
        return displayName;
    }

    /** True when the spot has to be hunted for inside a building of a given type. */
    public boolean isIndoors() {
        return building != null;
    }
}
