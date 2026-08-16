// Shim: LWJGL 3 dropped util.vector.Vector3f. Game uses fields and a few setters.
package org.lwjgl.util.vector;

public class Vector3f {
    public float x, y, z;

    public Vector3f() {}
    public Vector3f(float x, float y, float z) { this.x = x; this.y = y; this.z = z; }

    public Vector3f set(float x, float y, float z) {
        this.x = x; this.y = y; this.z = z;
        return this;
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public float getZ() { return z; }
    public void  setX(float v) { x = v; }
    public void  setY(float v) { y = v; }
    public void  setZ(float v) { z = v; }
}
