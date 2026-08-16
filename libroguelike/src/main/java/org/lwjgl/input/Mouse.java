// Shim: LWJGL 2-style polling Mouse backed by GLFW callbacks.
package org.lwjgl.input;

import static org.lwjgl.glfw.GLFW.*;

public final class Mouse {
    private static InputBridge.MouseEvent current;

    public static boolean next() {
        current = InputBridge.mouseQueue.poll();
        return current != null;
    }

    public static int getEventButton() { return current == null ? -1 : current.button; }
    public static int getEventDX()     { return current == null ? 0 : current.dx; }
    public static int getEventDY()     { return current == null ? 0 : current.dy; }

    public static int getX() { return InputBridge.mouseX; }
    public static int getY() { return InputBridge.mouseY; }

    public static boolean isButtonDown(int button) {
        if (InputBridge.window == 0L) return false;
        return glfwGetMouseButton(InputBridge.window, button) == GLFW_PRESS;
    }

    public static void setGrabbed(boolean grab) {
        InputBridge.grabbed = grab;
        if (InputBridge.window != 0L) {
            glfwSetInputMode(InputBridge.window, GLFW_CURSOR,
                grab ? GLFW_CURSOR_DISABLED : GLFW_CURSOR_NORMAL);
        }
    }

    /** No-op: custom-bitmap cursors via legacy lwjgl.input.Cursor are not
     *  reimplemented here — Render.set_cursor() is best-effort. */
    public static void setNativeCursor(Cursor cursor) {
        /* TODO M2b: glfwCreateCursor + glfwSetCursor */
    }
}
