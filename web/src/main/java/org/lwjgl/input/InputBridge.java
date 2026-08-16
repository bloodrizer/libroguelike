package org.lwjgl.input;

import org.teavm.jso.dom.events.KeyboardEvent;
import org.teavm.jso.dom.events.MouseEvent;
import org.teavm.jso.dom.events.WheelEvent;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.teavm.jso.dom.html.HTMLDocument;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

/**
 * Browser side of the LWJGL 2-style polling input API.
 *
 * DOM events are queued as they arrive and drained once per frame by
 * {@link Keyboard#next()} / {@link Mouse#next()}, which is the same shape the
 * desktop GLFW bridge provides.
 */
public final class InputBridge {

    private InputBridge() {
    }

    public static final class KeyEvent {
        public final int key;
        public final boolean down;
        public final char chr;

        KeyEvent(int key, boolean down, char chr) {
            this.key = key;
            this.down = down;
            this.chr = chr;
        }
    }

    public static final class MouseEventRec {
        public final int button;
        public final int dx;
        public final int dy;

        MouseEventRec(int button, int dx, int dy) {
            this.button = button;
            this.dx = dx;
            this.dy = dy;
        }
    }

    public static final Deque<KeyEvent> keyQueue = new ArrayDeque<>();
    public static final Deque<MouseEventRec> mouseQueue = new ArrayDeque<>();
    private static final Set<Integer> buttonsDown = new HashSet<>();

    public static int mouseX;
    public static int mouseY;
    public static boolean grabbed;
    private static int lastX;
    private static int lastY;
    private static HTMLCanvasElement canvas;

    public static void attach(HTMLCanvasElement c, HTMLDocument doc) {
        canvas = c;

        doc.addEventListener("keydown", e -> {
            KeyboardEvent ke = (KeyboardEvent) e;
            int code = Keys.fromDom(ke.getCode(), ke.getKey());
            char ch = charOf(ke.getKey());
            keyQueue.add(new KeyEvent(code, true, ch));
            // Arrows and space scroll the page otherwise, which fights the game.
            if (Keys.isGameKey(code)) {
                e.preventDefault();
            }
        });

        doc.addEventListener("keyup", e -> {
            KeyboardEvent ke = (KeyboardEvent) e;
            keyQueue.add(new KeyEvent(Keys.fromDom(ke.getCode(), ke.getKey()), false, '\0'));
        });

        c.addEventListener("mousemove", e -> {
            MouseEvent me = (MouseEvent) e;
            updatePos(me);
        });

        c.addEventListener("mousedown", e -> {
            MouseEvent me = (MouseEvent) e;
            updatePos(me);
            buttonsDown.add((int) me.getButton());
            mouseQueue.add(new MouseEventRec(me.getButton(), mouseX - lastX, mouseY - lastY));
        });

        c.addEventListener("mouseup", e -> {
            MouseEvent me = (MouseEvent) e;
            updatePos(me);
            buttonsDown.remove((Integer) (int) me.getButton());
            // Button released: LWJGL reports the same button with state polled separately.
            mouseQueue.add(new MouseEventRec(me.getButton(), 0, 0));
        });

        c.addEventListener("wheel", e -> {
            WheelEvent we = (WheelEvent) e;
            e.preventDefault();
        });

        // The canvas needs focus to receive key events, and no context menu on right-click.
        c.addEventListener("contextmenu", e -> e.preventDefault());
    }

    private static void updatePos(MouseEvent me) {
        lastX = mouseX;
        lastY = mouseY;
        // Canvas backing store is fixed at the game's logical size; CSS may scale it.
        double scaleX = canvas.getWidth() / (double) canvas.getClientWidth();
        double scaleY = canvas.getHeight() / (double) canvas.getClientHeight();
        mouseX = (int) ((me.getClientX() - canvas.getBoundingClientRect().getLeft()) * scaleX);
        int domY = (int) ((me.getClientY() - canvas.getBoundingClientRect().getTop()) * scaleY);
        // LWJGL 2 convention: origin bottom-left. Input.java undoes this itself.
        mouseY = canvas.getHeight() - domY;
    }

    static boolean isDown(int button) {
        return buttonsDown.contains(button);
    }

    private static char charOf(String key) {
        return key != null && key.length() == 1 ? key.charAt(0) : '\0';
    }
}
