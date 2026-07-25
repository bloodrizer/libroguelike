package com.nuclearunicorn.serialkiller.render;

/**
 * A software ARGB canvas the sprite atlas is painted into before upload.
 * Everything the game draws as "pixel art" is generated here at run time, so
 * the look survives a change of cell size and needs no art assets.
 */
final class PixelCanvas {

    final int w, h;
    final int[] px;

    PixelCanvas(int w, int h) {
        this.w = w;
        this.h = h;
        this.px = new int[w * h];
    }

    static int rgb(int r, int g, int b) {
        return argb(255, r, g, b);
    }

    static int argb(int a, int r, int g, int b) {
        return (clamp(a) << 24) | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
    }

    static int clamp(int v) {
        return v < 0 ? 0 : v > 255 ? 255 : v;
    }

    /** Multiply an opaque colour's brightness. */
    static int shade(int color, float amt) {
        int a = (color >>> 24) & 0xFF;
        int r = (int) (((color >> 16) & 0xFF) * amt);
        int g = (int) (((color >> 8) & 0xFF) * amt);
        int b = (int) ((color & 0xFF) * amt);
        return argb(a, r, g, b);
    }

    void set(int x, int y, int color) {
        if (x < 0 || y < 0 || x >= w || y >= h) {
            return;
        }
        px[y * w + x] = color;
    }

    int get(int x, int y) {
        if (x < 0 || y < 0 || x >= w || y >= h) {
            return 0;
        }
        return px[y * w + x];
    }

    void rect(int x, int y, int rw, int rh, int color) {
        for (int j = 0; j < rh; j++) {
            for (int i = 0; i < rw; i++) {
                set(x + i, y + j, color);
            }
        }
    }

    /** Filled rect with per-pixel brightness jitter — the base of every material. */
    void speckle(int x, int y, int rw, int rh, int color, float amount, int seed) {
        for (int j = 0; j < rh; j++) {
            for (int i = 0; i < rw; i++) {
                float n = Palette.hash(x + i + seed * 31, y + j - seed * 17);
                set(x + i, y + j, shade(color, 1.0f - amount * 0.5f + amount * n));
            }
        }
    }

    /** Vertical light-to-dark ramp, for anything meant to read as a front face. */
    void gradient(int x, int y, int rw, int rh, int color, float top, float bottom) {
        for (int j = 0; j < rh; j++) {
            float t = rh <= 1 ? 0 : (float) j / (rh - 1);
            int row = shade(color, top + (bottom - top) * t);
            for (int i = 0; i < rw; i++) {
                float n = Palette.hash(x + i, y + j);
                set(x + i, y + j, shade(row, 0.94f + n * 0.12f));
            }
        }
    }

    void hline(int x, int y, int len, int color) {
        for (int i = 0; i < len; i++) {
            set(x + i, y, color);
        }
    }

    void vline(int x, int y, int len, int color) {
        for (int j = 0; j < len; j++) {
            set(x, y + j, color);
        }
    }

    void outline(int x, int y, int rw, int rh, int color) {
        hline(x, y, rw, color);
        hline(x, y + rh - 1, rw, color);
        vline(x, y, rh, color);
        vline(x + rw - 1, y, rh, color);
    }

    void ellipse(float cx, float cy, float rx, float ry, int color) {
        for (int j = (int) (cy - ry); j <= cy + ry; j++) {
            for (int i = (int) (cx - rx); i <= cx + rx; i++) {
                float dx = (i + 0.5f - cx) / rx;
                float dy = (j + 0.5f - cy) / ry;
                if (dx * dx + dy * dy <= 1.0f) {
                    set(i, j, color);
                }
            }
        }
    }

    /** Soft dark blob used for contact shadows: alpha falls off with radius. */
    void softEllipse(float cx, float cy, float rx, float ry, int r, int g, int b, int maxAlpha) {
        for (int j = (int) (cy - ry) - 1; j <= cy + ry + 1; j++) {
            for (int i = (int) (cx - rx) - 1; i <= cx + rx + 1; i++) {
                float dx = (i + 0.5f - cx) / rx;
                float dy = (j + 0.5f - cy) / ry;
                float d = dx * dx + dy * dy;
                if (d > 1.0f) {
                    continue;
                }
                int a = (int) (maxAlpha * (1.0f - d) * (1.0f - d * 0.5f));
                if (a > 0) {
                    set(i, j, argb(a, r, g, b));
                }
            }
        }
    }
}
