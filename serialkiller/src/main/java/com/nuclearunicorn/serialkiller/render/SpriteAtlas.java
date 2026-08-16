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
 *
 * Actors are baked in three layers — a complete neutral figure, then coat-only
 * and hair-only masks that are tinted per NPC — so a crowd is not uniform
 * without needing one baked sprite per person. A fourth layer is the same
 * figure as a rimmed silhouette, for actors the player remembers but cannot
 * currently see.
 *
 * {@code AtlasDump} renders all of this to a PNG without a GL context, which is
 * how the art is iterated on.
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
    public static final int OBJ_SHADOW = 0;
    public static final int OBJ_PERSON = 1;        // complete figure, neutral palette
    public static final int OBJ_PERSON_COAT = 2;   // coat mask, tinted per NPC
    public static final int OBJ_PERSON_HAIR = 3;   // hair mask, tinted per NPC
    public static final int OBJ_PERSON_DARK = 4;   // remembered, out of sight
    public static final int OBJ_CORPSE = 5;
    public static final int OBJ_LAMP = 6;
    public static final int OBJ_DOOR_EW = 7;       // door in an east-west wall
    public static final int OBJ_DOOR_NS = 8;       // door in a north-south wall
    public static final int OBJ_DOOR_EW_OPEN = 9;
    public static final int OBJ_DOOR_NS_OPEN = 10;
    public static final int OBJ_WINDOW_EW = 11;
    public static final int OBJ_WINDOW_NS = 12;
    public static final int OBJ_TREE = 13;
    public static final int OBJ_GRASS = 14;
    public static final int OBJ_ITEM = 15;
    public static final int OBJ_LADDER = 16;
    public static final int OBJ_CRATE = 17;
    public static final int OBJ_SHELF = 18;
    public static final int OBJ_DESK = 19;
    public static final int OBJ_CHAIR = 20;
    public static final int OBJ_BED = 21;
    public static final int OBJ_SOFA = 22;
    public static final int OBJ_FRIDGE = 23;
    public static final int OBJ_COUNTER = 24;
    public static final int OBJ_BATHTUB = 25;
    public static final int OBJ_SAFE = 26;
    public static final int OBJ_BOX_LOW = 27;      // generic fallbacks
    public static final int OBJ_BOX_TALL = 28;
    static final int OBJECTS = 29;

    static final String[] OBJECT_NAMES = {
            "shadow", "person", "person-coat", "person-hair", "person-dark",
            "corpse", "lamp", "door-ew", "door-ns", "door-ew-open", "door-ns-open",
            "window-ew", "window-ns", "tree", "grass", "item", "ladder",
            "crate", "shelf", "desk", "chair", "bed", "sofa", "fridge",
            "counter", "bathtub", "safe", "box-low", "box-tall"
    };

    private static final int ATLAS_W = 1024;
    private static final int ATLAS_H = 1024;

    // ------------------------------------------------------------- palette
    // Masonry reads as a bright lit cap over a near-black face. That contrast
    // is the whole silhouette of a building at night, so it is deliberately
    // wider than a real stone would be.
    private static final int WALL_TOP = PixelCanvas.rgb(112, 119, 138);
    private static final int WALL_FACE = PixelCanvas.rgb(54, 60, 76);
    private static final int WALL_LIP = PixelCanvas.rgb(198, 206, 222);
    private static final int EDGE = PixelCanvas.rgb(13, 14, 19);

    private static final int WOOD = PixelCanvas.rgb(150, 98, 54);
    private static final int WOOD_DARK = PixelCanvas.rgb(78, 48, 27);
    private static final int PANE = PixelCanvas.rgb(246, 226, 168);
    private static final int METAL = PixelCanvas.rgb(150, 156, 168);

    private static final int SKIN = PixelCanvas.rgb(226, 176, 140);
    private static final int TROUSERS = PixelCanvas.rgb(58, 62, 80);
    private static final int SHOE = PixelCanvas.rgb(32, 30, 36);
    private static final int COAT_NEUTRAL = PixelCanvas.rgb(176, 180, 192);
    private static final int HAIR_NEUTRAL = PixelCanvas.rgb(64, 48, 40);

    /** Rim traced around every object sprite, and around remembered actors. */
    private static final int RIM_DARK = PixelCanvas.argb(150, 8, 9, 14);
    private static final int RIM_LIGHT = PixelCanvas.rgb(206, 220, 246);

    private static SpriteAtlas instance;

    private final int cell;
    private final int boxH;
    final PixelCanvas canvas = new PixelCanvas(ATLAS_W, ATLAS_H);
    private final float[][] floors = new float[MATERIALS * VARIANTS][];
    private final float[][] walls = new float[16][];
    private final float[][] objects = new float[OBJECTS][];
    private int textureId = 0;

    private int penX = 1, penY = 1, shelfH = 0;

    private SpriteAtlas(int cell) {
        this.cell = cell;
        this.boxH = RenderConfig.spriteH();
        bake();
    }

    public static SpriteAtlas get() {
        if (instance == null || instance.cell != RenderConfig.CELL) {
            instance = new SpriteAtlas(RenderConfig.CELL);
            instance.upload();
        }
        return instance;
    }

    /** Bakes the atlas without touching GL — for the offscreen dump tool. */
    static SpriteAtlas bakeOffscreen(int cell) {
        return new SpriteAtlas(cell);
    }

    int cellSize() {
        return cell;
    }

    float[] objectSlot(int kind) {
        return objects[kind];
    }

    float[] wallSlot(int mask) {
        return walls[mask];
    }

    float[] floorSlot(int material, int variant) {
        return floors[material * VARIANTS + variant];
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
            objects[o] = slot(cell, boxH);
        }
        for (int o = 0; o < OBJECTS; o++) {
            float[] uv = objects[o];
            bakeObject(o, (int) uv[4], (int) uv[5]);
        }
    }

    // ------------------------------------------------------------------ floor

    private void bakeFloor(int material, int variant, int ox, int oy) {
        int seed = material * 7 + variant * 13 + 1;
        switch (material) {
            case MAT_ASPHALT: {
                canvas.speckle(ox, oy, cell, cell, PixelCanvas.rgb(58, 59, 68), 0.26f, seed);
                // the mock's road is a dotted grid, not noise — it gives the
                // empty street a scale to read distance against
                int dot = PixelCanvas.rgb(78, 80, 92);
                int step = Math.max(4, cell / 5);
                for (int y = step / 2; y < cell; y += step) {
                    for (int x = step / 2; x < cell; x += step) {
                        canvas.set(ox + x, oy + y, dot);
                    }
                }
                for (int k = 0; k < cell / 4; k++) {
                    int x = (int) (Palette.hash(seed * 3 + k, variant) * cell);
                    int y = (int) (Palette.hash(variant, seed * 5 + k) * cell);
                    canvas.set(ox + x, oy + y, PixelCanvas.rgb(90, 92, 104));
                }
                break;
            }
            case MAT_SIDEWALK: {
                canvas.speckle(ox, oy, cell, cell, PixelCanvas.rgb(104, 106, 114), 0.14f, seed);
                int seam = PixelCanvas.rgb(66, 68, 76);
                if (variant % 2 == 0) {
                    canvas.hline(ox, oy, cell, seam);
                    canvas.hline(ox, oy + 1, cell, PixelCanvas.rgb(124, 126, 134));
                }
                if (variant < 2) {
                    canvas.vline(ox, oy, cell, seam);
                }
                break;
            }
            case MAT_WOOD: {
                int plank = Math.max(3, cell / 4);
                int base = PixelCanvas.rgb(122, 92, 60);
                for (int y = 0; y < cell; y++) {
                    boolean seam = (y + variant) % plank == 0;
                    float row = 0.88f + Palette.hash(variant, (y + variant) / plank) * 0.22f;
                    for (int x = 0; x < cell; x++) {
                        float n = 0.95f + Palette.hash(x + seed, y) * 0.10f;
                        canvas.set(ox + x, oy + y,
                                PixelCanvas.shade(base, seam ? row * 0.72f : row * n));
                    }
                }
                break;
            }
            case MAT_CARPET: {
                canvas.speckle(ox, oy, cell, cell, PixelCanvas.rgb(98, 68, 72), 0.20f, seed);
                // a woven cross-hatch, one thread every other pixel
                int thread = PixelCanvas.rgb(112, 80, 84);
                for (int y = 0; y < cell; y += 2) {
                    for (int x = (y / 2) % 2; x < cell; x += 2) {
                        canvas.set(ox + x, oy + y, thread);
                    }
                }
                break;
            }
            case MAT_GRASS: {
                canvas.speckle(ox, oy, cell, cell, PixelCanvas.rgb(58, 88, 52), 0.35f, seed);
                for (int k = 0; k < cell / 4; k++) {
                    int x = (int) (Palette.hash(seed + k, variant * 3) * cell);
                    int y = (int) (Palette.hash(variant * 5, seed + k) * (cell - 3));
                    canvas.vline(ox + x, oy + y, 3, PixelCanvas.rgb(82, 118, 64));
                }
                break;
            }
            default: {
                canvas.speckle(ox, oy, cell, cell, PixelCanvas.rgb(98, 86, 70), 0.26f, seed);
                break;
            }
        }
    }

    // ------------------------------------------------------------------- wall

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

        // Each stencil column is one contiguous vertical run. A side column also
        // claims the corner square when both of its neighbours are walls, so an
        // L joint is solid and a filled block of walls has no holes in it.
        // the side columns reach into the neighbouring tile, so they must not be
        // capped — a cap there chops a continuous run into separate blocks
        if (w) {
            column(ox, oy, 0, lead, rise, n && w, s && w, 0);
        }
        column(ox, oy, lead, t, rise, n, s, (w ? 0 : CAP_LEFT) | (e ? 0 : CAP_RIGHT));
        if (e) {
            column(ox, oy, lead + t, trail, rise, n && e, s && e, 0);
        }
    }

    private void column(int ox, int oy, int x, int w, int rise,
                        boolean toTop, boolean toBottom, int sideCaps) {
        int t = thickness();
        int lead = lead();
        int top = toTop ? 0 : lead;
        int bottom = toBottom ? cell : lead + t;
        int caps = sideCaps | (toTop ? 0 : CAP_TOP) | (toBottom ? 0 : CAP_BOTTOM);
        slab(ox, oy, x, top, w, bottom - top, rise, WALL_TOP, WALL_FACE, caps);
    }

    private static final int CAP_TOP = 1;
    private static final int CAP_BOTTOM = 2;
    private static final int CAP_LEFT = 4;
    private static final int CAP_RIGHT = 8;

    private int thickness() {
        return Math.round(cell * 0.52f);
    }

    private int lead() {
        return (cell - thickness()) / 2;
    }

    /**
     * One extruded stencil column. x,y are cell-space; sprite space is the same
     * grid shifted down by the rise, so the top face lands at y and the front
     * face hangs directly below it.
     *
     * Contrast comes from three hard edges rather than from the fill: a bright
     * lip on the north cap, a near-black crease where the top face folds into
     * the front, and a black contact line where the front meets the floor.
     */
    private void slab(int ox, int oy, int x, int y, int w, int h, int rise,
                      int top, int face, int caps) {
        if (w <= 0 || h <= 0) {
            return;
        }
        // front face: dark, falling off downwards, with brick courses
        canvas.gradient(ox + x, oy + y + h, w, rise, face, 1.0f, 0.42f);
        int brick = PixelCanvas.shade(face, 0.72f);
        for (int by = 3; by < rise - 2; by += Math.max(4, rise / 3)) {
            canvas.hline(ox + x, oy + y + h + by, w, brick);
        }
        // top face
        canvas.speckle(ox + x, oy + y, w, h, top, 0.10f, 3);

        // The fold between the two faces is the strongest line in the sprite —
        // but only draw it where the front face is actually exposed. On a run
        // that continues south the fold falls one pixel above the next tile's
        // top face and shows through as a seam every cell.
        if ((caps & CAP_BOTTOM) != 0) {
            canvas.hline(ox + x, oy + y + h - 1, w, PixelCanvas.shade(top, 0.40f));
            canvas.hline(ox + x, oy + y + h, w, EDGE);
            if (rise > 0) {
                canvas.hline(ox + x, oy + y + h + rise - 1, w, EDGE);
            }
        }

        if ((caps & CAP_TOP) != 0) {
            canvas.hline(ox + x, oy + y, w, EDGE);
            canvas.hline(ox + x, oy + y + 1, w, WALL_LIP);
        }
        if ((caps & CAP_LEFT) != 0) {
            canvas.vline(ox + x, oy + y, h + rise, EDGE);
        }
        if ((caps & CAP_RIGHT) != 0) {
            canvas.vline(ox + x + w - 1, oy + y, h + rise, EDGE);
        }
    }

    // ---------------------------------------------------------------- objects

    private void bakeObject(int kind, int ox, int oy) {
        int rise = RenderConfig.riseH();
        int base = oy + boxH;           // floor line of the cell
        int mid = ox + cell / 2;

        switch (kind) {
            case OBJ_SHADOW:
                canvas.softEllipse(mid, base - cell * 0.10f, cell * 0.38f, cell * 0.16f,
                        0, 0, 0, 180);
                return;                 // no rim: it *is* the soft edge

            case OBJ_PERSON:
                person(ox, oy, PERSON_ALL);
                break;

            case OBJ_PERSON_COAT:
                person(ox, oy, PERSON_COAT);
                return;                 // masks are overlays, the base has the rim

            case OBJ_PERSON_HAIR:
                person(ox, oy, PERSON_HAIR);
                return;

            case OBJ_PERSON_DARK: {
                // the same figure, flattened in place — baking it fresh rather
                // than tracing the finished sprite keeps the dark rim of the
                // living version out of the outline
                person(ox, oy, PERSON_ALL);
                canvas.silhouette(ox, oy, ox, oy, cell, boxH,
                        PixelCanvas.rgb(12, 13, 20), RIM_LIGHT);
                return;
            }

            case OBJ_CORPSE: {
                // seen from above: a prone figure in a pool
                int pool = PixelCanvas.argb(200, 92, 16, 18);
                canvas.softEllipse(mid, base - cell * 0.22f, cell * 0.44f, cell * 0.22f,
                        92, 16, 18, 200);
                canvas.ellipse(mid - cell * 0.06f, base - cell * 0.26f,
                        cell * 0.26f, cell * 0.13f, COAT_NEUTRAL);
                canvas.ellipse(mid + cell * 0.22f, base - cell * 0.30f,
                        cell * 0.10f, cell * 0.10f, SKIN);
                canvas.ellipse(mid - cell * 0.28f, base - cell * 0.22f,
                        cell * 0.10f, cell * 0.07f, TROUSERS);
                canvas.blendRect(mid - 1, (int) (base - cell * 0.26f), 2, 1, pool);
                break;
            }

            case OBJ_LAMP: {
                int pole = PixelCanvas.rgb(96, 100, 112);
                int glow = PixelCanvas.rgb(255, 232, 168);
                int headY = oy + rise / 3;
                canvas.rect(mid - 1, headY, 2, base - headY - 1, pole);
                canvas.rect(mid - 1, headY, Math.max(4, cell / 4), 2, pole);
                int lampX = mid + Math.max(3, cell / 5);
                canvas.rect(lampX - 3, headY + 2, 6, 2, PixelCanvas.shade(pole, 0.7f));
                canvas.ellipse(lampX, headY + 5, Math.max(2, cell / 9.0f),
                        Math.max(2, cell / 11.0f), glow);
                canvas.rect(mid - 3, base - 3, 7, 3, PixelCanvas.shade(pole, 0.6f));
                break;
            }

            // A door fills the gap it was punched into, so it is drawn as a wall
            // slab in wood: across the cell for an east-west wall, along it for
            // a north-south one.
            case OBJ_DOOR_EW: {
                slab(ox, oy, 0, lead(), cell, thickness(), rise, WOOD,
                        PixelCanvas.shade(WOOD, 0.95f), CAP_TOP | CAP_BOTTOM);
                doorLeaf(ox + 2, oy + lead() + thickness() + 2, cell - 4, rise - 4, true);
                return;
            }

            case OBJ_DOOR_NS: {
                // the run goes north-south, so the leaf is seen edge-on from
                // above: a wooden plank lying along the wall, not across it.
                // Its front face is the door's underside, so it stays dark.
                slab(ox, oy, lead(), 0, thickness(), cell, rise, WOOD,
                        WOOD_DARK, CAP_LEFT | CAP_RIGHT);
                doorLeaf(ox + lead() + 2, oy + 3, thickness() - 4, cell - 6, false);
                return;
            }

            case OBJ_DOOR_EW_OPEN: {
                // leaf swung back against the jamb, gap on the right
                int leaf = Math.max(4, cell / 4);
                slab(ox, oy, 0, lead(), leaf, thickness(), rise, WOOD,
                        PixelCanvas.shade(WOOD, 0.95f), CAP_TOP | CAP_BOTTOM | CAP_RIGHT);
                canvas.vline(ox + cell - 1, oy + lead(), thickness() + rise, EDGE);
                return;
            }

            case OBJ_DOOR_NS_OPEN: {
                int leaf = Math.max(4, cell / 4);
                slab(ox, oy, lead(), 0, thickness(), leaf, rise, WOOD,
                        PixelCanvas.shade(WOOD, 0.95f), CAP_LEFT | CAP_RIGHT | CAP_BOTTOM);
                return;
            }

            // The generator punches the wall out before dropping a window entity
            // on the tile, so the sprite has to rebuild the missing wall segment
            // and inset the glass into it — otherwise the run shows a gap.
            case OBJ_WINDOW_EW: {
                slab(ox, oy, 0, lead(), cell, thickness(), rise, WALL_TOP, WALL_FACE,
                        CAP_TOP | CAP_BOTTOM);
                int y0 = oy + lead() + thickness();     // top of the front face
                pane(ox + 3, y0 + 2, cell - 6, rise - 4, 2, 1);
                // sill: a bright ledge under the glass
                canvas.hline(ox + 2, oy + lead() + thickness() + rise - 2, cell - 4,
                        PixelCanvas.shade(WALL_TOP, 0.8f));
                return;
            }

            case OBJ_WINDOW_NS: {
                slab(ox, oy, lead(), 0, thickness(), cell, rise, WALL_TOP, WALL_FACE,
                        CAP_LEFT | CAP_RIGHT);
                pane(ox + lead() + 2, oy + 3, thickness() - 4, cell - 6, 1, 2);
                return;
            }

            case OBJ_TREE: {
                int trunk = PixelCanvas.rgb(88, 62, 40);
                int leaf = PixelCanvas.rgb(62, 104, 54);
                canvas.rect(mid - 2, base - cell / 2, 4, cell / 2, trunk);
                canvas.vline(mid + 1, base - cell / 2, cell / 2,
                        PixelCanvas.shade(trunk, 0.7f));
                canvas.ellipse(mid, oy + boxH * 0.34f, cell * 0.44f, cell * 0.42f, leaf);
                canvas.ellipse(mid - cell * 0.12f, oy + boxH * 0.28f, cell * 0.26f,
                        cell * 0.24f, PixelCanvas.shade(leaf, 1.3f));
                canvas.ellipse(mid + cell * 0.18f, oy + boxH * 0.42f, cell * 0.18f,
                        cell * 0.16f, PixelCanvas.shade(leaf, 0.72f));
                break;
            }

            case OBJ_GRASS: {
                // one tuft, not scattered blades: loose strokes read as artefacts
                int leaf = PixelCanvas.rgb(88, 122, 64);
                int spread = Math.max(4, (int) (cell * 0.34f));
                for (int k = 0; k < 11; k++) {
                    float t = k / 10.0f;
                    int x = mid - spread + (int) (t * spread * 2);
                    int len = (int) (cell * (0.14f + Palette.hash(k, 11) * 0.20f)
                            * (1.0f - Math.abs(t - 0.5f)));
                    if (len < 2) {
                        continue;
                    }
                    int shade = PixelCanvas.shade(leaf, 0.75f + Palette.hash(k, 3) * 0.6f);
                    canvas.vline(x, base - 2 - len, len, shade);
                    // tip catches the light — a detached pixel beside it instead
                    // makes the tuft read as lettering
                    canvas.set(x, base - 2 - len, PixelCanvas.shade(shade, 1.3f));
                }
                canvas.hline(mid - spread / 2, base - 2, spread,
                        PixelCanvas.shade(leaf, 0.55f));
                return;                 // a rim on loose blades just muddies them
            }

            case OBJ_ITEM: {
                int c = PixelCanvas.rgb(214, 202, 168);
                int w = Math.max(5, cell / 4);
                int hgt = Math.max(4, cell / 6);
                canvas.rect(mid - w / 2, base - hgt - 1, w, hgt, c);
                canvas.hline(mid - w / 2, base - hgt - 1, w, PixelCanvas.shade(c, 1.2f));
                canvas.hline(mid - w / 2, base - 2, w, PixelCanvas.shade(c, 0.6f));
                break;
            }

            case OBJ_LADDER: {
                int c = PixelCanvas.rgb(178, 154, 108);
                int x0 = ox + cell / 4;
                int x1 = ox + cell - cell / 4 - 1;
                canvas.rect(x0, base - cell, 2, cell, c);
                canvas.rect(x1, base - cell, 2, cell, c);
                for (int y = base - cell + 2; y < base - 1; y += Math.max(3, cell / 7)) {
                    canvas.hline(x0, y, x1 - x0 + 2, PixelCanvas.shade(c, 0.82f));
                }
                break;
            }

            case OBJ_CRATE: {
                // slatted wooden crate with a corner brace, straight off the mock
                int wd = cell - 6;
                int hgt = Math.max(7, (int) (cell * 0.46f));
                int depth = Math.max(3, (int) (cell * 0.18f));
                box(ox + 3, base, wd, hgt, depth, PixelCanvas.shade(WOOD, 1.15f), WOOD);
                int slat = PixelCanvas.shade(WOOD, 0.52f);
                int lit = PixelCanvas.shade(WOOD, 1.3f);
                int y0 = base - hgt;
                canvas.hline(ox + 3, y0 + hgt / 3, wd, slat);
                canvas.hline(ox + 3, y0 + hgt / 3 + 1, wd, lit);
                canvas.hline(ox + 3, y0 + 2 * hgt / 3, wd, slat);
                canvas.hline(ox + 3, y0 + 2 * hgt / 3 + 1, wd, lit);
                canvas.line(ox + 4, base - 3, ox + 3 + wd - 2, y0 + 1, lit);
                canvas.line(ox + 4, y0 + 1, ox + 3 + wd - 2, base - 3, slat);
                break;
            }

            case OBJ_SHELF: {
                // tall, against a wall: dark carcass, lit shelf edges, clutter
                int wd = cell - 4;
                int hgt = boxH - rise / 2;
                int depth = Math.max(3, (int) (cell * 0.14f));
                box(ox + 2, base, wd, hgt, depth, PixelCanvas.shade(WOOD_DARK, 1.5f),
                        WOOD_DARK);
                int y0 = base - hgt;
                int shelves = 3;
                for (int k = 1; k <= shelves; k++) {
                    int y = y0 + k * hgt / (shelves + 1);
                    canvas.hline(ox + 3, y, wd - 2, PixelCanvas.shade(WOOD, 1.05f));
                    canvas.hline(ox + 3, y + 1, wd - 2, EDGE);
                    // a couple of things stored on it
                    for (int b = 0; b < 3; b++) {
                        if (Palette.hash(k * 5, b) < 0.45f) {
                            continue;
                        }
                        int bx = ox + 4 + b * (wd - 4) / 3;
                        int bh = 2 + (int) (Palette.hash(b, k) * 3);
                        canvas.rect(bx, y - bh, Math.max(2, (wd - 6) / 4), bh,
                                PixelCanvas.mix(PANE, WOOD, Palette.hash(k, b)));
                    }
                }
                break;
            }

            case OBJ_DESK: {
                int wd = cell - 4;
                int hgt = Math.max(6, (int) (cell * 0.34f));
                int depth = Math.max(4, (int) (cell * 0.26f));
                int top = PixelCanvas.shade(WOOD, 1.2f);
                box(ox + 2, base, wd, hgt, depth, top, PixelCanvas.shade(WOOD, 0.8f));
                int y0 = base - hgt - depth;
                // a sheet of paper and a drawer front
                canvas.rect(ox + 5, y0 + 1, Math.max(4, cell / 5), Math.max(3, depth - 2),
                        PixelCanvas.rgb(226, 224, 210));
                canvas.rect(ox + 4, base - hgt + 2, wd - 4, Math.max(2, hgt / 3),
                        PixelCanvas.shade(WOOD, 0.62f));
                canvas.hline(ox + 4 + wd / 3, base - hgt + 3, Math.max(3, wd / 3), METAL);
                break;
            }

            case OBJ_CHAIR: {
                int wd = Math.max(8, (int) (cell * 0.52f));
                int x0 = mid - wd / 2;
                int seatH = Math.max(3, (int) (cell * 0.14f));
                int backH = Math.max(6, (int) (cell * 0.34f));
                int depth = Math.max(3, (int) (cell * 0.18f));
                // backrest first: it stands behind the seat, so the seat's top
                // face has to overlap its foot
                int by = base - 3 - seatH - depth - backH;
                canvas.gradient(x0 + 1, by, wd - 2, backH + depth,
                        PixelCanvas.shade(WOOD, 0.72f), 1.15f, 0.7f);
                canvas.hline(x0 + 1, by, wd - 2, PixelCanvas.shade(WOOD, 1.35f));
                canvas.vline(x0 + wd / 2, by + 2, backH - 3, PixelCanvas.shade(WOOD, 0.5f));
                // legs
                canvas.vline(x0 + 1, base - seatH - 3, 3, WOOD_DARK);
                canvas.vline(x0 + wd - 2, base - seatH - 3, 3, WOOD_DARK);
                box(x0, base - 3, wd, seatH, depth, PixelCanvas.shade(WOOD, 1.15f), WOOD);
                break;
            }

            case OBJ_BED: {
                int wd = cell - 4;
                int hgt = Math.max(4, (int) (cell * 0.18f));
                int depth = Math.max(8, (int) (cell * 0.48f));
                box(ox + 2, base, wd, hgt, depth, PixelCanvas.rgb(96, 104, 132),
                        PixelCanvas.shade(WOOD_DARK, 1.1f));
                int y0 = base - hgt - depth;
                // pillow at the head, blanket over the rest
                canvas.rect(ox + 3, y0 + 1, wd - 2, Math.max(3, depth / 3),
                        PixelCanvas.rgb(224, 222, 214));
                canvas.rect(ox + 3, y0 + Math.max(3, depth / 3) + 1, wd - 2,
                        depth - Math.max(3, depth / 3) - 2, PixelCanvas.rgb(104, 66, 72));
                canvas.hline(ox + 3, y0 + Math.max(3, depth / 3) + 1, wd - 2,
                        PixelCanvas.rgb(138, 90, 96));
                break;
            }

            case OBJ_SOFA: {
                int wd = cell - 2;
                int hgt = Math.max(5, (int) (cell * 0.22f));
                int depth = Math.max(6, (int) (cell * 0.34f));
                int fabric = PixelCanvas.rgb(96, 78, 128);
                box(ox + 1, base, wd, hgt, depth, PixelCanvas.shade(fabric, 1.2f), fabric);
                int y0 = base - hgt - depth;
                // arms and back
                canvas.rect(ox + 1, y0 - 3, 3, depth + 3, PixelCanvas.shade(fabric, 1.35f));
                canvas.rect(ox + wd - 2, y0 - 3, 3, depth + 3,
                        PixelCanvas.shade(fabric, 1.35f));
                canvas.rect(ox + 4, y0 - 3, wd - 6, 4, PixelCanvas.shade(fabric, 1.45f));
                canvas.vline(mid, y0 + 3, depth - 3, PixelCanvas.shade(fabric, 0.7f));
                break;
            }

            case OBJ_FRIDGE: {
                int wd = Math.max(10, (int) (cell * 0.62f));
                int x0 = mid - wd / 2;
                int hgt = boxH - rise / 2 - 2;
                int depth = Math.max(3, (int) (cell * 0.14f));
                int white = PixelCanvas.rgb(206, 210, 214);
                box(x0, base, wd, hgt, depth, PixelCanvas.shade(white, 1.1f), white);
                int y0 = base - hgt;
                canvas.hline(x0, y0 + hgt / 3, wd, PixelCanvas.shade(white, 0.55f));
                canvas.vline(x0 + wd - 3, y0 + 2, hgt / 3 - 3, METAL);
                canvas.vline(x0 + wd - 3, y0 + hgt / 3 + 3, hgt / 4, METAL);
                break;
            }

            case OBJ_COUNTER: {
                int wd = cell - 2;
                int hgt = Math.max(7, (int) (cell * 0.40f));
                int depth = Math.max(3, (int) (cell * 0.16f));
                int cab = PixelCanvas.rgb(120, 96, 70);
                box(ox + 1, base, wd, hgt, depth, PixelCanvas.rgb(178, 182, 188), cab);
                int y0 = base - hgt;
                // two cabinet doors with a shadow gap between them
                canvas.vline(mid, y0 + 1, hgt - 2, PixelCanvas.shade(cab, 0.5f));
                canvas.hline(ox + 2, y0 + 2, wd - 2, PixelCanvas.shade(cab, 1.2f));
                break;
            }

            case OBJ_BATHTUB: {
                int wd = cell - 4;
                int hgt = Math.max(5, (int) (cell * 0.24f));
                int depth = Math.max(6, (int) (cell * 0.36f));
                int enamel = PixelCanvas.rgb(214, 218, 228);
                box(ox + 2, base, wd, hgt, depth, enamel, PixelCanvas.shade(enamel, 0.86f));
                int y0 = base - hgt - depth;
                // the water inside, inset from the rim
                canvas.rect(ox + 4, y0 + 2, wd - 4, depth - 3, PixelCanvas.rgb(96, 132, 156));
                canvas.hline(ox + 4, y0 + 2, wd - 4, PixelCanvas.rgb(132, 172, 196));
                break;
            }

            case OBJ_SAFE: {
                int wd = Math.max(9, (int) (cell * 0.56f));
                int x0 = mid - wd / 2;
                int hgt = Math.max(8, (int) (cell * 0.48f));
                int depth = Math.max(3, (int) (cell * 0.14f));
                int steel = PixelCanvas.rgb(86, 90, 100);
                box(x0, base, wd, hgt, depth, PixelCanvas.shade(steel, 1.3f), steel);
                int y0 = base - hgt;
                canvas.outline(x0 + 2, y0 + 2, wd - 4, hgt - 4,
                        PixelCanvas.shade(steel, 1.5f));
                canvas.ellipse(x0 + wd * 0.5f, y0 + hgt * 0.55f, 2.2f, 2.2f,
                        PixelCanvas.rgb(206, 196, 120));
                break;
            }

            case OBJ_BOX_LOW: {
                int c = PixelCanvas.rgb(188, 190, 196);
                int wd = cell - 4;
                int hgt = Math.max(6, (int) (cell * 0.40f));
                int depth = Math.max(3, (int) (cell * 0.16f));
                box(ox + 2, base, wd, hgt, depth, PixelCanvas.shade(c, 1.2f), c);
                break;
            }

            case OBJ_BOX_TALL: {
                int c = PixelCanvas.rgb(188, 190, 196);
                int wd = cell - 4;
                int hgt = boxH - rise / 2;
                int depth = Math.max(3, (int) (cell * 0.14f));
                box(ox + 2, base, wd, hgt, depth, PixelCanvas.shade(c, 1.2f), c);
                canvas.hline(ox + 3, base - hgt / 2, cell - 6, PixelCanvas.shade(c, 0.6f));
                break;
            }

            default:
                return;
        }

        canvas.rim(ox, oy, cell, boxH, RIM_DARK);
    }

    /**
     * A prop standing on the floor: a front face `hgt` tall with a `depth`-deep
     * top face above it. Same extrusion as a wall slab, so furniture and
     * masonry catch the light from the same direction.
     */
    private void box(int x, int floorY, int wdt, int hgt, int depth, int top, int face) {
        int y0 = floorY - hgt;
        canvas.gradient(x, y0, wdt, hgt, face, 1.0f, 0.52f);
        canvas.speckle(x, y0 - depth, wdt, depth, top, 0.10f, 5);
        canvas.hline(x, y0 - 1, wdt, PixelCanvas.shade(top, 0.35f));
        canvas.hline(x, y0 - depth, wdt, PixelCanvas.shade(top, 1.25f));
        canvas.vline(x, y0 - depth, hgt + depth, PixelCanvas.shade(face, 0.45f));
        canvas.vline(x + wdt - 1, y0 - depth, hgt + depth, PixelCanvas.shade(face, 0.45f));
        canvas.hline(x, floorY - 1, wdt, PixelCanvas.shade(face, 0.32f));
    }

    /** Wooden leaf inset into a door slab: two recessed panels and a knob. */
    private void doorLeaf(int x, int y, int wdt, int hgt, boolean horizontal) {
        if (wdt <= 2 || hgt <= 2) {
            return;
        }
        canvas.gradient(x, y, wdt, hgt, WOOD, 1.06f, 0.72f);
        canvas.outline(x, y, wdt, hgt, WOOD_DARK);
        if (horizontal) {
            int pw = Math.max(2, (wdt - 5) / 2);
            canvas.outline(x + 2, y + 2, pw, Math.max(2, hgt - 4), WOOD_DARK);
            canvas.outline(x + wdt - 2 - pw, y + 2, pw, Math.max(2, hgt - 4), WOOD_DARK);
            canvas.set(x + wdt - 3, y + hgt / 2, PixelCanvas.rgb(246, 224, 148));
        } else {
            int ph = Math.max(2, (hgt - 5) / 2);
            canvas.outline(x + 2, y + 2, Math.max(2, wdt - 4), ph, WOOD_DARK);
            canvas.outline(x + 2, y + hgt - 2 - ph, Math.max(2, wdt - 4), ph, WOOD_DARK);
            canvas.set(x + wdt / 2, y + hgt - 3, PixelCanvas.rgb(246, 224, 148));
        }
    }

    /** Lit glass in a dark frame, split into panes by mullions. */
    private void pane(int x, int y, int wdt, int hgt, int cols, int rows) {
        if (wdt <= 2 || hgt <= 2) {
            return;
        }
        canvas.rect(x, y, wdt, hgt, PixelCanvas.rgb(58, 50, 40));       // frame
        canvas.gradient(x + 1, y + 1, wdt - 2, hgt - 2, PANE, 1.0f, 0.62f);
        int mullion = PixelCanvas.rgb(72, 60, 44);
        for (int c = 1; c < cols; c++) {
            canvas.vline(x + c * wdt / cols, y, hgt, mullion);
        }
        for (int r = 1; r < rows; r++) {
            canvas.hline(x, y + r * hgt / rows, wdt, mullion);
        }
        // a highlight along the top of the glass sells it as a light source
        canvas.hline(x + 1, y + 1, wdt - 2, PixelCanvas.shade(PANE, 1.12f));
    }

    // ----------------------------------------------------------------- people

    private static final int PERSON_ALL = 0;
    private static final int PERSON_COAT = 1;
    private static final int PERSON_HAIR = 2;

    /**
     * One figure, in three passes over the same geometry: the whole person in a
     * neutral palette, then the coat alone and the hair alone as white masks
     * the draw call tints. Keeping the layout in one method is what stops the
     * masks from drifting off the body.
     */
    private void person(int ox, int oy, int layer) {
        int base = oy + boxH;
        int mid = ox + cell / 2;

        int shoeH = Math.max(2, Math.round(cell * 0.07f));
        int legH = Math.max(4, Math.round(cell * 0.19f));
        int torsoH = Math.max(6, Math.round(cell * 0.30f));
        int headR = Math.max(3, Math.round(cell * 0.13f));
        int bodyW = Math.max(7, Math.round(cell * 0.34f));
        int armW = Math.max(2, Math.round(cell * 0.07f));

        int feet = base - 1;
        int legY = feet - shoeH - legH;
        int torsoY = legY - torsoH;
        int headCy = torsoY - headR + 1;

        boolean all = layer == PERSON_ALL;
        int coat = all ? COAT_NEUTRAL : PixelCanvas.rgb(255, 255, 255);
        int hair = all ? HAIR_NEUTRAL : PixelCanvas.rgb(255, 255, 255);

        if (all) {
            // shoes and legs
            canvas.rect(mid - bodyW / 2 + 1, feet - shoeH, Math.max(2, bodyW / 3 - 1),
                    shoeH, SHOE);
            canvas.rect(mid + 1, feet - shoeH, Math.max(2, bodyW / 3 - 1), shoeH, SHOE);
            canvas.rect(mid - bodyW / 2 + 1, legY, Math.max(2, bodyW / 3 - 1), legH,
                    TROUSERS);
            canvas.rect(mid + 1, legY, Math.max(2, bodyW / 3 - 1), legH,
                    PixelCanvas.shade(TROUSERS, 0.82f));
        }

        if (layer == PERSON_ALL || layer == PERSON_COAT) {
            // torso, one pixel wider at the shoulders
            canvas.gradient(mid - bodyW / 2, torsoY, bodyW, torsoH, coat, 1.08f, 0.74f);
            canvas.hline(mid - bodyW / 2, torsoY, bodyW, PixelCanvas.shade(coat, 1.2f));
            // arms hanging at the sides, a shade darker than the chest
            canvas.rect(mid - bodyW / 2 - armW, torsoY + 1, armW, torsoH - 1,
                    PixelCanvas.shade(coat, 0.7f));
            canvas.rect(mid + bodyW / 2, torsoY + 1, armW, torsoH - 1,
                    PixelCanvas.shade(coat, 0.7f));
            // collar
            canvas.hline(mid - 2, torsoY + 1, 4, PixelCanvas.shade(coat, 0.6f));
        }

        if (all) {
            // hands poking out of the sleeves
            canvas.rect(mid - bodyW / 2 - armW, torsoY + torsoH - 2, armW, 2, SKIN);
            canvas.rect(mid + bodyW / 2, torsoY + torsoH - 2, armW, 2, SKIN);
            // head
            canvas.ellipse(mid, headCy, headR, headR, SKIN);
            canvas.set(mid - headR / 2, headCy, PixelCanvas.rgb(48, 38, 34));
            canvas.set(mid + headR / 2 - 1, headCy, PixelCanvas.rgb(48, 38, 34));
        }

        if (layer == PERSON_ALL || layer == PERSON_HAIR) {
            // hair as a cap over the top half of the skull, plus a short fringe
            canvas.ellipse(mid, headCy - 1, headR, headR * 0.72f, hair);
            canvas.rect(mid - headR, headCy - 1, headR * 2, 1,
                    PixelCanvas.shade(hair, 0.8f));
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
     * with their own palette (furniture, doors, windows, lamps) ignore it.
     */
    public static boolean isTinted(int kind) {
        return kind == OBJ_BOX_LOW || kind == OBJ_BOX_TALL || kind == OBJ_ITEM
                || kind == OBJ_PERSON_COAT || kind == OBJ_PERSON_HAIR;
    }

    /** Draws an object sprite standing in the cell whose top-left is (x,y). */
    public void object(int kind, float x, float y, float r, float g, float b, float a) {
        float[] uv = objects[kind];
        glColor4f(r, g, b, a);
        Draw.sprite(x, y - RenderConfig.riseH(), cell, boxH, uv[0], uv[1], uv[2], uv[3]);
    }
}
