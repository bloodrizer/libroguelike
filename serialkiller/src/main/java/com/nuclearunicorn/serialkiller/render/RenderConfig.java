package com.nuclearunicorn.serialkiller.render;

/**
 * Tunables for the pseudo-isometric render path.
 *
 * The world stays a plain square grid. Only the projection changes: a cell is
 * CELL x CELL on screen, but anything standing in it (walls, furniture, actors)
 * is drawn into a 2:3 box that is bottom-aligned to the cell and therefore
 * overhangs the cell above. Drawing rows top-to-bottom then gives a cheap
 * oblique "2.5D" look without leaving the grid.
 */
public final class RenderConfig {

    /** Screen pixels per grid step. */
    public static int CELL = 24;

    /** Object box aspect: width 2 : height 3. */
    public static final float SPRITE_ASPECT = 1.5f;

    // Every layer can be toggled at run time (F1..F4 in game) or from the
    // command line, which is what the comparison screenshots are made with.

    /** Smooth per-pixel lighting pass (corner-interpolated light field). */
    public static boolean SMOOTH_LIGHT = flag("lrl.light", true);

    /** Connected wall slabs instead of one '#' per tile. */
    public static boolean WALL_PSEUDOGRAPHICS = flag("lrl.walls", true);

    /** Baked pixel tiles/sprites under the glyph layer. */
    public static boolean PIXEL_SPRITES = flag("lrl.sprites", true);

    /** Draw glyphs for whatever has no sprite, plus actor state markers. */
    public static boolean ASCII_LAYER = flag("lrl.ascii", true);

    /** Draw the glyph on top of every sprite too — the full hybrid look. */
    public static boolean ASCII_OVER_SPRITES = flag("lrl.asciiOver", false);

    private static boolean flag(String key, boolean fallback) {
        String value = System.getProperty(key);
        return value == null ? fallback : Boolean.parseBoolean(value);
    }

    /** Debug/screenshot view: draw the map as if everything had been explored. */
    public static boolean REVEAL = Boolean.getBoolean("lrl.reveal");

    private RenderConfig() {
    }

    /** Height of the 2:3 object box, in pixels. */
    public static int spriteH() {
        return (int) (CELL * SPRITE_ASPECT);
    }

    /** How far an object box overhangs the cell above it. */
    public static int riseH() {
        return spriteH() - CELL;
    }
}
