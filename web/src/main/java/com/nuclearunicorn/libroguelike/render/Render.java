package com.nuclearunicorn.libroguelike.render;

import com.nuclearunicorn.web.Assets;
import org.newdawn.slick.opengl.Texture;
import org.newdawn.slick.opengl.TextureLoader;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Web build of the texture cache.
 *
 * Same contract as the desktop class, but textures come from images the page
 * preloaded rather than from a decoded InputStream, and the mouse cursor is a
 * CSS property instead of a GL bitmap cursor.
 */
public class Render {

    private static final Map<String, Texture> texture_cache = new HashMap<>();

    public static final int CURSOR_SIZE = 32;

    public static Texture precache_texture(String name, String format) {
        Texture texture;
        try {
            texture = TextureLoader.getTexture(name);
        } catch (IOException ex) {
            // Same contract as the desktop build: an unknown name yields the
            // placeholder, never null. Several callers dereference the result.
            System.err.println("missing texture('" + name + "') - using default");
            texture = invalidTexture();
        }
        texture_cache.put(name, texture);
        return texture;
    }

    private static Texture invalid;

    /** Cached so a page missing the placeholder does not retry it every frame. */
    private static Texture invalidTexture() {
        if (invalid == null) {
            try {
                invalid = TextureLoader.getTexture("/resources/invalid_texture.png");
            } catch (IOException e) {
                invalid = TextureLoader.blank();
            }
        }
        return invalid;
    }

    public static Texture precache_texture(String name) {
        return precache_texture(name, "PNG");
    }

    public static Texture get_texture(String name) {
        Texture texture = texture_cache.get(name);
        return texture != null ? texture : precache_texture(name);
    }

    public static void bind_texture(String name) {
        Texture texture = get_texture(name);
        if (texture != null) {
            texture.bind();
        }
    }

    public static Texture getInvalidTexture() throws IOException {
        return invalidTexture();
    }

    private static String cursor_name = "";

    public static void set_cursor(String name) {
        if (cursor_name.equals(name)) {
            return;
        }
        cursor_name = name;
        // A GL bitmap cursor has no browser analogue; the canvas keeps the default.
    }

    /** Present for source compatibility; the web build has no AWT raster to read. */
    public static java.nio.IntBuffer getHandMousePointer(String cursor_name) {
        return org.lwjgl.BufferUtils.createIntBuffer(CURSOR_SIZE * CURSOR_SIZE);
    }
}
