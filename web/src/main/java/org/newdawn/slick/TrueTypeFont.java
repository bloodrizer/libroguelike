// Web replacement for the slick-util TrueTypeFont. Bakes ASCII glyphs with
// Canvas2D into a single WebGL texture, then renders strings as textured quads
// through the GL 1.1 emulation, mirroring the desktop implementation.
package org.newdawn.slick;

import org.lwjgl.opengl.GLES;
import org.teavm.jso.browser.Window;
import org.teavm.jso.canvas.CanvasRenderingContext2D;
import org.teavm.jso.canvas.TextMetrics;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.teavm.jso.webgl.WebGLRenderingContext;
import org.teavm.jso.webgl.WebGLTexture;

import static org.lwjgl.opengl.GL11.*;

public class TrueTypeFont {

    private static final int FIRST_CHAR = 32;   // space
    private static final int LAST_CHAR = 256;
    private static final int PAD = 2;

    private final int[] glyphX = new int[LAST_CHAR];
    private final int[] glyphY = new int[LAST_CHAR];
    private final int[] glyphW = new int[LAST_CHAR];
    private final int[] glyphH = new int[LAST_CHAR];
    private final int textureId;
    private final int texW;
    private final int texH;
    private final int lineHeight;

    public TrueTypeFont(FontSpec spec, boolean antiAlias) {
        // A bundled TTF would need the async FontFace API; the family name is
        // resolved by the browser instead. See PORTING.md §10.
        String css = (spec.bold ? "bold " : "") + spec.size + "px " + cssFamily(spec.family);

        HTMLCanvasElement measure = (HTMLCanvasElement)
                Window.current().getDocument().createElement("canvas");
        CanvasRenderingContext2D mctx =
                (CanvasRenderingContext2D) measure.getContext("2d");
        mctx.setFont(css);

        // Canvas2D exposes no font-wide metrics in WebGL 1-era browsers, so the
        // line box is derived from the em size the way most 2D engines do.
        int ascent = spec.size;
        lineHeight = (int) Math.ceil(spec.size * 1.25);

        int rowMaxW = 1024;
        int x = PAD;
        int y = PAD;
        int rowH = 0;
        for (int c = FIRST_CHAR; c < LAST_CHAR; c++) {
            TextMetrics tm = mctx.measureText(String.valueOf((char) c));
            int w = (int) Math.ceil(tm.getWidth());
            if (w == 0) {
                continue;
            }
            if (x + w + PAD > rowMaxW) {
                x = PAD;
                y += rowH + PAD;
                rowH = 0;
            }
            glyphX[c] = x;
            glyphY[c] = y;
            glyphW[c] = w;
            glyphH[c] = lineHeight;
            x += w + PAD;
            if (lineHeight > rowH) {
                rowH = lineHeight;
            }
        }

        texW = rowMaxW;
        texH = nextPow2(y + rowH + PAD);

        HTMLCanvasElement atlas = (HTMLCanvasElement)
                Window.current().getDocument().createElement("canvas");
        atlas.setWidth(texW);
        atlas.setHeight(texH);
        CanvasRenderingContext2D ctx =
                (CanvasRenderingContext2D) atlas.getContext("2d");
        ctx.setFont(css);
        ctx.setFillStyle("#ffffff");
        ctx.setTextBaseline("alphabetic");
        for (int c = FIRST_CHAR; c < LAST_CHAR; c++) {
            if (glyphW[c] == 0) {
                continue;
            }
            ctx.fillText(String.valueOf((char) c), glyphX[c], glyphY[c] + ascent);
        }

        WebGLRenderingContext gl = GLES.gl;
        WebGLTexture tex = gl.createTexture();
        textureId = GLES.registerTexture(tex);
        gl.bindTexture(WebGLRenderingContext.TEXTURE_2D, tex);
        gl.pixelStorei(WebGLRenderingContext.UNPACK_PREMULTIPLY_ALPHA_WEBGL, 0);
        gl.texImage2D(WebGLRenderingContext.TEXTURE_2D, 0,
                WebGLRenderingContext.RGBA, WebGLRenderingContext.RGBA,
                WebGLRenderingContext.UNSIGNED_BYTE, atlas);
        gl.texParameteri(WebGLRenderingContext.TEXTURE_2D,
                WebGLRenderingContext.TEXTURE_MIN_FILTER, WebGLRenderingContext.LINEAR);
        gl.texParameteri(WebGLRenderingContext.TEXTURE_2D,
                WebGLRenderingContext.TEXTURE_MAG_FILTER, WebGLRenderingContext.LINEAR);
        gl.texParameteri(WebGLRenderingContext.TEXTURE_2D,
                WebGLRenderingContext.TEXTURE_WRAP_S, WebGLRenderingContext.CLAMP_TO_EDGE);
        gl.texParameteri(WebGLRenderingContext.TEXTURE_2D,
                WebGLRenderingContext.TEXTURE_WRAP_T, WebGLRenderingContext.CLAMP_TO_EDGE);
    }

    /** Maps the AWT logical family names the game uses onto CSS generics. */
    private static String cssFamily(String family) {
        if ("Monospaced".equals(family)) {
            return "monospace";
        }
        if ("Serif".equals(family)) {
            return "serif";
        }
        if ("SansSerif".equals(family)) {
            return "sans-serif";
        }
        return family + ", sans-serif";
    }

    private static int nextPow2(int v) {
        int p = 1;
        while (p < v) {
            p <<= 1;
        }
        return p;
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

            glTexCoord2f(u0, v0);
            glVertex2f(cx, y);
            glTexCoord2f(u1, v0);
            glVertex2f(cx + glyphW[c], y);
            glTexCoord2f(u1, v1);
            glVertex2f(cx + glyphW[c], y + glyphH[c]);
            glTexCoord2f(u0, v1);
            glVertex2f(cx, y + glyphH[c]);
            cx += glyphW[c];
        }
        glEnd();
        glColor4f(1, 1, 1, 1);
    }

    public int getWidth(String text) {
        int w = 0;
        for (int i = 0; i < text.length(); i++) {
            int c = text.charAt(i);
            if (c >= FIRST_CHAR && c < LAST_CHAR) {
                w += glyphW[c];
            }
        }
        return w;
    }

    public int getHeight() {
        return lineHeight;
    }

    public int getHeight(String text) {
        return lineHeight;
    }

    public int getLineHeight() {
        return lineHeight;
    }
}
