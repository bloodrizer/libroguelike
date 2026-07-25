package com.nuclearunicorn.libroguelike.render;

import com.nuclearunicorn.libroguelike.core.Game;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBImageWrite;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.*;

/**
 * Dumps the GL back buffer to a PNG. Driven by system properties so a build can
 * be screenshotted from a script without touching the game:
 *
 *   -Dlrl.capture.file=/tmp/shot.png   where to write (enables capture)
 *   -Dlrl.capture.frame=90             frame number to grab (default 90)
 *   -Dlrl.capture.exit=true            stop the game after grabbing
 */
public final class ScreenCapture {

    private static final String FILE = System.getProperty("lrl.capture.file");
    private static final int AT_FRAME = Integer.getInteger("lrl.capture.frame", 90);
    private static final boolean EXIT =
            Boolean.parseBoolean(System.getProperty("lrl.capture.exit", "true"));

    private static int frame = 0;

    private ScreenCapture() {
    }

    public static boolean isArmed() {
        return FILE != null;
    }

    /** Called once per frame after the scene is drawn, before the buffer swap. */
    public static void tick() {
        if (FILE == null) {
            return;
        }
        if (++frame != AT_FRAME) {
            return;
        }
        save(FILE);
        if (EXIT) {
            Game.stop();
        }
    }

    public static void save(String path) {
        int[] viewport = new int[4];
        glGetIntegerv(GL_VIEWPORT, viewport);
        int w = viewport[2];
        int h = viewport[3];

        ByteBuffer pixels = BufferUtils.createByteBuffer(w * h * 4);
        glReadBuffer(GL_BACK);
        glPixelStorei(GL_PACK_ALIGNMENT, 1);
        glReadPixels(0, 0, w, h, GL_RGBA, GL_UNSIGNED_BYTE, pixels);

        STBImageWrite.stbi_flip_vertically_on_write(true);
        boolean ok = STBImageWrite.stbi_write_png(path, w, h, 4, pixels, w * 4);
        System.out.println((ok ? "captured " : "FAILED to capture ") + w + "x" + h + " -> " + path);
    }
}
