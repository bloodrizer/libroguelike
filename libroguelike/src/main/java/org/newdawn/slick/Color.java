// Replacement for slick-util Color: int-RGBA constructors and float fields,
// matching the API the game uses.
package org.newdawn.slick;

public class Color {
    public static final Color white  = new Color(255, 255, 255);
    public static final Color black  = new Color(0, 0, 0);
    public static final Color red    = new Color(255, 0, 0);
    public static final Color green  = new Color(0, 255, 0);
    public static final Color blue   = new Color(0, 0, 255);
    public static final Color yellow = new Color(255, 255, 0);
    public static final Color orange = new Color(255, 200, 0);
    public static final Color cyan   = new Color(0, 255, 255);
    public static final Color magenta = new Color(255, 0, 255);
    public static final Color gray   = new Color(128, 128, 128);
    public static final Color lightGray = new Color(192, 192, 192);
    public static final Color darkGray  = new Color(64, 64, 64);

    public float r, g, b, a;

    public Color(int r, int g, int b)        { this(r, g, b, 255); }
    public Color(int r, int g, int b, int a) {
        this.r = r / 255f; this.g = g / 255f; this.b = b / 255f; this.a = a / 255f;
    }
    public Color(float r, float g, float b)            { this(r, g, b, 1f); }
    public Color(float r, float g, float b, float a)   {
        this.r = r; this.g = g; this.b = b; this.a = a;
    }

    public void bind() {
        org.lwjgl.opengl.GL11.glColor4f(r, g, b, a);
    }
}
