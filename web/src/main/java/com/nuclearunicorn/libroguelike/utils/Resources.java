package com.nuclearunicorn.libroguelike.utils;

import com.nuclearunicorn.web.Assets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Web build: text data comes from the registry the page filled before wasm
 * started (see Assets), because a wasm module has no classpath to read.
 * Paths are the same "/resources/..." names the desktop build uses.
 */
public final class Resources {

    private Resources() {
    }

    public static List<String> lines(String path) {
        String body = text(path);
        if (body == null) {
            System.err.println("missing resource: " + path);
            return Collections.emptyList();
        }
        List<String> out = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < body.length(); i++) {
            if (body.charAt(i) == '\n') {
                out.add(strip(body.substring(start, i)));
                start = i + 1;
            }
        }
        if (start < body.length()) {
            out.add(strip(body.substring(start)));
        }
        return out;
    }

    /** Files served over HTTP may carry CRLF; the desktop reader drops it too. */
    private static String strip(String s) {
        return s.endsWith("\r") ? s.substring(0, s.length() - 1) : s;
    }

    public static String text(String path) {
        return Assets.text(path);
    }
}
