package com.nuclearunicorn.serialkiller.render;

import com.nuclearunicorn.serialkiller.game.world.RLTile;
import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.*;

/**
 * The pixel-art layer, generated at run time.
 *
 * There are no image assets in this project, so every tile and object sprite is
 * rasterised into one texture atlas at startup by {@link PixelCanvas} and then
 * drawn as plain textured quads under the ASCII glyphs. Art resolution equals
 * the cell size, so pixels land 1:1 on screen; changing RenderConfig.CELL
 * re-bakes the atlas rather than scaling it.
 *
 * Walls are baked as 16 auto-tile variants — one per N/E/S/W neighbour mask —
 * so a wall run connects into the |, L, T and + joints of box drawing, already
 * extruded into a top face plus a front lip.
 */
public final class SpriteAtlas {

    // floor materials
    public static final int MAT_ASPHALT = 0;
    public static final int MAT_SIDEWALK = 1;
    public static final int MAT_WOOD = 2;
    public static final int MAT_CARPET = 3;
    public static final int MAT_GRASS = 4;
    public static final int MAT_DIRT = 5;
    private static final int MATERIALS = 6;
    private static final int VARIANTS = 4;

    // object sprites
    public static final int OBJ_HUMAN = 0;
    public static final int OBJ_CORPSE = 1;
    public static final int OBJ_LAMP = 2;
    public static final int OBJ_DOOR = 3;
    public static final int OBJ_DOOR_OPEN = 4;
    public static final int OBJ_WINDOW = 5;
    public static final int OBJ_TREE = 6;
    public static final int OBJ_GRASS = 7;
    public static final int OBJ_ITEM = 8;
    public static final int OBJ_BOX_LOW = 9;
    public static final int OBJ_BOX_TALL = 10;
    public static final int OBJ_LADDER = 11;
    public static final int OBJ_SHADOW = 12;
    public static final int OBJ_DOOR_NS = 13;      // door in a north-south wall
    public static final int OBJ_WINDOW_NS = 14;    // window in a north-south wall
    private static final int OBJECTS = 15;

    private static final int ATLAS_W = 512;
    private static final int ATLAS_H = 512;

    /** Base colour of masonry: cool grey, the mock's palette. */
    private static final int WALL = PixelCanvas.rgb(122, 128, 142);

    private static SpriteAtlas instance;

    private final int cell;
    private final int boxH;
    private final PixelCanvas canvas = new PixelCanvas(ATLAS_W, ATLAS_H);
    private final float[][] floors = new float[MATERIALS * VARIANTS][];
    private final float[][] walls = new float[16][];
    private final float[][] objects = new float[OBJECTS][];
    private int textureId = 0;

    private int penX = 1, penY = 1, shelfH = 0;

    private SpriteAtlas(int cell) {
        this.cell = cell;
        this.boxH = RenderConfig.spriteH();
        bake();
        upload();
    }

    public static SpriteAtlas get() {
        if (instance == null || instance.cell != RenderConfig.CELL) {
            instance = new SpriteAtlas(RenderConfig.CELL);
        }
        return instance;
    }

    // ------------------------------------------------------------ atlas slots

    /** Shelf packer. Returns {u0,v0,u1,v1,x,y} for a freshly claimed slot. */
    private float[] slot(int w, int h) {
        if (penX + w + 1 > ATLAS_W) {
            penX = 1;
            penY += shelfH + 1;
            shelfH = 0;
        }
        float[] uv = new float[]{
                penX / (float) ATLAS_W,
                penY / (float) ATLAS_H,
                (penX + w) / (float) ATLAS_W,
                (penY + h) / (float) ATLAS_H,
                penX, penY
        };
        penX += w + 1;
        shelfH = Math.max(shelfH, h);
        return uv;
    }

    // ------------------------------------------------------------------ bake

    private void bake() {
        for (int m = 0; m < MATERIALS; m++) {
            for (int v = 0; v < VARIANTS; v++) {
                float[] uv = slot(cell, cell);
                floors[m * VARIANTS + v] = uv;
                bakeFloor(m, v, (int) uv[4], (int) uv[5]);
            }
        }
        for (int mask = 0; mask < 16; mask++) {
            float[] uv = slot(cell, boxH);
            walls[mask] = uv;
            bakeWall(mask, (int) uv[4], (int) uv[5]);
        }
        for (int o = 0; o < OBJECTS; o++) {
            float[] uv = slot(cell, boxH);
            objects[o] = uv;
            bakeObject(o, (int) uv[4], (int) uv[5]);
        }
    }

    private void bakeFloor(int material, int variant, int ox, int oy) {
        int seed = material * 7 + variant * 13 + 1;
        switch (material) {
            case MAT_ASPHALT: {
                canvas.speckle(ox, oy, cell, cell, PixelCanvas.rgb(74, 74, 84), 0.30f, seed);
                // scattered grit and the odd crack
                for (int k = 0; k < cell / 3; k++) {
                    int x = (int) (Palette.hash(seed * 3 + k, variant) * cell);
                    int y = (int) (Palette.hash(variant, seed * 5 + k) * cell);
                    canvas.set(ox + x, oy + y, PixelCanvas.rgb(96, 96, 106));
                }
                break;
            }
            case MAT_SIDEWALK: {
                canvas.speckle(ox, oy, cell, cell, PixelCanvas.rgb(112, 112, 116), 0.16f, seed);
                int seam = PixelCanvas.rgb(84, 84, 88);
                if (variant % 2 == 0) {
                    canvas.hline(ox, oy, cell, seam);
                }
                if (variant < 2) {
                    canvas.vline(ox, oy, cell, seam);
                }
                break;
            }
            case MAT_WOOD: {
                int plank = Math.max(3, cell / 4);
                int base = PixelCanvas.rgb(126, 96, 62);
                for (int y = 0; y < cell; y++) {
                    boolean seam = (y + variant) % plank == 0;
                    float row = 0.86f + Palette.hash(variant, (y + variant) / plank) * 0.28f;
                    for (int x = 0; x < cell; x++) {
                        float n = 0.94f + Palette.hash(x + seed, y) * 0.12f;
                        canvas.set(ox + x, oy + y,
                                PixelCanvas.shade(base, seam ? row * 0.62f : row * n));
                    }
                }
                break;
            }
            case MAT_CARPET: {
                canvas.speckle(ox, oy, cell, cell, PixelCanvas.rgb(104, 74, 78), 0.22f, seed);
                if (variant == 3) {
                    canvas.outline(ox, oy, cell, cell,
                            PixelCanvas.shade(PixelCanvas.rgb(104, 74, 78), 0.8f));
                }
                break;
            }
            case MAT_GRASS: {
                canvas.speckle(ox, oy, cell, cell, PixelCanvas.rgb(64, 96, 56), 0.35f, seed);
                for (int k = 0; k < cell / 4; k++) {
                    int x = (int) (Palette.hash(seed + k, variant * 3) * cell);
                    int y = (int) (Palette.hash(variant * 5, seed + k) * (cell - 3));
                    canvas.vline(ox + x, oy + y, 3, PixelCanvas.rgb(86, 124, 68));
                }
                break;
            }
            default: {
                canvas.speckle(ox, oy, cell, cell, PixelCanvas.rgb(104, 92, 76), 0.28f, seed);
                break;
            }
        }
    }

    /**
     * Wall auto-tile. The 3x3 stencil is extruded upwards by the box rise: the
     * raised copy is the top face, the strip it left behind is the front face.
     */
    private void bakeWall(int mask, int ox, int oy) {
        int rise = RenderConfig.riseH();
        int t = thickness();
        int lead = lead();
        int trail = cell - lead - t;

        boolean n = (mask & 1) != 0;
        boolean e = (mask & 2) != 0;
        boolean s = (mask & 4) != 0;
        boolean w = (mask & 8) != 0;

        int face = WALL;

        // Each stencil column is one contiguous vertical run. A side column also
        // claims the corner square when both of its neighbours are walls, so an
        // L joint is solid and a filled block of walls has no holes in it.
        if (w) {
            column(ox, oy, 0, lead, rise, face, n && w, s && w);
        }
        column(ox, oy, lead, t, rise, face, n, s, (w ? 0 : CAP_LEFT) | (e ? 0 : CAP_RIGHT));
        if (e) {
            column(ox, oy, lead + t, trail, rise, face, n && e, s && e);
        }
    }

    private void column(int ox, int oy, int x, int w, int rise, int color,
                        boolean toTop, boolean toBottom) {
        column(ox, oy, x, w, rise, color, toTop, toBottom, 0);
    }

    private void column(int ox, int oy, int x, int w, int rise, int color,
                        boolean toTop, boolean toBottom, int sideCaps) {
        int t = thickness();
        int lead = lead();
        int top = toTop ? 0 : lead;
        int bottom = toBottom ? cell : lead + t;
        int caps = sideCaps | (toTop ? 0 : CAP_TOP) | (toBottom ? 0 : CAP_BOTTOM);
        slab(ox, oy, x, top, w, bottom - top, rise, color, caps);
    }

    private static final int CAP_TOP = 1;
    private static final int CAP_BOTTOM = 2;
    private static final int CAP_LEFT = 4;
    private static final int CAP_RIGHT = 8;

    private int thickness() {
        return Math.round(cell * 0.62f);
    }

    private int lead() {
        return (cell - thickness()) / 2;
    }

    /**
     * One extruded stencil column. x,y are cell-space; sprite space is the same
     * grid shifted down by the rise, so the top face lands at y and the front
     * face hangs directly below it.
     */
    private void slab(int ox, int oy, int x, int y, int w, int h, int rise, int color, int caps) {
        if (w <= 0 || h <= 0) {
            return;
        }
        // front face: brickwork, darker, lit from above
        canvas.gradient(ox + x, oy + y + h, w, rise, PixelCanvas.shade(color, 0.9f), 1.0f, 0.68f);
        int brick = PixelCanvas.shade(color, 0.62f);
        for (int by = 2; by < rise; by += 3) {
            canvas.hline(ox + x, oy + y + h + by, w, brick);
        }
        // top face
        canvas.speckle(ox + x, oy + y, w, h, color, 0.14f, 3);
        if ((caps & CAP_TOP) != 0) {
            canvas.hline(ox + x, oy + y, w, PixelCanvas.shade(color, 1.35f));
        }
        if ((caps & CAP_BOTTOM) != 0) {
            canvas.hline(ox + x, oy + y + h - 1, w, PixelCanvas.shade(color, 1.2f));
        }
        // dark sides give the slab an edge against the floor
        int edge = PixelCanvas.shade(color, 0.4f);
        if ((caps & CAP_LEFT) != 0) {
            canvas.vline(ox + x, oy + y, h + rise, edge);
        }
        if ((caps & CAP_RIGHT) != 0) {
            canvas.vline(ox + x + w - 1, oy + y, h + rise, edge);
        }
    }

    private void bakeObject(int kind, int ox, int oy) {
        int rise = RenderConfig.riseH();
        int base = oy + boxH;           // floor line of the cell
        int mid = ox + cell / 2;

        switch (kind) {
            case OBJ_SHADOW:
                canvas.softEllipse(mid, base - cell * 0.12f, cell * 0.36f, cell * 0.16f,
                        0, 0, 0, 170);
                break;

            case OBJ_HUMAN: {
                int body = PixelCanvas.rgb(215, 215, 225);
                int dark = PixelCanvas.shade(body, 0.55f);
                int legH = Math.max(3, cell / 4);
                int torsoH = Math.max(4, cell / 3);
                int headR = Math.max(2, cell / 7);

                int feet = base - 1;
                // legs
                canvas.rect(mid - Math.max(2, cell / 8), feet - legH,
                        Math.max(1, cell / 10), legH, dark);
                canvas.rect(mid + Math.max(1, cell / 10), feet - legH,
                        Math.max(1, cell / 10), legH, dark);
                // torso, slightly tapered
                canvas.rect(mid - Math.max(2, cell / 6), feet - legH - torsoH,
                        Math.max(4, cell / 3), torsoH, body);
                canvas.rect(mid - Math.max(2, cell / 6), feet - legH - torsoH,
                        Math.max(4, cell / 3), 1, PixelCanvas.shade(body, 1.2f));
                // arms
                canvas.rect(mid - Math.max(3, cell / 4), feet - legH - torsoH + 1,
                        Math.max(1, cell / 12), torsoH - 1, dark);
                canvas.rect(mid + Math.max(2, cell / 6), feet - legH - torsoH + 1,
                        Math.max(1, cell / 12), torsoH - 1, dark);
                // head
                canvas.ellipse(mid, feet - legH - torsoH - headR, headR, headR, body);
                canvas.ellipse(mid, feet - legH - torsoH - headR - 1, headR, headR * 0.6f,
                        PixelCanvas.shade(body, 0.7f));   // hair/cap
                break;
            }

            case OBJ_CORPSE: {
                int body = PixelCanvas.rgb(190, 170, 170);
                canvas.ellipse(mid, base - cell * 0.2f, cell * 0.34f, cell * 0.16f, body);
                canvas.ellipse(mid + cell * 0.22f, base - cell * 0.24f, cell * 0.11f,
                        cell * 0.11f, PixelCanvas.shade(body, 0.85f));
                break;
            }

            case OBJ_LAMP: {
                int pole = PixelCanvas.rgb(120, 120, 130);
                int glow = PixelCanvas.rgb(255, 226, 150);
                canvas.rect(mid - 1, oy + rise / 2, 2, boxH - rise / 2 - 1, pole);
                canvas.rect(mid - 1, oy + rise / 2, Math.max(3, cell / 4), 2, pole);
                canvas.ellipse(mid + cell / 8.0f, oy + rise / 2 + 3,
                        Math.max(2, cell / 8.0f), Math.max(2, cell / 10.0f), glow);
                canvas.rect(mid - 2, base - 2, 5, 2, PixelCanvas.shade(pole, 0.7f));
                break;
            }

            // A door fills the gap it was punched into, so it is drawn as a wall
            // slab in wood: across the cell for an east-west wall, along it for
            // a north-south one.
            case OBJ_DOOR: {
                int wood = PixelCanvas.rgb(146, 100, 56);
                slab(ox, oy, 0, lead(), cell, thickness(), rise, wood, CAP_TOP | CAP_BOTTOM);
                canvas.rect(ox + 3, oy + lead() + 2, cell - 6, thickness() - 4,
                        PixelCanvas.shade(wood, 0.86f));
                canvas.set(ox + cell - 5, oy + lead() + thickness() / 2,
                        PixelCanvas.rgb(240, 220, 140));
                break;
            }

            case OBJ_DOOR_NS: {
                int wood = PixelCanvas.rgb(146, 100, 56);
                slab(ox, oy, lead(), 0, thickness(), cell, rise, wood, CAP_LEFT | CAP_RIGHT);
                canvas.rect(ox + lead() + 2, oy + 3, thickness() - 4, cell - 6,
                        PixelCanvas.shade(wood, 0.86f));
                break;
            }

            case OBJ_DOOR_OPEN: {
                int wood = PixelCanvas.rgb(146, 100, 56);
                slab(ox, oy, 0, lead(), Math.max(3, cell / 4), thickness(), rise, wood,
                        CAP_TOP | CAP_BOTTOM | CAP_RIGHT);
                break;
            }

            // The generator punches the wall out before dropping a window entity
            // on the tile, so the sprite has to rebuild the missing wall segment
            // and inset the glass into it — otherwise the run shows a gap.
            case OBJ_WINDOW: {
                int pane = PixelCanvas.rgb(150, 190, 215);
                slab(ox, oy, 0, lead(), cell, thickness(), rise, WALL, CAP_TOP | CAP_BOTTOM);
                int y0 = oy + lead() + thickness();          // the slab's front face
                canvas.gradient(ox + 3, y0 + 2, cell - 6, rise - 4, pane, 1.1f, 0.55f);
                canvas.outline(ox + 3, y0 + 2, cell - 6, rise - 4, PixelCanvas.shade(WALL, 0.7f));
                canvas.vline(ox + cell / 2, y0 + 2, rise - 4, PixelCanvas.shade(WALL, 0.7f));
                break;
            }

            case OBJ_WINDOW_NS: {
                int pane = PixelCanvas.rgb(150, 190, 215);
                slab(ox, oy, lead(), 0, thickness(), cell, rise, WALL, CAP_LEFT | CAP_RIGHT);
                int x0 = ox + lead() + 2;
                int ww = thickness() - 4;
                canvas.gradient(x0, oy + 3, ww, cell - 6, pane, 1.0f, 0.78f);
                canvas.outline(x0, oy + 3, ww, cell - 6, PixelCanvas.shade(WALL, 0.7f));
                canvas.hline(x0, oy + cell / 2, ww, PixelCanvas.shade(WALL, 0.7f));
                break;
            }

            case OBJ_TREE: {
                int trunk = PixelCanvas.rgb(96, 68, 44);
                int leaf = PixelCanvas.rgb(72, 116, 60);
                canvas.rect(mid - 1, base - cell / 2, 3, cell / 2, trunk);
                canvas.ellipse(mid, oy + boxH * 0.36f, cell * 0.42f, cell * 0.40f, leaf);
                canvas.ellipse(mid - cell * 0.1f, oy + boxH * 0.30f, cell * 0.24f,
                        cell * 0.22f, PixelCanvas.shade(leaf, 1.25f));
                break;
            }

            case OBJ_GRASS: {
                int leaf = PixelCanvas.rgb(96, 130, 70);
                for (int k = 0; k < 5; k++) {
                    int x = (int) (ox + 3 + Palette.hash(k, 7) * (cell - 6));
                    int len = (int) (cell * (0.16f + Palette.hash(k, 11) * 0.2f));
                    canvas.vline(x, base - 1 - len, len, leaf);
                }
                break;
            }

            case OBJ_ITEM: {
                int c = PixelCanvas.rgb(200, 190, 160);
                canvas.rect(mid - cell / 8, base - cell / 5, cell / 4, cell / 6, c);
                canvas.outline(mid - cell / 8, base - cell / 5, cell / 4, cell / 6,
                        PixelCanvas.shade(c, 0.6f));
                break;
            }

            case OBJ_BOX_LOW: {
                int c = PixelCanvas.rgb(190, 190, 190);
                int hh = Math.max(5, (int) (cell * 0.55f));
                int y0 = base - hh;
                canvas.rect(ox + 2, y0 - Math.max(2, cell / 8), cell - 4,
                        Math.max(2, cell / 8), PixelCanvas.shade(c, 1.25f));   // top face
                canvas.gradient(ox + 2, y0, cell - 4, hh, c, 0.95f, 0.55f);    // front face
                canvas.outline(ox + 2, y0 - Math.max(2, cell / 8), cell - 4,
                        hh + Math.max(2, cell / 8), PixelCanvas.shade(c, 0.45f));
                break;
            }

            case OBJ_BOX_TALL: {
                int c = PixelCanvas.rgb(190, 190, 190);
                int hh = boxH - rise / 2;
                int y0 = base - hh;
                canvas.rect(ox + 2, y0, cell - 4, Math.max(2, cell / 8),
                        PixelCanvas.shade(c, 1.25f));
                canvas.gradient(ox + 2, y0 + Math.max(2, cell / 8), cell - 4,
                        hh - Math.max(2, cell / 8), c, 0.95f, 0.5f);
                canvas.outline(ox + 2, y0, cell - 4, hh, PixelCanvas.shade(c, 0.45f));
                canvas.hline(ox + 3, y0 + hh / 2, cell - 6, PixelCanvas.shade(c, 0.6f));
                break;
            }

            case OBJ_LADDER: {
                int c = PixelCanvas.rgb(170, 150, 110);
                canvas.vline(ox + cell / 3, base - cell, cell, c);
                canvas.vline(ox + cell - cell / 3, base - cell, cell, c);
                for (int y = base - cell + 2; y < base; y += Math.max(3, cell / 6)) {
                    canvas.hline(ox + cell / 3, y, cell - 2 * (cell / 3) + 1, c);
                }
                break;
            }

            default:
                break;
        }
    }

    // ---------------------------------------------------------------- upload

    private void upload() {
        ByteBuffer buf = BufferUtils.createByteBuffer(ATLAS_W * ATLAS_H * 4);
        for (int i = 0; i < canvas.px.length; i++) {
            int c = canvas.px[i];
            buf.put((byte) ((c >> 16) & 0xFF));
            buf.put((byte) ((c >> 8) & 0xFF));
            buf.put((byte) (c & 0xFF));
            buf.put((byte) ((c >>> 24) & 0xFF));
        }
        buf.flip();

        textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, ATLAS_W, ATLAS_H, 0, GL_RGBA,
                GL_UNSIGNED_BYTE, buf);
    }

    public void bind() {
        glBindTexture(GL_TEXTURE_2D, textureId);
    }

    // ----------------------------------------------------------------- draw

    /** Opens a quad batch bound to the atlas. */
    public static void beginSprites() {
        get().bind();
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glColor4f(1, 1, 1, 1);
        glBegin(GL_QUADS);
    }

    public static void endSprites() {
        glEnd();
    }

    public void floor(RLTile tile, int i, int j, int x, int y, int size) {
        int material = materialOf(tile, i, j);
        int variant = (int) (Palette.hash(i, j) * VARIANTS) % VARIANTS;
        float[] uv = floors[material * VARIANTS + variant];

        float blood = tile.getBloodAmt();
        if (blood > 0.01f) {
            glColor4f(1.0f, 1.0f - blood * 0.85f, 1.0f - blood * 0.85f, 1.0f);
        } else {
            glColor4f(1, 1, 1, 1);
        }
        Draw.sprite(x, y, size, size, uv[0], uv[1], uv[2], uv[3]);
    }

    private int materialOf(RLTile tile, int i, int j) {
        RLTile.TileType type = tile.getTileType();
        if (type == RLTile.TileType.ROAD) {
            return isSidewalk(tile) ? MAT_SIDEWALK : MAT_ASPHALT;
        }
        if (type == RLTile.TileType.GRASS) {
            return MAT_GRASS;
        }
        if (tile.isIndoor()) {
            // vary the flooring per building-ish area so rooms don't all match
            return Palette.hash(i >> 3, j >> 3) < 0.35f ? MAT_CARPET : MAT_WOOD;
        }
        return MAT_DIRT;
    }

    private static boolean isSidewalk(RLTile tile) {
        org.newdawn.slick.Color c = tile.getTileModelColor();
        return c != null && c.b > 0.1f;
    }

    public void wall(TileWindow view, int i, int j) {
        int mask = 0;
        if (view.isKnownWall(i, j - 1)) mask |= 1;
        if (view.isKnownWall(i + 1, j)) mask |= 2;
        if (view.isKnownWall(i, j + 1)) mask |= 4;
        if (view.isKnownWall(i - 1, j)) mask |= 8;

        float[] uv = walls[mask];
        glColor4f(1, 1, 1, 1);
        Draw.sprite(Grid.cellX(i), Grid.boxTop(j), cell, boxH, uv[0], uv[1], uv[2], uv[3]);
    }

    /**
     * Sprites that are generic shapes take the entity's colour; the ones baked
     * with their own palette (doors, windows, lamps, foliage) ignore it.
     */
    public static boolean isTinted(int kind) {
        return kind == OBJ_BOX_LOW || kind == OBJ_BOX_TALL || kind == OBJ_HUMAN
                || kind == OBJ_CORPSE || kind == OBJ_ITEM;
    }

    /** Draws an object sprite standing in the cell whose top-left is (x,y). */
    public void object(int kind, float x, float y, float r, float g, float b, float a) {
        float[] uv = objects[kind];
        glColor4f(r, g, b, a);
        Draw.sprite(x, y - RenderConfig.riseH(), cell, boxH, uv[0], uv[1], uv[2], uv[3]);
    }
}
