package com.nuclearunicorn.serialkiller.render.overlays;

import com.nuclearunicorn.libroguelike.game.world.WorldTimer;
import com.nuclearunicorn.libroguelike.render.WindowRender;
import org.newdawn.slick.Color;
import org.newdawn.slick.FontSpec;
import org.newdawn.slick.TrueTypeFont;

import java.util.Calendar;

import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_LINE_LOOP;
import static org.lwjgl.opengl.GL11.GL_ONE;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_TRIANGLE_FAN;
import static org.lwjgl.opengl.GL11.GL_TRIANGLE_STRIP;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glColor4f;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glLineWidth;
import static org.lwjgl.opengl.GL11.glVertex2f;

/**
 * The wall clock, top right: HH:MM on the world's own time, and one vector disc that is the
 * sun by day and a crescent by night.
 *
 * <p>It is one shape, not two sprites swapped at dawn. The disc is drawn as the classic
 * moon-phase <i>lune</i> — bounded by the circle on one side and by the terminator ellipse on
 * the other — so a single number moves it continuously from crescent through half to full,
 * while the rays grow in and the colour warms. Sunrise takes three hours of game time and
 * looks like it.
 *
 * <p>The phase is driven by a smooth ramp of its own rather than by
 * {@link WorldTimer#get_light_amt()}: that one is tuned for the light field and steps at 17:00,
 * which on an icon reads as a glitch.
 */
public final class ClockOverlay {

    private ClockOverlay() {}

    private static final int MARGIN = 12;
    private static final int PLATE_W = 134;
    private static final int PLATE_H = 50;
    private static final int PLATE_TOP = 12;

    private static final float R = 13f;             //icon radius
    private static final int RAYS = 12;
    private static final int ARC_STEPS = 40;        //per lune edge

    /** How long dawn and dusk take, in hours, for the icon. Half either side of the boundary. */
    private static final float TWILIGHT = 3.0f;

    private static final float[] SUN_CORE = {1.00f, 0.90f, 0.55f};
    private static final float[] SUN_RIM  = {1.00f, 0.58f, 0.18f};
    private static final float[] MOON_CORE = {0.96f, 0.97f, 1.00f};
    private static final float[] MOON_RIM  = {0.60f, 0.68f, 0.88f};

    private static TrueTypeFont font;

    private static TrueTypeFont font() {
        if (font == null) {
            font = new TrueTypeFont(new FontSpec("Monospaced", 18, true), true);
        }
        return font;
    }

    public static void render() {
        float hour = WorldTimer.hourOfDay();
        float day = dayness(hour);

        float plateX = WindowRender.get_window_w() - MARGIN - PLATE_W;
        float cx = plateX + 27;
        float cy = PLATE_TOP + PLATE_H / 2f;

        float[] core = mix(MOON_CORE, SUN_CORE, day);
        float[] rim = mix(MOON_RIM, SUN_RIM, day);

        plate(plateX, PLATE_TOP, PLATE_W, PLATE_H);

        glow(cx, cy, R * (1.45f + 0.35f * day), core, 0.10f + 0.20f * day);

        //rays hold off until the crescent has filled out, or the moon sprouts a corona
        float rayAmt = clamp((day - 0.30f) / 0.70f);
        if (rayAmt > 0) {
            //the corona turns once per day, so the icon is never quite the same twice
            rays(cx, cy, rayAmt, (float) (Math.PI * 2 * hour / 24.0), core);
        }

        //+1 is a crescent, 0 a half disc, -1 the full sun
        float phase = 0.62f - 1.62f * day;
        //the crescent tips over as the night wears on; the sun stays upright
        float tilt = (float) (Math.sin(Math.PI * 2 * (hour - 3) / 24.0) * 0.44 * (1 - day));
        lune(cx, cy, R, phase, tilt, core, rim);

        TrueTypeFont f = font();
        String text = clockText();
        f.drawString(plateX + PLATE_W - 14 - f.getWidth(text),
                PLATE_TOP + (PLATE_H - f.getHeight()) / 2f,
                text, new Color(core[0], core[1], core[2]));
    }

    private static String clockText() {
        return pad2(WorldTimer.datetime.get(Calendar.HOUR_OF_DAY))
                + ":" + pad2(WorldTimer.datetime.get(Calendar.MINUTE));
    }

    private static String pad2(int v) {
        return v < 10 ? "0" + v : Integer.toString(v);
    }

    /**
     * 0 at night, 1 in daylight, a smooth ramp across the two hours either side of the
     * world's own dawn and dusk. Everything the icon does hangs off this one number.
     */
    private static float dayness(float hour) {
        return smoothstep(WorldTimer.DAWN - TWILIGHT / 2, WorldTimer.DAWN + TWILIGHT / 2, hour)
             * (1 - smoothstep(WorldTimer.DUSK - TWILIGHT / 2, WorldTimer.DUSK + TWILIGHT / 2, hour));
    }

    /**
     * The lit part of a disc, as a triangle strip between the circle and the terminator.
     *
     * @param phase -1 is the whole disc, 0 a half disc, +1 nothing at all
     * @param tilt  radians, rotates the terminator around the disc centre
     */
    private static void lune(float cx, float cy, float r, float phase, float tilt,
                             float[] core, float[] rim) {
        float sin = (float) Math.sin(tilt);
        float cos = (float) Math.cos(tilt);

        glDisable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        glBegin(GL_TRIANGLE_STRIP);
        for (int i = 0; i <= ARC_STEPS; i++) {
            double th = Math.PI * i / ARC_STEPS;
            float ex = (float) Math.sin(th) * r;
            float ey = (float) Math.cos(th) * r;
            //a vertical gradient stands in for a highlight: no radial fill fits a crescent
            float[] c = mix(rim, core, (1 - (float) Math.cos(th)) / 2);
            glColor4f(c[0] * 0.86f, c[1] * 0.86f, c[2] * 0.90f, 1f);
            vertex(cx, cy, ex * phase, ey, sin, cos);
            glColor4f(c[0], c[1], c[2], 1f);
            vertex(cx, cy, ex, ey, sin, cos);
        }
        glEnd();

        //the rim is what keeps a thin crescent legible over a lit street
        glLineWidth(1);
        glBegin(GL_LINE_LOOP);
        glColor4f(Math.min(1, core[0] * 1.1f), Math.min(1, core[1] * 1.1f),
                Math.min(1, core[2] * 1.1f), 0.75f);
        for (int i = 0; i <= ARC_STEPS; i++) {
            double th = Math.PI * i / ARC_STEPS;
            vertex(cx, cy, (float) Math.sin(th) * r, (float) Math.cos(th) * r, sin, cos);
        }
        for (int i = ARC_STEPS; i >= 0; i--) {
            double th = Math.PI * i / ARC_STEPS;
            vertex(cx, cy, (float) Math.sin(th) * r * phase, (float) Math.cos(th) * r, sin, cos);
        }
        glEnd();

        glEnable(GL_TEXTURE_2D);
    }

    /** One rotated point of the icon, emitted about its centre. */
    private static void vertex(float cx, float cy, float x, float y, float sin, float cos) {
        glVertex2f(cx + x * cos - y * sin, cy + x * sin + y * cos);
    }

    /** Tapered spokes, every other one short, fading out at the tip. */
    private static void rays(float cx, float cy, float amt, float rot, float[] color) {
        float inner = R * 1.25f;
        float half = R * 0.12f;

        glDisable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE);      //additive: spokes glow rather than paint

        glBegin(GL_TRIANGLES);
        for (int i = 0; i < RAYS; i++) {
            double a = rot + Math.PI * 2 * i / RAYS;
            float outer = inner + R * (i % 2 == 0 ? 0.60f : 0.34f) * amt;
            float sin = (float) Math.sin(a);
            float cos = (float) Math.cos(a);

            glColor4f(color[0], color[1], color[2], 0.85f * amt);
            glVertex2f(cx + inner * cos - half * sin, cy + inner * sin + half * cos);
            glVertex2f(cx + inner * cos + half * sin, cy + inner * sin - half * cos);
            glColor4f(color[0], color[1], color[2], 0f);
            glVertex2f(cx + outer * cos, cy + outer * sin);
        }
        glEnd();

        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_TEXTURE_2D);
    }

    /** Soft halo: a fan bright at the centre and transparent at the rim. */
    private static void glow(float cx, float cy, float r, float[] color, float alpha) {
        glDisable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE);

        glBegin(GL_TRIANGLE_FAN);
        glColor4f(color[0], color[1], color[2], alpha);
        glVertex2f(cx, cy);
        glColor4f(color[0], color[1], color[2], 0f);
        for (int i = 0; i <= 24; i++) {
            double a = Math.PI * 2 * i / 24;
            glVertex2f(cx + (float) Math.cos(a) * r, cy + (float) Math.sin(a) * r);
        }
        glEnd();

        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_TEXTURE_2D);
    }

    /** The rounded slab the clock sits on. Filled as a fan, then traced. */
    private static void plate(float x, float y, float w, float h) {
        float[] outline = roundRect(x, y, w, h, 8f);

        glDisable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        glBegin(GL_TRIANGLE_FAN);
        glColor4f(0.05f, 0.05f, 0.08f, 0.62f);
        glVertex2f(x + w / 2, y + h / 2);
        for (int i = 0; i < outline.length; i += 2) {
            glVertex2f(outline[i], outline[i + 1]);
        }
        glVertex2f(outline[0], outline[1]);
        glEnd();

        glLineWidth(1);
        glBegin(GL_LINE_LOOP);
        glColor4f(0.35f, 0.37f, 0.45f, 0.45f);
        for (int i = 0; i < outline.length; i += 2) {
            glVertex2f(outline[i], outline[i + 1]);
        }
        glEnd();

        glEnable(GL_TEXTURE_2D);
    }

    /** Rounded-rectangle outline as flat x,y pairs, clockwise from the top left corner. */
    private static float[] roundRect(float x, float y, float w, float h, float r) {
        final int seg = 5;
        float[] pts = new float[4 * (seg + 1) * 2];
        float[] ox = {x + r, x + w - r, x + w - r, x + r};
        float[] oy = {y + r, y + r, y + h - r, y + h - r};
        int p = 0;
        for (int corner = 0; corner < 4; corner++) {
            double start = Math.PI + corner * Math.PI / 2;
            for (int i = 0; i <= seg; i++) {
                double a = start + Math.PI / 2 * i / seg;
                pts[p++] = ox[corner] + (float) Math.cos(a) * r;
                pts[p++] = oy[corner] + (float) Math.sin(a) * r;
            }
        }
        return pts;
    }

    private static float[] mix(float[] a, float[] b, float t) {
        return new float[] {
            a[0] + (b[0] - a[0]) * t,
            a[1] + (b[1] - a[1]) * t,
            a[2] + (b[2] - a[2]) * t
        };
    }

    private static float smoothstep(float edge0, float edge1, float v) {
        float t = clamp((v - edge0) / (edge1 - edge0));
        return t * t * (3 - 2 * t);
    }

    private static float clamp(float v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }
}
