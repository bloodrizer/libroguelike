package com.nuclearunicorn.serialkiller.render;

import com.nuclearunicorn.libroguelike.render.overlay.OverlaySystem;
import org.newdawn.slick.Color;
import org.newdawn.slick.FontSpec;
import org.newdawn.slick.TrueTypeFont;

/**
 * The ASCII layer. Glyphs are centred in their cell horizontally and sit on the
 * cell's floor line vertically, so they line up with the bottom-aligned sprite
 * boxes instead of floating in the middle of the tile.
 */
public final class Glyphs {

    private static TrueTypeFont font;
    private static int fontCell = -1;

    private Glyphs() {
    }

    /** Monospaced face sized to the current cell, baked once per cell size. */
    public static TrueTypeFont font() {
        if (font == null || fontCell != RenderConfig.CELL) {
            int size = Math.max(10, Math.round(RenderConfig.CELL * 0.85f));
            font = new TrueTypeFont(new FontSpec("Monospaced", size, true), true);
            fontCell = RenderConfig.CELL;
        }
        return font;
    }

    public static void draw(int i, int j, String glyph, Color color) {
        draw(Grid.cellX(i), Grid.cellY(j), glyph, color, 0);
    }

    /**
     * @param x,y  top-left of the cell in world pixels
     * @param lift how far above the floor line to draw (for things standing up)
     */
    public static void draw(float x, float y, String glyph, Color color, int lift) {
        TrueTypeFont ttf = font();
        float gx = x + (RenderConfig.CELL - ttf.getWidth(glyph)) / 2.0f;
        float gy = y + RenderConfig.CELL - ttf.getHeight() - lift;
        ttf.drawString(gx, gy, glyph, color == null ? Color.white : color);
    }
}
