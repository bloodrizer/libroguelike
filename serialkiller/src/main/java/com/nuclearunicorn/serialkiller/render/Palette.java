package com.nuclearunicorn.serialkiller.render;

import com.nuclearunicorn.serialkiller.game.world.RLTile;
import org.newdawn.slick.Color;

/**
 * Material colours (albedo) for terrain. These are what the surface looks like
 * under white light — all the mood comes from {@link LightMap} multiplying on
 * top, so nothing here encodes distance, FOV or time of day.
 *
 * The returned arrays are scratch buffers reused between calls: read them
 * before the next call. Rendering is single threaded.
 */
public final class Palette {

    private static final float[] rgb = new float[3];
    private static final float[] rgb2 = new float[3];

    private Palette() {
    }

    /** Deterministic per-tile noise in [0,1), for material speckle. */
    public static float hash(int x, int y) {
        int h = x * 374761393 + y * 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        return ((h ^ (h >>> 16)) & 0xFFFF) / 65536.0f;
    }

    public static float[] floor(RLTile tile, int i, int j) {
        float r, g, b;

        switch (tile.getTileType() == null ? RLTile.TileType.GROUND : tile.getTileType()) {
            case ROAD:
                if (isSidewalk(tile)) {
                    r = 0.33f; g = 0.33f; b = 0.34f;
                } else {
                    r = 0.21f; g = 0.21f; b = 0.24f;
                }
                break;
            case GRASS:
                r = 0.17f; g = 0.27f; b = 0.15f;
                break;
            case WALL:
                r = 0.30f; g = 0.29f; b = 0.28f;
                break;
            default:
                if (tile.isIndoor()) {
                    r = 0.46f; g = 0.38f; b = 0.28f;   // boards
                } else {
                    r = 0.31f; g = 0.28f; b = 0.24f;   // bare ground
                }
                break;
        }

        float n = 0.92f + hash(i, j) * 0.16f;
        r *= n;
        g *= n;
        b *= n;

        float blood = tile.getBloodAmt();
        if (blood > 0.0f) {
            r = lerp(r, 0.55f, blood);
            g = lerp(g, 0.05f, blood);
            b = lerp(b, 0.06f, blood);
        }

        rgb[0] = r;
        rgb[1] = g;
        rgb[2] = b;
        return rgb;
    }

    public static float[] wallTop(RLTile tile, int i, int j) {
        float n = 0.90f + hash(i, j) * 0.2f;
        rgb2[0] = 0.52f * n;
        rgb2[1] = 0.50f * n;
        rgb2[2] = 0.47f * n;
        return rgb2;
    }

    /** Sidewalk and asphalt are both ROAD; the generator tints them apart. */
    private static boolean isSidewalk(RLTile tile) {
        Color c = tile.getTileModelColor();
        return c != null && c.b > 0.1f;   // sidewalk is grey (90,90,90), asphalt is (127,127,0)
    }

    public static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}
