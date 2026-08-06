// Shim: LWJGL 3 dropped the Display class. Wraps GLFW for the legacy API the
// game uses: setDisplayMode/setTitle/setVSyncEnabled/create, then a loop of
// update()+sync()+isCloseRequested(), and finally destroy().
package org.lwjgl.opengl;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWCursorPosCallback;

import static org.lwjgl.glfw.GLFW.*;

import java.awt.Canvas;

public final class Display {

    private static long window = 0L;
    private static int width = 800, height = 600;
    private static String title = "LWJGL";
    private static boolean vsync = true;
    private static double lastFrameTime = 0.0;

    /** Set in InputBridge by the input module's first poll. */
    public static long handle() { return window; }
    public static int width()   { return width; }
    public static int height()  { return height; }

    public static void setDisplayMode(DisplayMode mode) {
        width = mode.getWidth();
        height = mode.getHeight();
    }

    public static void setTitle(String t) {
        title = t;
        if (window != 0L) glfwSetWindowTitle(window, t);
    }

    public static void setVSyncEnabled(boolean enabled) {
        vsync = enabled;
        if (window != 0L) glfwSwapInterval(enabled ? 1 : 0);
    }

    /** No-op: applet/canvas embedding is gone from LWJGL 3. */
    public static void setParent(Canvas parent) { /* unsupported */ }

    public static void create() throws org.lwjgl.LWJGLException {
        GLFWErrorCallback.createPrint(System.err).set();

        if (!glfwInit()) {
            throw new org.lwjgl.LWJGLException("Failed to initialise GLFW");
        }

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
        // Compat profile so glBegin/glEnd keep working until the VBO migration.
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 2);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 1);
        // Match window pixels 1:1 with framebuffer pixels — game's hardcoded
        // viewport assumes WINDOW_W × WINDOW_H and would only fill a quadrant
        // on Retina otherwise.
        glfwWindowHint(GLFW_COCOA_RETINA_FRAMEBUFFER, GLFW_FALSE);

        window = glfwCreateWindow(width, height, title, 0L, 0L);
        if (window == 0L) {
            throw new org.lwjgl.LWJGLException("Failed to create GLFW window");
        }

        glfwMakeContextCurrent(window);
        GL.createCapabilities();
        glfwSwapInterval(vsync ? 1 : 0);

        // Center the window on the primary monitor before showing it so it
        // doesn't end up off-screen on macOS (where the default position is
        // sometimes under the menu bar / off the visible area).
        long monitor = glfwGetPrimaryMonitor();
        if (monitor != 0L) {
            org.lwjgl.glfw.GLFWVidMode vid = glfwGetVideoMode(monitor);
            if (vid != null) {
                glfwSetWindowPos(window,
                    (vid.width() - width) / 2,
                    (vid.height() - height) / 2);
            }
        }
        // Offscreen mode for scripted screenshots: render into the back buffer
        // of a window that is never mapped (see render/ScreenCapture).
        if (!Boolean.getBoolean("lrl.window.hidden")) {
            glfwShowWindow(window);
            glfwFocusWindow(window);
        }

        org.lwjgl.input.InputBridge.attach(window);
        lastFrameTime = glfwGetTime();
    }

    public static boolean isCloseRequested() {
        return window != 0L && glfwWindowShouldClose(window);
    }

    public static void update() {
        if (window == 0L) return;
        glfwSwapBuffers(window);
        org.lwjgl.input.InputBridge.beginFrame();
        glfwPollEvents();
    }

    /** Best-effort fixed-rate cap. GLFW vsync handles most of it. */
    public static void sync(int targetFps) {
        if (targetFps <= 0) return;
        double frameTime = 1.0 / targetFps;
        double elapsed = glfwGetTime() - lastFrameTime;
        long sleepNanos = (long) ((frameTime - elapsed) * 1_000_000_000L);
        if (sleepNanos > 0) {
            try { Thread.sleep(sleepNanos / 1_000_000L, (int) (sleepNanos % 1_000_000L)); }
            catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
        }
        lastFrameTime = glfwGetTime();
    }

    public static void destroy() {
        if (window != 0L) {
            org.lwjgl.input.InputBridge.detach();
            glfwDestroyWindow(window);
            window = 0L;
        }
        glfwTerminate();
        GLFWErrorCallback cb = glfwSetErrorCallback(null);
        if (cb != null) cb.free();
    }
}
