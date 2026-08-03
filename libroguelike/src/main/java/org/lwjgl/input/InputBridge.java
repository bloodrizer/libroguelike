// GLFW callback plumbing shared by Keyboard and Mouse shims.
// Display.create() calls attach() once; Display.update() calls beginFrame()
// before glfwPollEvents() so Keyboard.next()/Mouse.next() can drain a
// per-frame event queue.
package org.lwjgl.input;

import org.lwjgl.glfw.GLFWCharCallback;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWScrollCallback;

import java.util.ArrayDeque;
import java.util.Deque;

import static org.lwjgl.glfw.GLFW.*;

public final class InputBridge {

    static final class KeyEvent {
        final int key;
        final boolean down;
        char chr;   // backfilled by the char callback, which GLFW fires right after the key callback
        KeyEvent(int key, boolean down, char chr) { this.key = key; this.down = down; this.chr = chr; }
    }

    static final class MouseEvent {
        final int button;        // -1 if move-only
        final boolean down;
        final int x, y;          // absolute, GLFW Y already inverted to LWJGL2 convention
        final int dx, dy;
        MouseEvent(int button, boolean down, int x, int y, int dx, int dy) {
            this.button = button; this.down = down;
            this.x = x; this.y = y;
            this.dx = dx; this.dy = dy;
        }
    }

    static final Deque<KeyEvent> keyQueue = new ArrayDeque<>();
    static final Deque<MouseEvent> mouseQueue = new ArrayDeque<>();

    static int mouseX, mouseY;          // last seen, LWJGL2-style: 0 at bottom
    static int prevMouseX, prevMouseY;
    static boolean grabbed = false;
    static int winW = 1, winH = 1;
    static long window = 0L;

    private static GLFWKeyCallback keyCb;
    private static GLFWCharCallback charCb;
    private static GLFWMouseButtonCallback btnCb;
    private static GLFWCursorPosCallback posCb;
    private static GLFWScrollCallback scrollCb;

    private InputBridge() {}

    public static void attach(long handle) {
        window = handle;
        int[] w = new int[1], h = new int[1];
        glfwGetWindowSize(window, w, h);
        winW = w[0]; winH = h[0];

        keyCb = GLFWKeyCallback.create((win, key, scancode, action, mods) -> {
            if (action == GLFW_REPEAT) return;
            keyQueue.add(new KeyEvent(key, action == GLFW_PRESS, (char) 0));
        });
        glfwSetKeyCallback(window, keyCb);

        // GLFW fires the char callback right AFTER the key callback for a printable press.
        // Backfill the char onto the key event we just queued so the pairing is exact.
        charCb = GLFWCharCallback.create((win, codepoint) -> {
            if (codepoint < 0x10000 && !keyQueue.isEmpty()) {
                keyQueue.peekLast().chr = (char) codepoint;
            }
        });
        glfwSetCharCallback(window, charCb);

        btnCb = GLFWMouseButtonCallback.create((win, button, action, mods) -> {
            if (action == GLFW_REPEAT) return;
            mouseQueue.add(new MouseEvent(button, action == GLFW_PRESS,
                                          mouseX, mouseY, 0, 0));
        });
        glfwSetMouseButtonCallback(window, btnCb);

        posCb = GLFWCursorPosCallback.create((win, xpos, ypos) -> {
            // LWJGL 2: origin at bottom-left, Y grows upward.
            int newX = (int) xpos;
            int newY = winH - (int) ypos;
            int dx = newX - prevMouseX;
            int dy = newY - prevMouseY;
            prevMouseX = mouseX; prevMouseY = mouseY;
            mouseX = newX; mouseY = newY;
            mouseQueue.add(new MouseEvent(-1, false, mouseX, mouseY, dx, dy));
        });
        glfwSetCursorPosCallback(window, posCb);

        scrollCb = GLFWScrollCallback.create((win, dx, dy) -> { /* unused */ });
        glfwSetScrollCallback(window, scrollCb);
    }

    public static void detach() {
        if (keyCb != null)    { keyCb.free();    keyCb = null; }
        if (charCb != null)   { charCb.free();   charCb = null; }
        if (btnCb != null)    { btnCb.free();    btnCb = null; }
        if (posCb != null)    { posCb.free();    posCb = null; }
        if (scrollCb != null) { scrollCb.free(); scrollCb = null; }
    }

    /** Called once per frame *before* glfwPollEvents to clear last frame's queues. */
    public static void beginFrame() {
        keyQueue.clear();
        mouseQueue.clear();
    }
}
