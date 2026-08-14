package com.nuclearunicorn.serialkiller.render.overlays;

import com.nuclearunicorn.libroguelike.core.Input;
import com.nuclearunicorn.libroguelike.game.player.Player;
import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.serialkiller.game.sound.Acoustics;
import com.nuclearunicorn.serialkiller.game.sound.SoundConfig;
import com.nuclearunicorn.serialkiller.game.sound.SoundField;
import com.nuclearunicorn.serialkiller.game.sound.SoundKind;
import com.nuclearunicorn.serialkiller.render.Draw;
import com.nuclearunicorn.serialkiller.render.Grid;
import com.nuclearunicorn.serialkiller.render.RenderConfig;

import static org.lwjgl.opengl.GL11.GL_LINES;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glColor4f;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glLineWidth;
import static org.lwjgl.opengl.GL11.glVertex2f;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;

/**
 * Hold ALT to see what the town can hear.
 *
 * <p>The cost tables in SOUND_DESIGN.md 4 are unfalsifiable without this. Numbers like "an
 * exterior wall costs 46dB" mean nothing on paper; watching a gunshot pour through a doorway,
 * bleed faintly through a window and stop dead against brick is what makes them tunable.
 *
 * <p>Two fields, in priority order: the last sound anything actually emitted (so a murder
 * leaves a visible footprint for as long as you hold the key), falling back to the player's
 * own footsteps — which is Splinter Cell's noise meter made spatial. You see exactly which
 * rooms you are audible in before you decide where to stand.
 *
 * <p>Drawn from inside the scene pass rather than the screen-space overlay, because the
 * camera matrix is still applied there and {@link Grid} maps tiles to it directly.
 */
public final class DebugSoundField {

    private DebugSoundField() {}

    /**
     * {@code -Dlrl.sound=GUNSHOT} pins the overlay on and floods that kind from the player
     * every frame, so an offscreen capture can show the field with no key held down.
     * {@code -Dlrl.sound=true} just forces the overlay on and shows whatever is current.
     */
    private static final String FORCED = System.getProperty("lrl.sound");

    /** Colour ramp for received level: hot where it is loud, cold where it is nearly gone. */
    private static final float[][] RAMP = {
        {0.15f, 0.15f, 0.55f},   // barely audible
        {0.10f, 0.55f, 0.55f},
        {0.20f, 0.75f, 0.25f},
        {0.85f, 0.80f, 0.15f},
        {0.95f, 0.45f, 0.10f},
        {1.00f, 0.15f, 0.10f},   // deafening
    };

    public static void render() {
        if (!Input.key_state_alt && FORCED == null) {
            return;
        }
        SoundField field = forcedField();
        if (field == null) {
            field = Acoustics.lastField();
        }
        if (field == null) {
            field = playerNoise();
        }
        if (field == null) {
            return;
        }

        int cell = RenderConfig.CELL;
        int x1 = field.x0 + field.size - 1;
        int y1 = field.y0 + field.size - 1;

        Draw.beginFlat();
        for (int y = field.y0; y <= y1; y++) {
            for (int x = field.x0; x <= x1; x++) {
                if (!Grid.onScreen(x, y)) {
                    continue;
                }
                int received = field.received(x, y);
                if (received == Integer.MIN_VALUE || received < SoundConfig.FLOOR) {
                    continue;
                }
                float[] c = ramp(received, field.loudness);
                // alpha tracks loudness too, so the quiet fringe does not paint over the map
                float a = 0.25f + 0.45f * level(received, field.loudness);
                Draw.quad(Grid.cellX(x), Grid.cellY(y), cell, cell, c[0], c[1], c[2], a);
            }
        }
        Draw.endFlat();

        drawArrows(field, x1, y1, cell);
        markSource(field, cell);
    }

    /**
     * One short line per cell pointing back the way the sound came.
     *
     * <p>This is the half of the system that is invisible in a heat map and matters most:
     * inside a building the arrows converge on the doorway, not on the wall the noise
     * happened behind, and that is exactly the route an investigating NPC will walk.
     */
    private static void drawArrows(SoundField field, int x1, int y1, int cell) {
        // Too dense to read at every cell on a big field; thin them out as it grows.
        int step = field.size > 40 ? 3 : (field.size > 20 ? 2 : 1);
        int half = cell / 2;
        int len = Math.max(3, cell / 3);

        glDisable(GL_TEXTURE_2D);
        glLineWidth(2);
        glBegin(GL_LINES);
        glColor4f(1.0f, 1.0f, 1.0f, 0.75f);
        for (int y = field.y0; y <= y1; y += step) {
            for (int x = field.x0; x <= x1; x += step) {
                if (!Grid.onScreen(x, y)) {
                    continue;
                }
                if (field.received(x, y) == Integer.MIN_VALUE) {
                    continue;
                }
                int d = field.directionAt(x, y);
                if (d < 0) {
                    continue;
                }
                float cx = Grid.cellX(x) + half;
                float cy = Grid.cellY(y) + half;
                glVertex2f(cx, cy);
                glVertex2f(cx + Acoustics.DX[d] * len, cy + Acoustics.DY[d] * len);
            }
        }
        glEnd();
        glEnable(GL_TEXTURE_2D);
    }

    private static void markSource(SoundField field, int cell) {
        if (!Grid.onScreen(field.originX, field.originY)) {
            return;
        }
        Draw.beginFlat();
        Draw.quad(Grid.cellX(field.originX) + cell / 4, Grid.cellY(field.originY) + cell / 4,
                cell / 2, cell / 2, 1.0f, 1.0f, 1.0f, 0.9f);
        Draw.endFlat();
    }

    /** Where a level sits between the audibility floor and the source level, in 0..1. */
    private static float level(int received, int loudness) {
        int span = loudness - SoundConfig.FLOOR;
        if (span <= 0) {
            return 1.0f;
        }
        float t = (float) (received - SoundConfig.FLOOR) / span;
        return t < 0 ? 0 : (t > 1 ? 1 : t);
    }

    private static float[] ramp(int received, int loudness) {
        float t = level(received, loudness) * (RAMP.length - 1);
        int i = (int) t;
        if (i >= RAMP.length - 1) {
            return RAMP[RAMP.length - 1];
        }
        float f = t - i;
        return new float[] {
            RAMP[i][0] + (RAMP[i + 1][0] - RAMP[i][0]) * f,
            RAMP[i][1] + (RAMP[i + 1][1] - RAMP[i][1]) * f,
            RAMP[i][2] + (RAMP[i + 1][2] - RAMP[i][2]) * f,
        };
    }

    /** The sound named by {@code -Dlrl.sound}, flooded from the player, or null. */
    private static SoundField forcedField() {
        if (FORCED == null || "true".equalsIgnoreCase(FORCED)) {
            return null;
        }
        Entity player = Player.get_ent();
        if (player == null || player.origin == null) {
            return null;
        }
        try {
            SoundKind kind = SoundKind.valueOf(FORCED.toUpperCase());
            return Acoustics.propagate(player.origin, kind.db(), player.getLayerId());
        } catch (IllegalArgumentException bad) {
            return null;    // not a kind name; treat as a plain "on"
        }
    }

    /** The player's own footsteps: what the town would hear if you took a step right now. */
    private static SoundField playerNoise() {
        Entity player = Player.get_ent();
        if (player == null || player.origin == null) {
            return null;
        }
        return Acoustics.propagate(player.origin, SoundKind.FOOTSTEP_WALK.db(),
                player.getLayerId());
    }
}
