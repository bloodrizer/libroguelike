package com.nuclearunicorn.serialkiller.utils;

import org.lwjgl.util.Point;

import java.util.List;

/**

 */
public class RLMath {
    public static Point getNearestPoint(List<Point> list, Point anchor){

        double min_disst = Double.MAX_VALUE;
        Point nearestPoint = null;
        for (Point node : list){
            Double disst = Math.pow(anchor.getX()-node.getX(),2) + Math.pow(anchor.getY()-node.getY(),2);
            if (disst < min_disst){
                min_disst = disst;
                nearestPoint = node;
            }
        }

        return nearestPoint;
    }
}
