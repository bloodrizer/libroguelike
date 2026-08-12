package com.nuclearunicorn.serialkiller.render;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Bakes the sprite atlas without a GL context and writes it to a PNG, so the
 * pixel art can be iterated on in a second instead of by launching the game and
 * hunting for a crate in the dark.
 *
 *   mvn -q exec:java -Dexec.mainClass=...AtlasDump -Dexec.args="out.png 32 6"
 *   scripts/atlas.sh out.png [cell] [zoom]
 *
 * Two images come out: the raw atlas (out.png) and a contact sheet laying every
 * object sprite over a floor tile with its neighbours' wall masks (out.sheet.png).
 */
public final class AtlasDump {

    private AtlasDump() {
    }

    public static void main(String[] args) throws IOException {
        String out = args.length > 0 ? args[0] : "atlas.png";
        int cell = args.length > 1 ? Integer.parseInt(args[1]) : RenderConfig.CELL;
        int zoom = args.length > 2 ? Integer.parseInt(args[2]) : 4;

        RenderConfig.CELL = cell;
        SpriteAtlas atlas = SpriteAtlas.bakeOffscreen(cell);

        write(new File(out), toImage(atlas.canvas, zoom == 0 ? 1 : zoom));
        write(new File(sheetPath(out)), sheet(atlas, zoom));

        System.out.println("cell=" + cell + " box=" + RenderConfig.spriteH()
                + " rise=" + RenderConfig.riseH());
        System.out.println("wrote " + out + " and " + sheetPath(out));
        for (int i = 0; i < SpriteAtlas.OBJECTS; i++) {
            System.out.println("  [" + i + "] " + SpriteAtlas.OBJECT_NAMES[i]);
        }
    }

    private static String sheetPath(String out) {
        int dot = out.lastIndexOf('.');
        return dot < 0 ? out + ".sheet.png" : out.substring(0, dot) + ".sheet.png";
    }

    /**
     * Object sprites in a grid, each over a floor tile so alpha and the contact
     * shadow are visible, plus all 16 wall masks and every floor material.
     */
    private static BufferedImage sheet(SpriteAtlas atlas, int zoom) {
        int cell = atlas.cellSize();
        int boxH = RenderConfig.spriteH();
        int pad = 4;
        int cols = 8;
        int objRows = (SpriteAtlas.OBJECTS + cols - 1) / cols;
        int wallRows = (16 + cols - 1) / cols;
        int floorRows = 6;

        int cw = cell + pad;
        int chH = boxH + pad;
        int w = cols * cw + pad;
        int h = pad + objRows * chH + wallRows * chH + floorRows * (cell + pad) + pad;

        PixelCanvas out = new PixelCanvas(w, h);
        out.rect(0, 0, w, h, PixelCanvas.rgb(24, 24, 30));

        int y = pad;
        for (int i = 0; i < SpriteAtlas.OBJECTS; i++) {
            int x = pad + (i % cols) * cw;
            int row = i / cols;
            // checker under the sprite: light on one half, dark on the other, so
            // both the dark rim and the light rim are readable
            out.rect(x, y + row * chH, cell / 2, boxH, PixelCanvas.rgb(96, 84, 62));
            out.rect(x + cell / 2, y + row * chH, cell - cell / 2, boxH,
                    PixelCanvas.rgb(38, 40, 50));
            blit(atlas.canvas, atlas.objectSlot(i), out, x, y + row * chH, cell, boxH);
        }
        y += objRows * chH;

        for (int mask = 0; mask < 16; mask++) {
            int x = pad + (mask % cols) * cw;
            int row = mask / cols;
            out.rect(x, y + row * chH, cell, boxH, PixelCanvas.rgb(38, 40, 50));
            blit(atlas.canvas, atlas.wallSlot(mask), out, x, y + row * chH, cell, boxH);
        }
        y += wallRows * chH;

        for (int m = 0; m < floorRows; m++) {
            for (int v = 0; v < 4; v++) {
                blit(atlas.canvas, atlas.floorSlot(m, v), out,
                        pad + v * cw, y + m * (cell + pad), cell, cell);
            }
        }

        return toImage(out, zoom == 0 ? 1 : zoom);
    }

    private static void blit(PixelCanvas src, float[] slot, PixelCanvas dst,
                             int dx, int dy, int w, int h) {
        int sx = (int) slot[4];
        int sy = (int) slot[5];
        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                dst.blend(dx + i, dy + j, src.get(sx + i, sy + j));
            }
        }
    }

    /** Nearest-neighbour upscale onto a magenta backdrop, so alpha shows up. */
    private static BufferedImage toImage(PixelCanvas canvas, int zoom) {
        BufferedImage img = new BufferedImage(canvas.w * zoom, canvas.h * zoom,
                BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < canvas.h * zoom; y++) {
            for (int x = 0; x < canvas.w * zoom; x++) {
                int c = canvas.get(x / zoom, y / zoom);
                if (((c >>> 24) & 0xFF) == 0) {
                    c = 0xFF200820;
                }
                img.setRGB(x, y, c);
            }
        }
        return img;
    }

    private static void write(File file, BufferedImage img) throws IOException {
        ImageIO.write(img, "png", file);
    }
}
