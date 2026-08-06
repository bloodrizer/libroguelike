package com.nuclearunicorn.serialkiller.render;

import static org.lwjgl.opengl.GL11.*;

/**
 * Tiny immediate-mode helper. The renderer batches long runs of quads between
 * one begin/end pair instead of toggling GL state per quad.
 */
public final class Draw {

    private Draw() {
    }

    /** Start a batch of untextured, per-vertex coloured quads. */
    public static void beginFlat() {
        glDisable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glBegin(GL_QUADS);
    }

    public static void endFlat() {
        glEnd();
        glEnable(GL_TEXTURE_2D);
    }

    /** Start a batch of quads multiplied into the frame buffer (dst *= src). */
    public static void beginMultiply() {
        glDisable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_ZERO, GL_SRC_COLOR);
        glBegin(GL_QUADS);
    }

    /** Start a batch of quads added to the frame buffer (dst += src*alpha). */
    public static void beginAdditive() {
        glDisable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE);
        glBegin(GL_QUADS);
    }

    public static void endBlend() {
        glEnd();
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_TEXTURE_2D);
    }

    public static void quad(float x, float y, float w, float h, float r, float g, float b) {
        quad(x, y, w, h, r, g, b, 1.0f);
    }

    public static void quad(float x, float y, float w, float h,
                            float r, float g, float b, float a) {
        glColor4f(r, g, b, a);
        glVertex2f(x, y);
        glVertex2f(x + w, y);
        glVertex2f(x + w, y + h);
        glVertex2f(x, y + h);
    }

    /** Quad with a colour per corner: the GL interpolates it per pixel. */
    public static void quadGouraud(float x, float y, float w, float h,
                                   float[] tl, float[] tr, float[] br, float[] bl) {
        glColor4f(tl[0], tl[1], tl[2], 1.0f);
        glVertex2f(x, y);
        glColor4f(tr[0], tr[1], tr[2], 1.0f);
        glVertex2f(x + w, y);
        glColor4f(br[0], br[1], br[2], 1.0f);
        glVertex2f(x + w, y + h);
        glColor4f(bl[0], bl[1], bl[2], 1.0f);
        glVertex2f(x, y + h);
    }

    /** Textured quad, uv in [0..1] atlas space. Needs an active texture bind. */
    public static void sprite(float x, float y, float w, float h,
                              float u0, float v0, float u1, float v1) {
        glTexCoord2f(u0, v0);
        glVertex2f(x, y);
        glTexCoord2f(u1, v0);
        glVertex2f(x + w, y);
        glTexCoord2f(u1, v1);
        glVertex2f(x + w, y + h);
        glTexCoord2f(u0, v1);
        glVertex2f(x, y + h);
    }
}
