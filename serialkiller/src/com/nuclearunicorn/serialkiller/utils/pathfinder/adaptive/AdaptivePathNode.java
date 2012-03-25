package com.nuclearunicorn.serialkiller.utils.pathfinder.adaptive;


import org.lwjgl.util.Point;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Administrator
 * Date: 25.03.12
 * Time: 13:28
 * To change this template use File | Settings | File Templates.
 */
public class AdaptivePathNode implements Comparable<AdaptivePathNode>{
    public Point point;

    public List<AdaptivePath> nb = new ArrayList<AdaptivePath>(4);
    public double minDistance = Double.POSITIVE_INFINITY;
    public AdaptivePathNode prev;

    public AdaptivePathNode(Point point){
        this.point = point;
    }

    @Override
    public int compareTo(AdaptivePathNode other) {
        return Double.compare(minDistance, other.minDistance);
    }

    public boolean isNodeOf(Point point) {
        return this.point.equals(point);
    }

    @Override
    public String toString() {
        return "@["+point.getX()+","+point.getY()+"]";
    }
}
