// Shim: LWJGL 3 dropped DisplayMode. We only need width/height.
package org.lwjgl.opengl;

public class DisplayMode {
    private final int width, height;

    public DisplayMode(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }
}
