// Shim: legacy LWJGL 2 Cursor used by Render.set_cursor() to upload an image
// pointer. Stored as data only; Mouse.setNativeCursor is a no-op for now.
package org.lwjgl.input;

import org.lwjgl.LWJGLException;

import java.nio.IntBuffer;

public class Cursor {
    public final int width, height, xHotspot, yHotspot, numImages;
    public final IntBuffer images;
    public final IntBuffer delays;

    public Cursor(int width, int height, int xHotspot, int yHotspot, int numImages,
                  IntBuffer images, IntBuffer delays) throws LWJGLException {
        this.width = width;
        this.height = height;
        this.xHotspot = xHotspot;
        this.yHotspot = yHotspot;
        this.numImages = numImages;
        this.images = images;
        this.delays = delays;
    }
}
