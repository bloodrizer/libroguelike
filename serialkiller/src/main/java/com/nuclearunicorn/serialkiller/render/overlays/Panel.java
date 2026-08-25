package com.nuclearunicorn.serialkiller.render.overlays;

import com.nuclearunicorn.serialkiller.render.Draw;
import org.newdawn.slick.Color;
import org.newdawn.slick.FontSpec;
import org.newdawn.slick.TrueTypeFont;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.GL_LINE_LOOP;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glColor4f;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glLineWidth;
import static org.lwjgl.opengl.GL11.glVertex2f;

/**
 * The debug-panel toolkit: one palette, one font, one self-measuring text column.
 *
 * <p>Extracted the moment there were two panels. They have to agree about what "a section
 * heading" or "a value that is worrying" looks like, or the eye has to relearn the colours
 * every time it crosses the screen — and a second private copy of the wrapper would have
 * drifted from the first inside a week.
 */
public final class Panel {

    private Panel() {}

    public static final Color HEADER = new Color(255, 226, 120);
    public static final Color SECTION = new Color(120, 200, 235);
    public static final Color KEY = new Color(150, 150, 160);
    public static final Color VALUE = new Color(225, 225, 230);
    public static final Color DIM = new Color(120, 120, 130);
    public static final Color HOT = new Color(255, 110, 100);
    public static final Color WARM = new Color(245, 175, 80);
    public static final Color GOOD = new Color(130, 220, 130);
    public static final Color POLICE = new Color(120, 190, 255);
    public static final Color LUST = new Color(255, 120, 180);

    private static final Color BORDER = new Color(70, 70, 90);

    private static TrueTypeFont font;
    private static TrueTypeFont tagFont;

    public static TrueTypeFont font() {
        if (font == null) {
            font = new TrueTypeFont(new FontSpec("Monospaced", 12, false), true);
        }
        return font;
    }

    /** Smaller and bold: what gets drawn over a head, where there is no room to be polite. */
    public static TrueTypeFont tagFont() {
        if (tagFont == null) {
            tagFont = new TrueTypeFont(new FontSpec("Monospaced", 11, true), true);
        }
        return tagFont;
    }

    public static void box(float x, float y, float w, float h, Color color) {
        glDisable(GL_TEXTURE_2D);
        glLineWidth(1);
        glBegin(GL_LINE_LOOP);
        glColor4f(color.r, color.g, color.b, 0.9f);
        glVertex2f(x, y);
        glVertex2f(x + w, y);
        glVertex2f(x + w, y + h);
        glVertex2f(x, y + h);
        glEnd();
        glEnable(GL_TEXTURE_2D);
    }

    /** The dark plate a panel's text sits on, framed. Drawn after the text is measured. */
    public static void backdrop(float x, float y, float w, float h) {
        Draw.beginFlat();
        Draw.quad(x, y, w, h, 0.05f, 0.05f, 0.08f, 0.85f);
        Draw.endFlat();
        box(x, y, w, h, BORDER);
    }

    /** Left-align in a column, always leaving a separator: names run longer than the width. */
    public static String pad(String text, int width) {
        StringBuilder sb = new StringBuilder(text == null ? "?" : text);
        do {
            sb.append(' ');
        } while (sb.length() < width);
        return sb.toString();
    }

    public static String join(List<String> parts, String separator) {
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (sb.length() > 0) {
                sb.append(separator);
            }
            sb.append(part);
        }
        return sb.toString();
    }

    /** A 0..max quantity as a fixed-width bar, so a column of them reads as a chart. */
    public static String bar(float value, float max, int width) {
        int filled = max <= 0 ? 0 : Math.round(Math.min(1f, value / max) * width);
        StringBuilder sb = new StringBuilder(width);
        for (int i = 0; i < width; i++) {
            sb.append(i < filled ? '#' : '.');
        }
        return sb.toString();
    }

    /**
     * A column of text that measures itself, wraps to the panel width and stops when it runs
     * out of room. Lines are buffered rather than drawn, because the panel's background has
     * to be sized to the content and is therefore drawn after it.
     */
    public static final class Text {

        private final TrueTypeFont font;
        private final float x;
        private final float top;
        private final int width;
        private final int maxY;
        private final List<float[]> positions = new ArrayList<float[]>();
        private final List<String> texts = new ArrayList<String>();
        private final List<Color> colors = new ArrayList<Color>();
        private float y;

        public Text(TrueTypeFont font, float x, float y, int width, int maxY) {
            this.font = font;
            this.x = x;
            this.top = y;
            this.y = y;
            this.width = width;
            this.maxY = maxY;
        }

        public void line(String text, Color color) {
            if (y + font.getHeight() > maxY) {
                return;
            }
            positions.add(new float[] {x, y});
            texts.add(text);
            colors.add(color);
            y += font.getHeight() - 2;
        }

        public void blank() {
            if (y + font.getHeight() > maxY) {
                return;   // out of room: a gap here would only lengthen the background
            }
            y += 4;
        }

        public void section(String title) {
            blank();
            line(title, SECTION);
        }

        /** Break a long line at word boundaries; continuations are indented under the first. */
        public void wrap(String text, Color color) {
            if (text == null) {
                return;
            }
            String rest = text;
            String indent = "";
            while (font.getWidth(rest) > width) {
                int cut = rest.length();
                while (cut > 1 && font.getWidth(rest.substring(0, cut)) > width) {
                    cut--;
                }
                int space = rest.lastIndexOf(' ', cut);
                if (space <= 0) {
                    space = cut;
                }
                line(indent + rest.substring(0, space).trim(), color);
                rest = rest.substring(space).trim();
                indent = "  ";
                if (y + font.getHeight() > maxY) {
                    return;
                }
            }
            line(indent + rest, color);
        }

        public float height() {
            return y - top;
        }

        public void flush() {
            for (int i = 0; i < texts.size(); i++) {
                float[] at = positions.get(i);
                font.drawString(at[0], at[1], texts.get(i), colors.get(i));
            }
        }
    }
}
