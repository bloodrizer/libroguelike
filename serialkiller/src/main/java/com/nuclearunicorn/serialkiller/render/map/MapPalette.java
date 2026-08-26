package com.nuclearunicorn.serialkiller.render.map;

import com.nuclearunicorn.serialkiller.generators.town.BuildingType;

/**
 * The town map's own colour scheme, in packed ARGB.
 *
 * <p>Separate from {@link com.nuclearunicorn.serialkiller.render.Palette} on purpose. That one
 * is albedo under white light, and the light field supplies all the contrast; a map has no
 * light field, so a street painted in the world's asphalt grey and a floor painted in the
 * world's board brown come out as two nearly identical smudges at one pixel per tile. These
 * colours are picked for legibility at that size instead: terrain stays desaturated and dark,
 * and hue is spent entirely on telling one kind of building from another.
 */
public final class MapPalette {

    private MapPalette() {}

    // ------------------------------------------------------------------ terrain

    public static final int UNMAPPED = 0x00000000;          //never seen: the plate shows through
    public static final int ASPHALT  = 0xFF2B2C34;
    public static final int SIDEWALK = 0xFF474A54;
    public static final int GRASS    = 0xFF24421F;
    public static final int GROUND   = 0xFF37332C;
    /** A door. Off-white rather than any type's hue, so a way in never reads as a bank. */
    public static final int DOOR     = 0xFFFFF2CC;

    // Buildings with no known type (a basement, say) still have to read as built.
    public static final int WALL     = 0xFF6B6A66;
    public static final int FLOOR    = 0xFF3B3A37;

    public static final int PLAYER      = 0xFFFFFFFF;
    public static final int PLAYER_RING = 0xFFFF6B6B;

    /**
     * Wall colour per building type — the type's identity, near full strength, because at
     * map scale the outline is most of what you see of a house.
     */
    public static int wall(BuildingType type) {
        switch (type) {
            case BROTHEL:        return 0xFFD4568F;
            case BANK:           return 0xFFD8B441;
            case POLICE_STATION: return 0xFF5E9BE0;
            case SHOP:           return 0xFFE0873C;
            case OFFICE:         return 0xFF8792A6;
            default:             return 0xFF9C7C5C;         //APARTMENT: brick
        }
    }

    /**
     * Interior floor: the same hue at half strength. Half rather than a tenth on purpose —
     * what the map is for is telling a brothel from a bank at a glance, and a building filled
     * with its own colour says that from across the screen where a thin outline does not.
     */
    public static int floor(BuildingType type) {
        return shade(wall(type), FLOOR_AMT);
    }

    public static final float FLOOR_AMT = 0.5f;

    /** The player's own flat. Not a type — a house of any type can be it. */
    public static final int HOME_WALL = 0xFF4FE0E8;

    // ------------------------------------------------------------------ labels

    public static String label(BuildingType type) {
        switch (type) {
            case BROTHEL:        return "BROTHEL";
            case BANK:           return "BANK";
            case POLICE_STATION: return "POLICE";
            case SHOP:           return "SHOP";
            case OFFICE:         return "OFFICE";
            default:             return "FLATS";
        }
    }

    /**
     * Whether the town takes this kind of building for granted. Everybody knows where the
     * bank is; nobody knows the layout of a stranger's flat until they have been inside it,
     * so landmarks are drawn through the fog and apartments are not.
     */
    public static boolean isLandmark(BuildingType type) {
        return type != BuildingType.APARTMENT;
    }

    // ------------------------------------------------------------------ colour maths

    public static int shade(int argb, float amt) {
        int a = (argb >>> 24) & 0xFF;
        return pack(a,
                (int) (((argb >> 16) & 0xFF) * amt),
                (int) (((argb >> 8) & 0xFF) * amt),
                (int) ((argb & 0xFF) * amt));
    }

    /** Pull a colour towards white, for text that has to sit on top of that same colour. */
    public static int lift(int argb, float amt) {
        return pack((argb >>> 24) & 0xFF,
                (int) (((argb >> 16) & 0xFF) + (255 - ((argb >> 16) & 0xFF)) * amt),
                (int) (((argb >> 8) & 0xFF) + (255 - ((argb >> 8) & 0xFF)) * amt),
                (int) ((argb & 0xFF) + (255 - (argb & 0xFF)) * amt));
    }

    /** Darken and thin a colour down to "on the map, but not walked": known by hearsay. */
    public static int hearsay(int argb) {
        int dim = shade(argb, 0.42f);
        return (dim & 0x00FFFFFF) | (0xB0 << 24);
    }

    public static int pack(int a, int r, int g, int b) {
        return (clamp(a) << 24) | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
    }

    public static float red(int argb)   { return ((argb >> 16) & 0xFF) / 255.0f; }
    public static float green(int argb) { return ((argb >> 8) & 0xFF) / 255.0f; }
    public static float blue(int argb)  { return (argb & 0xFF) / 255.0f; }
    public static float alpha(int argb) { return ((argb >>> 24) & 0xFF) / 255.0f; }

    private static int clamp(int v) {
        return v < 0 ? 0 : (v > 255 ? 255 : v);
    }
}
