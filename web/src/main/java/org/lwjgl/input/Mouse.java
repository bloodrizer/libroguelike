package org.lwjgl.input;

/** Web shim: LWJGL 2-style polling Mouse backed by DOM mouse events. */
public final class Mouse {

    private Mouse() {
    }

    private static InputBridge.MouseEventRec current;

    public static boolean next() {
        current = InputBridge.mouseQueue.poll();
        return current != null;
    }

    public static int getEventButton() {
        return current == null ? -1 : current.button;
    }

    public static int getEventDX() {
        return current == null ? 0 : current.dx;
    }

    public static int getEventDY() {
        return current == null ? 0 : current.dy;
    }

    public static int getX() {
        return InputBridge.mouseX;
    }

    public static int getY() {
        return InputBridge.mouseY;
    }

    public static boolean isButtonDown(int button) {
        return InputBridge.isDown(button);
    }

    /** Pointer lock would be the browser equivalent; the game never grabs today. */
    public static void setGrabbed(boolean grab) {
        InputBridge.grabbed = grab;
    }

    public static void setNativeCursor(Cursor cursor) {
        // Custom bitmap cursors are not reimplemented; the CSS cursor is used.
    }
}
