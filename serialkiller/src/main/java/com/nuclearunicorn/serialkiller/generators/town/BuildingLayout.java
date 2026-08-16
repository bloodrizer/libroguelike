package com.nuclearunicorn.serialkiller.generators.town;

import com.nuclearunicorn.serialkiller.generators.Block;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Stage 4 orchestration (TOWN_GENERATION_DESIGN.md 5.4): turn a building's
 * footprint parts into typed {@link Room}s. Corridor-spine templates try a
 * double-loaded corridor per part (falling back to constrained BSP when a part
 * is too small); open-plan templates use BSP throughout. A best-fit pass then
 * tags the non-corridor rooms from the template's {@link RoomSpec} list.
 *
 * Pure geometry — no engine references — so the Phase 3 harness can validate the
 * room mix (corridor present, vault present, sizes in range) without booting.
 */
public class BuildingLayout {

    /** Build all rooms for a building and assign their types. */
    public static List<Room> layout(BuildingTemplate template, List<Block> parts, Random rng) {
        List<Room> rooms = new ArrayList<Room>();

        for (int i = 0; i < parts.size(); i++) {
            Block part = parts.get(i);
            boolean laid = false;

            if (template.hasCorridor) {
                CorridorLayout.Result r = CorridorLayout.layoutPart(part, TownGenConfig.CORRIDOR_WIDTH, rng);
                if (r != null) {
                    rooms.addAll(r.rooms);
                    laid = true;
                }
            }
            if (!laid) {
                List<Block> bsp = new ArrayList<Block>();
                RoomSplitter.split(part, rng, bsp);
                for (int j = 0; j < bsp.size(); j++) {
                    rooms.add(new Room(bsp.get(j)));
                }
            }
        }

        assignTypes(template, rooms, rng);
        return rooms;
    }

    /**
     * Priority type assignment (TOWN_GENERATION_DESIGN.md 5.4B tail). Rooms are
     * sorted by area desc; then, working from both ends of that list:
     *   - entrance rooms (LOBBY/RECEPTION/SHOP_FLOOR) claim the largest rooms,
     *   - secure rooms (VAULT/CELL) claim the smallest rooms,
     *   - other mandatory rooms take the next-largest.
     * Assigning entrances and secure rooms before the "nice-to-have" mandatory
     * rooms guarantees a bank keeps its vault and a station its cells even when
     * the footprint yields only a handful of rooms. Leftover capacity fills each
     * spec up to maxCount, then the template filler. CORRIDOR rooms are untouched.
     */
    private static void assignTypes(BuildingTemplate template, List<Room> allRooms, Random rng) {
        List<Room> pool = new ArrayList<Room>();
        for (int i = 0; i < allRooms.size(); i++) {
            Room r = allRooms.get(i);
            if (r.type != RoomType.CORRIDOR) {
                pool.add(r);
            }
        }
        Collections.sort(pool, new Comparator<Room>() {
            public int compare(Room a, Room b) {
                return b.interiorArea() - a.interiorArea();
            }
        });

        //split mandatory slots into front (entrance), back (secure), mid (rest)
        List<RoomType> front = new ArrayList<RoomType>();
        List<RoomType> back = new ArrayList<RoomType>();
        List<RoomType> mid = new ArrayList<RoomType>();
        for (int i = 0; i < template.rooms.size(); i++) {
            RoomSpec s = template.rooms.get(i);
            for (int c = 0; c < s.minCount; c++) {
                if (s.type.isEntranceRoom()) front.add(s.type);
                else if (s.type.isSecure()) back.add(s.type);
                else mid.add(s.type);
            }
        }

        int lo = 0, hi = pool.size() - 1;
        for (int i = 0; i < front.size() && lo <= hi; i++) {   //biggest rooms
            pool.get(lo++).type = front.get(i);
        }
        for (int i = 0; i < back.size() && lo <= hi; i++) {    //smallest rooms
            pool.get(hi--).type = back.get(i);
        }
        for (int i = 0; i < mid.size() && lo <= hi; i++) {     //next biggest
            pool.get(lo++).type = mid.get(i);
        }

        //optional instances up to maxCount, in template order, from the middle
        for (int i = 0; i < template.rooms.size() && lo <= hi; i++) {
            RoomSpec s = template.rooms.get(i);
            int extra = s.maxCount - s.minCount;
            while (extra > 0 && lo <= hi) {
                pool.get(lo++).type = s.type;
                extra--;
            }
        }

        //filler for anything still untyped
        while (lo <= hi) {
            pool.get(lo++).type = template.filler;
        }
    }
}
