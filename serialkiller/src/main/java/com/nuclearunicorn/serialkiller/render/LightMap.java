package com.nuclearunicorn.serialkiller.render;

import com.nuclearunicorn.libroguelike.core.client.ClientGameEnvironment;
import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.game.player.Player;
import com.nuclearunicorn.libroguelike.game.world.WorldTile;
import com.nuclearunicorn.libroguelike.game.world.WorldTimer;
import com.nuclearunicorn.libroguelike.game.world.WorldView;
import com.nuclearunicorn.libroguelike.game.world.layers.WorldLayer;
import com.nuclearunicorn.serialkiller.game.world.RLTile;
import com.nuclearunicorn.serialkiller.game.world.entities.EntityRLActor;
import com.nuclearunicorn.serialkiller.game.world.entities.EntityRLHuman;
import rlforj.los.ILosBoard;
import rlforj.los.PrecisePermissive;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The light field, and the pass that applies it.
 *
 * Light is accumulated per cell from every emitter in range (each with its own
 * shadow-cast visibility mask, so lamps spill through doorways and windows the
 * way they should), then resampled onto the cell *corners* by averaging the
 * four cells that touch each corner. Drawing one Gouraud quad per cell makes
 * the GL interpolate that per pixel, which is what turns the old per-tile
 * banding into a smooth gradient — the shadow edges soften for free.
 *
 * The pass runs last and multiplies into the frame buffer, so it lights terrain,
 * sprites and glyphs uniformly instead of every renderer having to tint itself.
 */
public class LightMap {

    /** Bumped when something that blocks light changes (a door, a wall). */
    private static int epoch = 0;

    public static void invalidate() {
        epoch++;
    }

    private static final float[] MEMORY = {0.24f, 0.25f, 0.34f};   // explored, out of sight

    private int x0, y0, w, h;
    private float[] r = new float[0];
    private float[] g = new float[0];
    private float[] b = new float[0];
    private float[] cr = new float[0];
    private float[] cg = new float[0];
    private float[] cb = new float[0];
    // second, additive term: a haze that lifts the blacks instead of scaling them
    private float[] hr = new float[0];
    private float[] hg = new float[0];
    private float[] hb = new float[0];

    private final Map<Long, Mask> masks = new HashMap<Long, Mask>();
    private final PrecisePermissive fov = new PrecisePermissive();

    // -------------------------------------------------------------- emitters

    private static final class Emitter {
        int x, y, radius;
        float r, g, b, power;
    }

    private final java.util.ArrayList<Emitter> emitters = new java.util.ArrayList<Emitter>();

    /** Shadow-cast visibility for one emitter, cached until the world changes. */
    private static final class Mask implements ILosBoard {
        int epoch, cx, cy, radius, size;
        boolean[] lit;
        WorldLayer layer;

        boolean lit(int x, int y) {
            int lx = x - cx + radius;
            int ly = y - cy + radius;
            if (lx < 0 || ly < 0 || lx >= size || ly >= size) {
                return false;
            }
            return lit[ly * size + lx];
        }

        public boolean contains(int x, int y) {
            return Math.abs(x - cx) <= radius && Math.abs(y - cy) <= radius;
        }

        public boolean isObstacle(int x, int y) {
            WorldTile tile = layer.get_tile(x, y);
            if (tile == null || ((RLTile) tile).isWall()) {
                return true;
            }
            if (tile.isBlocked()) {
                Entity ent = tile.getEntity();
                return !(ent instanceof EntityRLActor) || ((EntityRLActor) ent).isBlockSight();
            }
            return false;
        }

        public void visit(int x, int y) {
            int lx = x - cx + radius;
            int ly = y - cy + radius;
            if (lx >= 0 && ly >= 0 && lx < size && ly < size) {
                lit[ly * size + lx] = true;
            }
        }
    }

    // ---------------------------------------------------------------- update

    public void update(TileWindow view, int fromX, int fromY, int toX, int toY) {
        x0 = fromX;
        y0 = fromY;
        w = toX - fromX + 1;
        h = toY - fromY + 1;

        int cells = w * h;
        if (r.length < cells) {
            r = new float[cells];
            g = new float[cells];
            b = new float[cells];
        }
        int corners = (w + 1) * (h + 1);
        if (cr.length < corners) {
            cr = new float[corners];
            cg = new float[corners];
            cb = new float[corners];
            hr = new float[corners];
            hg = new float[corners];
            hb = new float[corners];
        }

        collectEmitters();

        float day = WorldTimer.get_light_amt();
        float ambR = Palette.lerp(0.13f, 0.86f, day);
        float ambG = Palette.lerp(0.14f, 0.86f, day);
        float ambB = Palette.lerp(0.24f, 0.90f, day);

        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                int wx = x0 + i;
                int wy = y0 + j;
                int at = j * w + i;

                RLTile tile = view.at(wx, wy);
                if (tile == null || (!tile.isExplored() && !tile.isVisible())) {
                    float unseen = RenderConfig.REVEAL ? 0.8f : 0.0f;
                    r[at] = MEMORY[0] * unseen;
                    g[at] = MEMORY[1] * unseen;
                    b[at] = MEMORY[2] * unseen;
                    continue;
                }
                if (!tile.isVisible()) {
                    r[at] = MEMORY[0];
                    g[at] = MEMORY[1];
                    b[at] = MEMORY[2];
                    continue;
                }

                float lr = ambR;
                float lg = ambG;
                float lb = ambB;

                for (int k = 0; k < emitters.size(); k++) {
                    Emitter em = emitters.get(k);
                    int dx = wx - em.x;
                    int dy = wy - em.y;
                    int d2 = dx * dx + dy * dy;
                    int rr = em.radius * em.radius;
                    if (d2 > rr) {
                        continue;
                    }
                    Mask mask = maskFor(em);
                    if (mask != null && !mask.lit(wx, wy)) {
                        continue;
                    }
                    // smooth quadratic falloff: bright core, soft rim
                    float att = 1.0f - (float) d2 / rr;
                    att = att * att * em.power;
                    lr += em.r * att;
                    lg += em.g * att;
                    lb += em.b * att;
                }

                r[at] = lr;
                g[at] = lg;
                b[at] = lb;
            }
        }

        spreadOntoBlockers(view);
        applyMood();
        buildCorners();
    }

    /**
     * A wall's own cell is unlit by definition — light stops at its face. Give
     * every blocker the brightest light of its open neighbours so the slab that
     * faces a lamp is lit and the one behind it stays dark.
     *
     * The same fixup pulls light into the unknown cell *above* a known wall:
     * that cell is where the wall's top face is drawn, so without it every
     * north-facing wall would have a black cap.
     */
    private void spreadOntoBlockers(TileWindow view) {
        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                int wx = x0 + i;
                int wy = y0 + j;
                RLTile tile = view.at(wx, wy);
                boolean unknown = tile == null || (!tile.isExplored() && !tile.isVisible());
                boolean blocker = tile != null && tile.isWall();

                if (!blocker && !unknown) {
                    continue;
                }
                if (unknown && !view.isKnownWall(wx, wy + 1)) {
                    continue;
                }

                int at = j * w + i;
                float br = r[at], bg = g[at], bb = b[at];
                float best = br + bg + bb;

                for (int k = 0; k < 4; k++) {
                    int nx = i + (k == 0 ? -1 : k == 1 ? 1 : 0);
                    int ny = j + (k == 2 ? -1 : k == 3 ? 1 : 0);
                    if (nx < 0 || ny < 0 || nx >= w || ny >= h) {
                        continue;
                    }
                    if (unknown && ny != j + 1) {
                        continue;   // the cap only inherits from the wall below
                    }
                    int nat = ny * w + nx;
                    float sum = r[nat] + g[nat] + b[nat];
                    if (sum > best) {
                        best = sum;
                        br = r[nat];
                        bg = g[nat];
                        bb = b[nat];
                    }
                }
                r[at] = br;
                g[at] = bg;
                b[at] = bb;
            }
        }
    }

    /** Keeps the old bloodlust tint: the world reddens as the player loses it. */
    private void applyMood() {
        float bloodlust = 0.0f;
        if (Player.get_ent() instanceof EntityRLHuman) {
            bloodlust = ((EntityRLHuman) Player.get_ent()).getBodysim().getBloodlust() / 400f;
        }
        if (bloodlust <= 0.001f) {
            return;
        }
        for (int at = 0; at < w * h; at++) {
            r[at] = Palette.lerp(r[at], r[at] * 1.6f + 0.15f, bloodlust);
            g[at] = Palette.lerp(g[at], g[at] * 0.45f, bloodlust);
            b[at] = Palette.lerp(b[at], b[at] * 0.45f, bloodlust);
        }
    }

    /** Resample the cell field onto cell corners: this is what smooths it. */
    private void buildCorners() {
        int cw = w + 1;
        for (int cj = 0; cj <= h; cj++) {
            for (int ci = 0; ci <= w; ci++) {
                float sr = 0, sg = 0, sb = 0;
                int n = 0;
                for (int k = 0; k < 4; k++) {
                    int i = ci - 1 + (k & 1);
                    int j = cj - 1 + (k >> 1);
                    if (i < 0 || j < 0 || i >= w || j >= h) {
                        continue;
                    }
                    int at = j * w + i;
                    sr += r[at];
                    sg += g[at];
                    sb += b[at];
                    n++;
                }
                int at = cj * cw + ci;
                if (n == 0) {
                    cr[at] = 0;
                    cg[at] = 0;
                    cb[at] = 0;
                } else {
                    cr[at] = sr / n;
                    cg[at] = sg / n;
                    cb[at] = sb / n;
                }
                // A pure multiply can only ever darken what is already there, so
                // a remembered room turns muddy instead of moonlit. This second
                // term adds a little of the light's own colour back on top.
                float lift = cr[at] + cg[at] + cb[at] > 0.02f ? 1.0f : 0.0f;
                hr[at] = cr[at] * 0.13f + 0.020f * lift;
                hg[at] = cg[at] * 0.13f + 0.022f * lift;
                hb[at] = cb[at] * 0.13f + 0.042f * lift;
            }
        }
    }

    // -------------------------------------------------------------- emitters

    private void collectEmitters() {
        emitters.clear();

        float day = WorldTimer.get_light_amt();
        float lampPower = 1.0f - Math.min(1.0f, day * 1.6f);   // street lamps switch off by day

        Entity playerEnt = Player.get_ent();
        if (playerEnt != null) {
            Emitter torch = new Emitter();
            torch.x = playerEnt.x();
            torch.y = playerEnt.y();
            torch.radius = 8;
            torch.r = 0.72f;
            torch.g = 0.62f;
            torch.b = 0.44f;
            torch.power = 1.0f;
            emitters.add(torch);
        }

        List<Entity> all = ClientGameEnvironment.getEntityManager()
                .getList(WorldView.get_zindex());
        Entity[] snapshot = all.toArray(new Entity[0]);
        for (Entity ent : snapshot) {
            if (ent == null || ent.origin == null || ent.getName() == null) {
                continue;
            }
            int ex = ent.x();
            int ey = ent.y();
            if (ex < x0 - 12 || ex > x0 + w + 12 || ey < y0 - 12 || ey > y0 + h + 12) {
                continue;
            }

            String name = ent.getName();
            Emitter em = null;
            if (name.startsWith("lamppost")) {
                em = new Emitter();
                em.radius = 9;
                em.r = 0.95f;
                em.g = 0.78f;
                em.b = 0.38f;
                em.power = 1.15f * lampPower;
            } else if (name.startsWith("window")) {
                em = new Emitter();
                em.radius = 6;
                em.r = 0.70f;
                em.g = 0.58f;
                em.b = 0.30f;
                em.power = 0.75f * lampPower;
            }
            if (em == null || em.power <= 0.01f) {
                continue;
            }
            em.x = ex;
            em.y = ey;
            emitters.add(em);
        }
    }

    private Mask maskFor(Emitter em) {
        WorldLayer layer = ClientGameEnvironment.getWorldLayer(WorldView.get_zindex());
        if (layer == null) {
            return null;
        }
        long key = ((long) em.x << 32) ^ (em.y & 0xFFFFFFFFL) ^ ((long) em.radius << 20);
        Mask mask = masks.get(key);
        if (mask != null && mask.epoch == epoch) {
            return mask;
        }
        if (masks.size() > 512) {
            masks.clear();   // the player walked far enough that the cache is stale anyway
        }

        mask = new Mask();
        mask.epoch = epoch;
        mask.cx = em.x;
        mask.cy = em.y;
        mask.radius = em.radius;
        mask.size = em.radius * 2 + 1;
        mask.lit = new boolean[mask.size * mask.size];
        mask.layer = layer;
        fov.visitFieldOfView(mask, em.x, em.y, em.radius);
        mask.lit[mask.radius * mask.size + mask.radius] = true;   // the emitter's own cell
        masks.put(key, mask);
        return mask;
    }

    // ----------------------------------------------------------------- apply

    public void render(int fromX, int fromY, int toX, int toY) {
        Draw.beginMultiply();
        mesh(cr, cg, cb);
        Draw.endBlend();

        Draw.beginAdditive();
        mesh(hr, hg, hb);
        Draw.endBlend();
    }

    /** One Gouraud-shaded quad per cell, corner colours taken from the field. */
    private void mesh(float[] mr, float[] mg, float[] mb) {
        int cell = RenderConfig.CELL;
        int cw = w + 1;

        for (int j = 0; j < h; j++) {
            int y = Grid.cellY(y0 + j);
            for (int i = 0; i < w; i++) {
                int x = Grid.cellX(x0 + i);
                int tl = j * cw + i;
                int tr = tl + 1;
                int bl = tl + cw;
                int br = bl + 1;

                org.lwjgl.opengl.GL11.glColor4f(mr[tl], mg[tl], mb[tl], 1.0f);
                org.lwjgl.opengl.GL11.glVertex2f(x, y);
                org.lwjgl.opengl.GL11.glColor4f(mr[tr], mg[tr], mb[tr], 1.0f);
                org.lwjgl.opengl.GL11.glVertex2f(x + cell, y);
                org.lwjgl.opengl.GL11.glColor4f(mr[br], mg[br], mb[br], 1.0f);
                org.lwjgl.opengl.GL11.glVertex2f(x + cell, y + cell);
                org.lwjgl.opengl.GL11.glColor4f(mr[bl], mg[bl], mb[bl], 1.0f);
                org.lwjgl.opengl.GL11.glVertex2f(x, y + cell);
            }
        }
    }
}
