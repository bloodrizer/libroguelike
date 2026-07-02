package com.nuclearunicorn.serialkiller.generators.town;

import java.util.ArrayList;
import java.util.List;

/**
 * The static template table (TOWN_GENERATION_DESIGN.md 4). One
 * {@link BuildingTemplate} per {@link BuildingType}; weighted selection and
 * per-chunk caps live in {@link TypeSelector}. Built once, immutable thereafter.
 */
public class BuildingTemplates {

    private static final List<BuildingTemplate> ALL = build();

    public static List<BuildingTemplate> all() {
        return ALL;
    }

    public static BuildingTemplate forType(BuildingType type) {
        for (int i = 0; i < ALL.size(); i++) {
            if (ALL.get(i).type == type) {
                return ALL.get(i);
            }
        }
        return ALL.get(0);   // APARTMENT fallback
    }

    private static RoomSpec spec(RoomType t, int minC, int maxC, int minA, int maxA,
                                 boolean win, boolean corr) {
        return new RoomSpec(t, minC, maxC, minA, maxA, win, corr);
    }

    private static List<BuildingTemplate> build() {
        List<BuildingTemplate> list = new ArrayList<BuildingTemplate>();

        // APARTMENT — open plan; furnished by the residential bed/kitchen logic,
        // so its room specs are only advisory (kept for completeness).
        List<RoomSpec> apt = new ArrayList<RoomSpec>();
        apt.add(spec(RoomType.LIVING_ROOM, 1, 1, 20, 48, true, false));
        apt.add(spec(RoomType.KITCHEN,     1, 1, 12, 25, true, false));
        apt.add(spec(RoomType.BEDROOM,     1, 3, 12, 30, true, false));
        apt.add(spec(RoomType.BATHROOM,    1, 1, 6, 12, false, false));
        apt.add(spec(RoomType.STOREROOM,   0, 1, 6, 16, false, false));
        list.add(new BuildingTemplate(BuildingType.APARTMENT, 0, 0, 55f, false, apt, RoomType.BEDROOM));

        // OFFICE — corridor spine with office rooms both sides.
        List<RoomSpec> office = new ArrayList<RoomSpec>();
        office.add(spec(RoomType.LOBBY,       1, 1, 16, 36, true, true));
        office.add(spec(RoomType.OFFICE_ROOM, 3, 6, 12, 30, true, true));
        office.add(spec(RoomType.STOREROOM,   1, 1, 6, 16, false, true));
        office.add(spec(RoomType.BATHROOM,    1, 1, 6, 9, false, true));
        list.add(new BuildingTemplate(BuildingType.OFFICE, 14, 14, 15f, true, office, RoomType.OFFICE_ROOM));

        // BANK — lobby + offices + a locked vault.
        List<RoomSpec> bank = new ArrayList<RoomSpec>();
        bank.add(spec(RoomType.LOBBY,          1, 1, 25, 48, true, false));
        bank.add(spec(RoomType.MANAGER_OFFICE, 1, 1, 12, 20, false, true));
        bank.add(spec(RoomType.OFFICE_ROOM,    1, 2, 12, 20, false, true));
        bank.add(spec(RoomType.VAULT,          1, 1, 9, 16, false, true));
        list.add(new BuildingTemplate(BuildingType.BANK, 14, 14, 5f, true, bank, RoomType.OFFICE_ROOM));

        // BROTHEL — reception + private rooms (bed each).
        List<RoomSpec> brothel = new ArrayList<RoomSpec>();
        brothel.add(spec(RoomType.RECEPTION,    1, 1, 16, 30, true, false));
        brothel.add(spec(RoomType.PRIVATE_ROOM, 3, 6, 9, 16, false, true));
        brothel.add(spec(RoomType.BATHROOM,     1, 1, 6, 9, false, true));
        brothel.add(spec(RoomType.STOREROOM,    0, 1, 6, 12, false, true));
        list.add(new BuildingTemplate(BuildingType.BROTHEL, 14, 14, 5f, true, brothel, RoomType.PRIVATE_ROOM));

        // SHOP — open plan: big shop floor + backroom.
        List<RoomSpec> shop = new ArrayList<RoomSpec>();
        shop.add(spec(RoomType.SHOP_FLOOR, 1, 1, 25, 60, true, false));
        shop.add(spec(RoomType.BACKROOM,   1, 1, 9, 20, false, false));
        shop.add(spec(RoomType.BATHROOM,   0, 1, 6, 9, false, false));
        list.add(new BuildingTemplate(BuildingType.SHOP, 12, 12, 15f, false, shop, RoomType.SHOP_FLOOR));

        // POLICE_STATION — lobby + offices + cells.
        List<RoomSpec> police = new ArrayList<RoomSpec>();
        police.add(spec(RoomType.LOBBY,       1, 1, 16, 30, true, false));
        police.add(spec(RoomType.OFFICE_ROOM, 2, 3, 12, 25, true, true));
        police.add(spec(RoomType.CELL,        2, 3, 6, 9, false, true));
        list.add(new BuildingTemplate(BuildingType.POLICE_STATION, 14, 14, 5f, true, police, RoomType.OFFICE_ROOM));

        return list;
    }
}
