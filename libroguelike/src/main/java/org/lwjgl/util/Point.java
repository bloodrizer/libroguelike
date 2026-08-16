// Shim: LWJGL 3 dropped util.Point. Game uses (x,y) integer pairs.
package org.lwjgl.util;

public class Point {
    private int x, y;

    public Point() {}
    public Point(int x, int y) { this.x = x; this.y = y; }
    public Point(Point src) { this.x = src.x; this.y = src.y; }

    public int getX() { return x; }
    public int getY() { return y; }
    public void setX(int x) { this.x = x; }
    public void setY(int y) { this.y = y; }
    public void setLocation(int x, int y) { this.x = x; this.y = y; }
    public void setLocation(Point p) { this.x = p.x; this.y = p.y; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Point p)) return false;
        return p.x == x && p.y == y;
    }

    @Override
    public int hashCode() { return 31 * x + y; }

    @Override
    public String toString() { return "Point(" + x + "," + y + ")"; }
}
