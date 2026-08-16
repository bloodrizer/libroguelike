package org.lwjgl.input;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * DOM KeyboardEvent.code -> the integer key names the game compares against.
 *
 * The values are arbitrary but stable: the game only ever compares them to the
 * Keyboard.KEY_* constants, never to raw platform scancodes.
 */
final class Keys {

    private Keys() {
    }

    private static final Map<String, Integer> BY_CODE = new HashMap<>();
    private static final Set<Integer> GAME_KEYS = new HashSet<>();

    static {
        letter("KeyA", Keyboard.KEY_A);
        letter("KeyD", Keyboard.KEY_D);
        letter("KeyE", Keyboard.KEY_E);
        letter("KeyG", Keyboard.KEY_G);
        letter("KeyI", Keyboard.KEY_I);
        letter("KeyL", Keyboard.KEY_L);
        letter("KeyM", Keyboard.KEY_M);
        letter("KeyQ", Keyboard.KEY_Q);
        letter("KeyS", Keyboard.KEY_S);
        letter("KeyT", Keyboard.KEY_T);
        letter("KeyW", Keyboard.KEY_W);

        letter("F1", Keyboard.KEY_F1);
        letter("F2", Keyboard.KEY_F2);
        letter("F3", Keyboard.KEY_F3);
        letter("F4", Keyboard.KEY_F4);
        letter("Tab", Keyboard.KEY_TAB);
        letter("Backspace", Keyboard.KEY_BACK);
        letter("Enter", Keyboard.KEY_RETURN);
        letter("Space", Keyboard.KEY_SPACE);
        letter("Escape", Keyboard.KEY_ESCAPE);
        letter("ArrowUp", Keyboard.KEY_UP);
        letter("ArrowDown", Keyboard.KEY_DOWN);
        letter("ArrowLeft", Keyboard.KEY_LEFT);
        letter("ArrowRight", Keyboard.KEY_RIGHT);
        letter("BracketLeft", Keyboard.KEY_LBRACKET);
        letter("BracketRight", Keyboard.KEY_RBRACKET);
        letter("AltLeft", Keyboard.KEY_LMENU);
        letter("ControlLeft", Keyboard.KEY_LCONTROL);
        letter("ShiftLeft", Keyboard.KEY_LSHIFT);
        letter("MetaLeft", Keyboard.KEY_LMETA);
        letter("MetaRight", Keyboard.KEY_RMETA);
    }

    private static void letter(String code, int key) {
        BY_CODE.put(code, key);
        GAME_KEYS.add(key);
    }

    static int fromDom(String code, String key) {
        Integer v = code != null ? BY_CODE.get(code) : null;
        if (v != null) {
            return v;
        }
        // Unmapped keys still need a stable identity for text input.
        return key != null && key.length() == 1 ? Character.toUpperCase(key.charAt(0)) : 0;
    }

    /** Keys the page must not act on itself (scrolling, focus traversal). */
    static boolean isGameKey(int key) {
        return GAME_KEYS.contains(key);
    }
}
