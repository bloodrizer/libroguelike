package com.nuclearunicorn.serialkiller.render;

/**
 * Draws a wall tile as a connected slab instead of a lone '#'.
 *
 * The tile is treated as a 3x3 stencil, exactly like box-drawing characters:
 * the centre is always solid, and an arm is extended towards every neighbouring
 * wall. That yields the |, L, T and + joints for free.
 *
 * Each stencil column is then extruded upwards by {@link RenderConfig#riseH()}:
 * the raised copy is the slab's top face, the band left behind at the bottom is
 * its front face. Because rows are painted north to south, a wall running north
 * covers the previous cell's front band and the run reads as one continuous
 * slab with a lip on its southern side.
 */
public final class WallPainter {

    /** Fraction of the cell taken by the slab body. */
    private static final float THICKNESS = 0.62f;

    private WallPainter() {
    }

    public static void paint(TileWindow view, int i, int j,
                             float topR, float topG, float topB) {
        int cell = RenderConfig.CELL;
        int rise = RenderConfig.riseH();

        int t = Math.round(cell * THICKNESS);
        int lead = (cell - t) / 2;          // gap before the centre block
        int trail = cell - lead - t;        // gap after it

        boolean n = view.isKnownWall(i, j - 1);
        boolean s = view.isKnownWall(i, j + 1);
        boolean w = view.isKnownWall(i - 1, j);
        boolean e = view.isKnownWall(i + 1, j);

        int x = Grid.cellX(i);
        int y = Grid.cellY(j);

        // a side column also claims the corner square when both of its
        // neighbours are walls, so L joints and solid blocks stay solid
        if (w) {
            column(x, y, lead, rise, n && w, s && w, lead, t, cell, topR, topG, topB);
        }
        column(x + lead, y, t, rise, n, s, lead, t, cell, topR, topG, topB);
        if (e) {
            column(x + lead + t, y, trail, rise, n && e, s && e, lead, t, cell, topR, topG, topB);
        }
    }

    /**
     * One extruded stencil column: front band at the bottom, top face above it.
     */
    private static void column(int x, int cellY, int w, int rise,
                               boolean toTop, boolean toBottom,
                               int lead, int t, int cell,
                               float r, float g, float b) {
        int y = cellY + (toTop ? 0 : lead);
        int h = (toBottom ? cell : lead + t) - (toTop ? 0 : lead);
        if (w <= 0 || h <= 0) {
            return;
        }
        // front face: the strip the slab's top face vacated when it rose
        Draw.quad(x, y + h - rise, w, rise, r * 0.45f, g * 0.45f, b * 0.5f);
        // top face
        Draw.quad(x, y - rise, w, h, r, g, b);
        // thin highlight along the top edge sells the height
        Draw.quad(x, y - rise, w, 1, Math.min(1f, r * 1.5f), Math.min(1f, g * 1.5f),
                Math.min(1f, b * 1.5f));
    }
}
