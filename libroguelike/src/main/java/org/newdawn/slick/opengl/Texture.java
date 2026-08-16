// Replacement for slick-util Texture (interface in slick-util, simple class here).
package org.newdawn.slick.opengl;

import static org.lwjgl.opengl.GL11.*;

public class Texture {
    private final int id;
    private final int width, height;
    private final int imgWidth, imgHeight;

    public Texture(int id, int width, int height, int imgWidth, int imgHeight) {
        this.id = id;
        this.width = width;
        this.height = height;
        this.imgWidth = imgWidth;
        this.imgHeight = imgHeight;
    }

    public int getTextureID() { return id; }
    public int getTextureWidth()  { return width; }
    public int getTextureHeight() { return height; }
    public int getImageWidth()  { return imgWidth; }
    public int getImageHeight() { return imgHeight; }
    public float getWidth()  { return imgWidth  / (float) width; }
    public float getHeight() { return imgHeight / (float) height; }

    public void bind() {
        glBindTexture(GL_TEXTURE_2D, id);
    }
}
