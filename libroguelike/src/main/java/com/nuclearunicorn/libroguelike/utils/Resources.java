package com.nuclearunicorn.libroguelike.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Reads bundled text data.
 *
 * Callers go through here rather than calling getResourceAsStream directly, so
 * the browser build can serve the same paths from assets fetched by the page —
 * a wasm module has no classpath to read from.
 */
public final class Resources {

    private Resources() {
    }

    /** Lines of a bundled text resource; empty (never null) when it is missing. */
    public static List<String> lines(String path) {
        String body = text(path);
        if (body == null) {
            System.err.println("missing resource: " + path);
            return Collections.emptyList();
        }
        List<String> out = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new java.io.StringReader(body))) {
            String line;
            while ((line = br.readLine()) != null) {
                out.add(line);
            }
        } catch (IOException e) {
            System.err.println("failed reading " + path + ": " + e);
        }
        return out;
    }

    /** Whole text resource, or null when it is missing. */
    public static String text(String path) {
        try (InputStream is = Resources.class.getResourceAsStream(path)) {
            if (is == null) {
                return null;
            }
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append('\n');
                }
            }
            return sb.toString();
        } catch (IOException e) {
            System.err.println("failed reading " + path + ": " + e);
            return null;
        }
    }
}
