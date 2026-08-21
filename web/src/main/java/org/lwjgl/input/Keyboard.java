package org.lwjgl.input;

/**
 * Web shim: LWJGL 2-style polling Keyboard backed by DOM key events.
 *
 * Key constants are private to this build (see {@link Keys}); the game only
 * ever compares against these names.
 */
public final class Keyboard {

    private Keyboard() {
    }

    public static final char CHAR_NONE = '\0';

    public static final int KEY_A = 'A';
    public static final int KEY_D = 'D';
    public static final int KEY_E = 'E';
    public static final int KEY_G = 'G';
    public static final int KEY_I = 'I';
    public static final int KEY_L = 'L';
    public static final int KEY_M = 'M';
    public static final int KEY_N = 'N';
    public static final int KEY_Q = 'Q';
    public static final int KEY_S = 'S';
    public static final int KEY_T = 'T';
    public static final int KEY_W = 'W';

    public static final int KEY_F1 = 1001;
    public static final int KEY_F2 = 1002;
    public static final int KEY_F3 = 1003;
    public static final int KEY_F4 = 1004;
    public static final int KEY_TAB = 1005;
    public static final int KEY_BACK = 1006;
    public static final int KEY_RETURN = 1007;
    public static final int KEY_SPACE = 1008;
    public static final int KEY_ESCAPE = 1009;
    public static final int KEY_UP = 1010;
    public static final int KEY_DOWN = 1011;
    public static final int KEY_LEFT = 1012;
    public static final int KEY_RIGHT = 1013;
    public static final int KEY_LBRACKET = 1014;
    public static final int KEY_RBRACKET = 1015;
    public static final int KEY_LMENU = 1016;
    public static final int KEY_LCONTROL = 1017;
    public static final int KEY_LSHIFT = 1018;
    public static final int KEY_LMETA = 1019;
    public static final int KEY_RMETA = 1020;

    private static InputBridge.KeyEvent current;

    public static boolean next() {
        current = InputBridge.keyQueue.poll();
        return current != null;
    }

    public static int getEventKey() {
        return current == null ? 0 : current.key;
    }

    public static boolean getEventKeyState() {
        return current != null && current.down;
    }

    public static char getEventCharacter() {
        return current == null ? CHAR_NONE : current.chr;
    }
}
