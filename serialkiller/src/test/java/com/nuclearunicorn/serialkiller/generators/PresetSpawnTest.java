package com.nuclearunicorn.serialkiller.generators;

import com.nuclearunicorn.libroguelike.core.client.ClientGameEnvironment;
import com.nuclearunicorn.serialkiller.game.character.CharacterPreset;
import com.nuclearunicorn.serialkiller.game.character.CharacterPresets;
import com.nuclearunicorn.serialkiller.game.character.CharacterSetup;
import com.nuclearunicorn.serialkiller.game.character.SpawnPlace;
import com.nuclearunicorn.serialkiller.game.world.RLWorldModel;
import com.nuclearunicorn.serialkiller.generators.town.Building;
import com.nuclearunicorn.serialkiller.generators.town.Room;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.lwjgl.util.Point;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Where a character preset actually puts the player, checked against real towns.
 *
 * <p>The spawn place is a lookup into a town that does not exist until generation is over —
 * "a private room of the brothel" is a query, not a coordinate — and the town may not contain
 * one: the type picker allows at most one brothel per chunk and only on a lot big enough to
 * hold it. So there are exactly two acceptable answers, and this is the test that says a
 * silent third one (nowhere, off-map, in a wall) is not among them.
 */
class PresetSpawnTest {

    static long[] seeds() {
        return TownFixture.seeds();
    }

    @AfterEach
    void restoreDefaultPreset() {
        CharacterSetup.choose(CharacterPresets.citizen());
        CharacterSetup.reset();
    }

    @ParameterizedTest(name = "seed {0}")
    @MethodSource("seeds")
    void theBrothelStartIsInTheBrothel(long seed) {
        CharacterPreset prostitute = CharacterPresets.byId("prostitute");
        CharacterSetup.choose(prostitute);

        //a fresh town, not the shared cache: this one is generated for a different preset
        TownFixture.generate(seed);

        Point spawn = RLWorldModel.playerSpawnLocation;
        Point home = RLWorldModel.playerSafeHouseLocation;
        assertTrue(spawn != null, "seed " + seed + ": the game was given nowhere to start");

        SpawnPlace place = prostitute.getSpawn();
        if (isIn(place, spawn)) {
            return;   //the start it asked for
        }

        //the documented fallback, and only when the town really has no such room
        assertTrue(home != null && home.getX() == spawn.getX() && home.getY() == spawn.getY(),
                () -> "seed " + seed + ": started at " + spawn + ", which is neither a "
                        + place + " nor home (" + home + ")");
        assertTrue(!townHas(place), () -> "seed " + seed + ": this town has a " + place
                + ", but the prostitute was sent home anyway");
    }

    /** True if the point is an interior tile of a room the place describes. */
    private static boolean isIn(SpawnPlace place, Point spot) {
        for (Room room : roomsOf(place)) {
            if (spot.getX() > room.getX() && spot.getX() < room.getX() + room.getW()
             && spot.getY() > room.getY() && spot.getY() < room.getY() + room.getH()) {
                return true;
            }
        }
        return false;
    }

    private static boolean townHas(SpawnPlace place) {
        return !roomsOf(place).isEmpty();
    }

    private static java.util.List<Room> roomsOf(SpawnPlace place) {
        java.util.List<Room> rooms = new java.util.ArrayList<Room>();
        RLWorldModel model = (RLWorldModel) ClientGameEnvironment.getEnvironment().getWorld();

        for (Apartment apt : model.getApartments()) {
            if (!(apt instanceof Building) || ((Building) apt).type != place.building) {
                continue;
            }
            for (Room room : ((Building) apt).roomList) {
                if (room.type == place.room) {
                    rooms.add(room);
                }
            }
        }
        return rooms;
    }
}
