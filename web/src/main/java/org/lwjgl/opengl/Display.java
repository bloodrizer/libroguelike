package org.lwjgl.opengl;

import org.lwjgl.input.InputBridge;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLCanvasElement;

/**
 * Web shim for the LWJGL 2 Display API.
 *
 * There is no window to create or swap: the canvas already exists in the page
 * and the browser presents it after each task. sync()/update() therefore only
 * need to close out the frame's GL batch — the actual frame pacing is done by
 * requestAnimationFrame in WebMain.
 */
public final class Display {

    private Display() {
    }

    private static HTMLCanvasElement canvas;
    private static boolean created;
    private static int width = 1024;
    private static int height = 768;

    /** Called by the entry point before the engine boots. */
    public static void bind(HTMLCanvasElement c) {
        canvas = c;
    }

    public static void setDisplayMode(DisplayMode mode) {
        width = mode.getWidth();
        height = mode.getHeight();
        if (canvas != null) {
            canvas.setWidth(width);
            canvas.setHeight(height);
        }
    }

    public static void create() {
        if (created) {
            return;
        }
        GLES.init(canvas);
        InputBridge.attach(canvas, Window.current().getDocument());
        created = true;
    }

    public static void setTitle(String title) {
        Window.current().getDocument().setTitle(title);
    }

    public static void setVSyncEnabled(boolean vsync) {
        // requestAnimationFrame is always vsynced.
    }

    public static void sync(int fps) {
        // Frame pacing belongs to requestAnimationFrame.
    }

    public static void update() {
        GL11.endFrame();
    }

    public static boolean isCloseRequested() {
        return false; // a tab close tears the whole runtime down anyway
    }

    public static void destroy() {
        created = false;
    }

    public static int getWidth() {
        return width;
    }

    public static int getHeight() {
        return height;
    }
}
