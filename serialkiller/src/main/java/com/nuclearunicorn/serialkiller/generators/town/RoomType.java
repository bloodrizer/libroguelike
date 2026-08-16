package com.nuclearunicorn.serialkiller.generators.town;

/**
 * Room roles (TOWN_GENERATION_DESIGN.md 4). Drives interior layout (which rooms
 * open onto the corridor) and furnishing (which entity goes where). CORRIDOR is
 * special: it must stay walkable and is placed by the corridor-spine layout, not
 * by template assignment.
 */
public enum RoomType {
    // residential
    KITCHEN, BEDROOM, BATHROOM, STOREROOM, LIVING_ROOM,
    // commercial / civic
    CORRIDOR, LOBBY, OFFICE_ROOM, VAULT, MANAGER_OFFICE,
    SHOP_FLOOR, BACKROOM, PRIVATE_ROOM, CELL, RECEPTION;

    /** Rooms that must never get an exterior window (privacy / security). */
    public boolean allowsWindow() {
        return this != VAULT && this != CELL && this != BATHROOM;
    }

    /** Rooms that can host a building's main street entrance. */
    public boolean isEntranceRoom() {
        return this == LOBBY || this == RECEPTION || this == SHOP_FLOOR
            || this == LIVING_ROOM;
    }

    /** Doors into these rooms are locked with a reinforced (high-hp) door. */
    public boolean isSecure() {
        return this == VAULT || this == CELL;
    }
}
