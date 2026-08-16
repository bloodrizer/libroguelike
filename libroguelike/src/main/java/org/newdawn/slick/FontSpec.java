package org.newdawn.slick;

/**
 * Platform-neutral description of a face to bake.
 *
 * The desktop build turns this into a java.awt.Font and rasterises with
 * Graphics2D; the browser build turns it into a CSS font string and rasterises
 * with Canvas2D. Callers (OverlaySystem, Glyphs) stay free of either.
 */
public final class FontSpec {

    public final String family;
    public final int size;
    public final boolean bold;
    /** Optional classpath TTF to prefer over the family name; may be null. */
    public final String resource;

    public FontSpec(String family, int size, boolean bold) {
        this(family, size, bold, null);
    }

    public FontSpec(String family, int size, boolean bold, String resource) {
        this.family = family;
        this.size = size;
        this.bold = bold;
        this.resource = resource;
    }

    public FontSpec withSize(int newSize) {
        return new FontSpec(family, newSize, bold, resource);
    }
}
