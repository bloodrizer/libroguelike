package com.nuclearunicorn.serialkiller.utils.pathfinder.adaptive;

import org.lwjgl.util.Point;

import java.util.ArrayList;
import java.util.List;

/**
 * Optimised version of the Bresinham line rasterisation algorythm
 */
public class BresinhamLine {
    public static List<Point> line(int x, int y, int x2, int y2) {

        List<Point> line = new ArrayList<Point>();

        int w = x2 - x;
        int h = y2 - y;
        int dx1 = 0, dy1 = 0, dx2 = 0, dy2 = 0;
        if (w < 0) { dx1 = -1; } else { if (w > 0) { dx1 = 1; }}
        if (h < 0) { dy1 = -1; } else { if (h > 0) { dy1 = 1; }}
        if (w < 0) { dx2 = -1; } else { if (w > 0) { dx2 = 1; }}

        int longest = Math.abs(w);
        int shortest = Math.abs(h);
        if (!(longest > shortest)) {
            longest = Math.abs(h);
            shortest = Math.abs(w);
            if (h < 0) { dy2 = -1;}
            else { if (h > 0) { dy2 = 1; }}
            dx2 = 0;
        }
        int numerator = longest >> 1;
        for (int i = 0; i <= longest; i++) {

            line.add(new Point(x, y));

            numerator += shortest;
            if (!(numerator < longest)) {
                numerator -= longest;
                x += dx1;
                y += dy1;
            } else {
                x += dx2;
                y += dy2;
            }
        }

        return line;
    }
}
