// Replacement for slick-util TrueTypeFont. Bakes ASCII glyphs from a
// java.awt.Font into a single GL texture atlas at construction, then renders
// strings as a list of textured quads in immediate mode.
package org.newdawn.slick;

import org.lwjgl.BufferUtils;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;

public class TrueTypeFont {

    private static final int FIRST_CHAR = 32;   // space
    private static final int LAST_CHAR  = 256;
    private static final int PAD = 2;

    private final Font font;
    private final int[] glyphX = new int[LAST_CHAR];
    private final int[] glyphY = new int[LAST_CHAR];
    private final int[] glyphW = new int[LAST_CHAR];
    private final int[] glyphH = new int[LAST_CHAR];
    private final int textureId;
    private final int texW, texH;
    private final int lineHeight;
    private int ascent;

    /** Builds the AWT face described by the spec, falling back to the family name. */
    private static Font toAwt(FontSpec spec) {
        int style = spec.bold ? Font.BOLD : Font.PLAIN;
        if (spec.resource != null) {
            try (java.io.InputStream is =
                         TrueTypeFont.class.getResourceAsStream(spec.resource)) {
                if (is != null) {
                    return Font.createFont(Font.TRUETYPE_FONT, is)
                            .deriveFont(style, (float) spec.size);
                }
            } catch (Exception e) {
                System.err.println(spec.resource + " not loaded, using " + spec.family);
            }
        }
        return new Font(spec.family, style, spec.size);
    }

    public TrueTypeFont(FontSpec spec, boolean antiAlias) {
        this(toAwt(spec), antiAlias);
    }

    public TrueTypeFont(Font font, boolean antiAlias) {
        this.font = font;

        // Measure glyphs first so we can size the atlas.
        BufferedImage probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = probe.createGraphics();
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();
        ascent = fm.getAscent();
        lineHeight = fm.getHeight();
        g.dispose();

        int rowMaxW = 1024;
        int x = PAD, y = PAD, rowH = 0;
        for (int c = FIRST_CHAR; c < LAST_CHAR; c++) {
            int w = fm.charWidth((char) c);
            int h = fm.getHeight();
            if (w == 0) continue;
            if (x + w + PAD > rowMaxW) { x = PAD; y += rowH + PAD; rowH = 0; }
            glyphX[c] = x;
            glyphY[c] = y;
            glyphW[c] = w;
            glyphH[c] = h;
            x += w + PAD;
            if (h > rowH) rowH = h;
        }
        int neededH = y + rowH + PAD;
        texW = rowMaxW;
        texH = nextPow2(neededH);

        BufferedImage atlas = new BufferedImage(texW, texH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D ag = atlas.createGraphics();
        if (antiAlias) {
            ag.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        }
        ag.setFont(font);
        ag.setColor(java.awt.Color.WHITE);
        for (int c = FIRST_CHAR; c < LAST_CHAR; c++) {
            if (glyphW[c] == 0) continue;
            ag.drawString(String.valueOf((char) c), glyphX[c], glyphY[c] + ascent);
        }
        ag.dispose();

        // Upload to GL.
        int[] data = ((DataBufferInt) atlas.getRaster().getDataBuffer()).getData();
        IntBuffer pix = BufferUtils.createIntBuffer(data.length);
        // Java's TYPE_INT_ARGB -> GL needs RGBA bytes (little-endian => 0xAABBGGRR).
        for (int i = 0; i < data.length; i++) {
            int argb = data[i];
            int a = (argb >>> 24) & 0xFF;
            int r = (argb >>> 16) & 0xFF;
            int gC = (argb >>> 8)  & 0xFF;
            int b = argb           & 0xFF;
            pix.put((a << 24) | (b << 16) | (gC << 8) | r);
        }
        pix.flip();

        textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, texW, texH, 0, GL_RGBA, GL_UNSIGNED_BYTE, pix);
    }

    private static int nextPow2(int v) {
        int p = 1; while (p < v) p <<= 1; return p;
    }

    public void drawString(float x, float y, String text) {
        drawString(x, y, text, Color.white);
    }

    public void drawString(float x, float y, String text, Color color) {
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glBindTexture(GL_TEXTURE_2D, textureId);
        glColor4f(color.r, color.g, color.b, color.a);

        glBegin(GL_QUADS);
        float cx = x;
        for (int i = 0; i < text.length(); i++) {
            int c = text.charAt(i);
            if (c < FIRST_CHAR || c >= LAST_CHAR || glyphW[c] == 0) {
                cx += getWidth(" ");
                continue;
            }
            float u0 = glyphX[c] / (float) texW;
            float v0 = glyphY[c] / (float) texH;
            float u1 = (glyphX[c] + glyphW[c]) / (float) texW;
            float v1 = (glyphY[c] + glyphH[c]) / (float) texH;
            float qx0 = cx;
            float qy0 = y;
            float qx1 = cx + glyphW[c];
            float qy1 = y + glyphH[c];

            glTexCoord2f(u0, v0); glVertex2f(qx0, qy0);
            glTexCoord2f(u1, v0); glVertex2f(qx1, qy0);
            glTexCoord2f(u1, v1); glVertex2f(qx1, qy1);
            glTexCoord2f(u0, v1); glVertex2f(qx0, qy1);
            cx += glyphW[c];
        }
        glEnd();
        glColor4f(1, 1, 1, 1);
    }

    public int getWidth(String text) {
        int w = 0;
        for (int i = 0; i < text.length(); i++) {
            int c = text.charAt(i);
            if (c >= FIRST_CHAR && c < LAST_CHAR) w += glyphW[c];
        }
        return w;
    }

    public int getHeight() { return lineHeight; }
    public int getHeight(String text) { return lineHeight; }
    public int getLineHeight() { return lineHeight; }
}
