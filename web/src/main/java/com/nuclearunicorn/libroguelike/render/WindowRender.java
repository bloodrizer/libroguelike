package com.nuclearunicorn.libroguelike.render;

import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;

/**
 * Web build of the window/GL setup.
 *
 * Mirrors the desktop class minus the GLFW window creation and the debug-output
 * extensions, which have no browser equivalent. The 2D ortho setup is identical,
 * so every draw call downstream measures in the same window-unit coordinates.
 */
public class WindowRender {

    static int WINDOW_W = 1024;
    static int WINDOW_H = 768;

    public static void create() {
        Display.setDisplayMode(new DisplayMode(WINDOW_W, WINDOW_H));
        Display.create();
        Display.setTitle("The Nameless Engine");

        WindowRender.initGL(WINDOW_W, WINDOW_H);
    }

    public static int get_window_h() {
        return WINDOW_H;
    }

    public static int get_window_w() {
        return WINDOW_W;
    }

    public static void initGL(int w, int h) {
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();

        GL11.glViewport(0, 0, w, h);
        GL11.glOrtho(0.0f, w, h, 0.0f, -1.0f, 1.0f);

        GL11.glClearColor(0, 0, 0, 1);

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();
    }

    /** The 3D path is unused by serialkiller; kept so callers still link. */
    public static void set3DMode() {
        GL11.glViewport(0, 0, WINDOW_W, WINDOW_H);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();
    }

    public static void set2DMode() {
        GL11.glLoadIdentity();
        GL11.glViewport(0, 0, WINDOW_W, WINDOW_H);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glOrtho(0.0f, WINDOW_W, WINDOW_H, 0.0f, -1.0f, 1.0f);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();
    }

    public static void destroy() {
        Display.destroy();
    }
}
