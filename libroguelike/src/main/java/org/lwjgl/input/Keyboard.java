// Shim: LWJGL 2-style polling Keyboard backed by GLFW callbacks.
// Key constants alias to GLFW codes (the game only compares names, never raw ints).
package org.lwjgl.input;

import static org.lwjgl.glfw.GLFW.*;

public final class Keyboard {
    public static final char CHAR_NONE = '\0';

    public static final int KEY_A = GLFW_KEY_A;
    public static final int KEY_D = GLFW_KEY_D;
    public static final int KEY_E = GLFW_KEY_E;
    public static final int KEY_G = GLFW_KEY_G;
    public static final int KEY_I = GLFW_KEY_I;
    public static final int KEY_L = GLFW_KEY_L;
    public static final int KEY_M = GLFW_KEY_M;
    public static final int KEY_Q = GLFW_KEY_Q;
    public static final int KEY_S = GLFW_KEY_S;
    public static final int KEY_T = GLFW_KEY_T;
    public static final int KEY_W = GLFW_KEY_W;

    public static final int KEY_F1       = GLFW_KEY_F1;
    public static final int KEY_F2       = GLFW_KEY_F2;
    public static final int KEY_F3       = GLFW_KEY_F3;
    public static final int KEY_F4       = GLFW_KEY_F4;
    public static final int KEY_TAB      = GLFW_KEY_TAB;
    public static final int KEY_BACK     = GLFW_KEY_BACKSPACE;
    public static final int KEY_RETURN   = GLFW_KEY_ENTER;
    public static final int KEY_SPACE    = GLFW_KEY_SPACE;
    public static final int KEY_ESCAPE   = GLFW_KEY_ESCAPE;
    public static final int KEY_UP       = GLFW_KEY_UP;
    public static final int KEY_DOWN     = GLFW_KEY_DOWN;
    public static final int KEY_LEFT     = GLFW_KEY_LEFT;
    public static final int KEY_RIGHT    = GLFW_KEY_RIGHT;
    public static final int KEY_LBRACKET = GLFW_KEY_LEFT_BRACKET;
    public static final int KEY_RBRACKET = GLFW_KEY_RIGHT_BRACKET;

    public static final int KEY_LMENU    = GLFW_KEY_LEFT_ALT;
    public static final int KEY_LCONTROL = GLFW_KEY_LEFT_CONTROL;
    public static final int KEY_LSHIFT   = GLFW_KEY_LEFT_SHIFT;

    // macOS Command keys — treated as an attack modifier alongside Control (see Input.java).
    public static final int KEY_LMETA    = GLFW_KEY_LEFT_SUPER;
    public static final int KEY_RMETA    = GLFW_KEY_RIGHT_SUPER;

    private static InputBridge.KeyEvent current;

    public static boolean next() {
        current = InputBridge.keyQueue.poll();
        return current != null;
    }

    public static int  getEventKey()       { return current == null ? 0 : current.key; }
    public static boolean getEventKeyState() { return current != null && current.down; }
    public static char getEventCharacter() { return current == null ? CHAR_NONE : current.chr; }
}
