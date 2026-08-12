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

    /** Linear interpolation between two opaque colours. */
    static int mix(int a, int b, float t) {
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        return argb(255,
                (int) (ar + (br - ar) * t),
                (int) (ag + (bg - ag) * t),
                (int) (ab + (bb - ab) * t));
    }

    void set(int x, int y, int color) {
        if (x < 0 || y < 0 || x >= w || y >= h) {
            return;
        }
        px[y * w + x] = color;
    }

    /** Alpha-composite one pixel over whatever is already there. */
    void blend(int x, int y, int color) {
        int a = (color >>> 24) & 0xFF;
        if (a == 0 || x < 0 || y < 0 || x >= w || y >= h) {
            return;
        }
        if (a == 255) {
            px[y * w + x] = color;
            return;
        }
        int dst = px[y * w + x];
        int da = (dst >>> 24) & 0xFF;
        float sa = a / 255.0f;
        int outA = (int) (a + da * (1 - sa));
        int r = (int) (((color >> 16) & 0xFF) * sa + ((dst >> 16) & 0xFF) * (1 - sa));
        int g = (int) (((color >> 8) & 0xFF) * sa + ((dst >> 8) & 0xFF) * (1 - sa));
        int b = (int) ((color & 0xFF) * sa + (dst & 0xFF) * (1 - sa));
        px[y * w + x] = argb(outA, r, g, b);
    }

    void blendRect(int x, int y, int rw, int rh, int color) {
        for (int j = 0; j < rh; j++) {
            for (int i = 0; i < rw; i++) {
                blend(x + i, y + j, color);
            }
        }
    }

    boolean opaque(int x, int y) {
        return ((get(x, y) >>> 24) & 0xFF) > 40;
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

    /** Bresenham segment — the diagonal braces on crates and ladders. */
    void line(int x0, int y0, int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0), sx = x0 < x1 ? 1 : -1;
        int dy = -Math.abs(y1 - y0), sy = y0 < y1 ? 1 : -1;
        int err = dx + dy;
        while (true) {
            set(x0, y0, color);
            if (x0 == x1 && y0 == y1) {
                return;
            }
            int e2 = 2 * err;
            if (e2 >= dy) {
                err += dy;
                x0 += sx;
            }
            if (e2 <= dx) {
                err += dx;
                y0 += sy;
            }
        }
    }

    /**
     * Trace a 1px dark rim just outside whatever is already drawn in the box.
     * Every object sprite gets one — it is what makes the pixel layer read as
     * shapes rather than as smudges once the light pass has multiplied over it.
     */
    void rim(int x, int y, int rw, int rh, int color) {
        int[] copy = new int[rw * rh];
        for (int j = 0; j < rh; j++) {
            for (int i = 0; i < rw; i++) {
                copy[j * rw + i] = opaque(x + i, y + j) ? 1 : 0;
            }
        }
        for (int j = 0; j < rh; j++) {
            for (int i = 0; i < rw; i++) {
                if (copy[j * rw + i] != 0) {
                    continue;
                }
                boolean touches = (i > 0 && copy[j * rw + i - 1] != 0)
                        || (i < rw - 1 && copy[j * rw + i + 1] != 0)
                        || (j > 0 && copy[(j - 1) * rw + i] != 0)
                        || (j < rh - 1 && copy[(j + 1) * rw + i] != 0);
                if (touches) {
                    blend(x + i, y + j, color);
                }
            }
        }
    }

    /**
     * Re-draw a source sprite as a flat silhouette with a bright rim — how an
     * actor the player remembers but cannot currently see is shown.
     */
    void silhouette(int sx, int sy, int dx, int dy, int rw, int rh, int fill, int rimColor) {
        for (int j = 0; j < rh; j++) {
            for (int i = 0; i < rw; i++) {
                if (opaque(sx + i, sy + j)) {
                    set(dx + i, dy + j, fill);
                }
            }
        }
        rim(dx, dy, rw, rh, rimColor);
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
