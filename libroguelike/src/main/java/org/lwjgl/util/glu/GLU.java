// Shim: LWJGL 3 dropped GLU. Only gluPerspective is used.
package org.lwjgl.util.glu;

import static org.lwjgl.opengl.GL11.glMultMatrixf;

public final class GLU {
    private GLU() {}

    public static void gluPerspective(float fovy, float aspect, float zNear, float zFar) {
        float f = (float) (1.0 / Math.tan(Math.toRadians(fovy) / 2.0));
        // Column-major matrix expected by glMultMatrixf
        float[] m = new float[] {
            f / aspect, 0, 0,                                  0,
            0,          f, 0,                                  0,
            0,          0, (zFar + zNear) / (zNear - zFar),    -1,
            0,          0, (2 * zFar * zNear) / (zNear - zFar), 0
        };
        glMultMatrixf(m);
    }
}
