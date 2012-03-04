/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nuclearunicorn.libroguelike.utils;

import org.lwjgl.util.Point;

/**
 *
 * @author bloodrizer
 */
public abstract class LazyLoadWorldElement<ElementType> extends java.util.HashMap<Point, ElementType>{
    private static Point util_point = new Point(0,0);
    
    public final ElementType get_cached(int x, int y){
        util_point.setLocation(x, y);
        return get_cached(util_point);
    }
    
    public final ElementType get_cached(Point origin){
        ElementType element = super.get(origin);
        if (element == null){
            Point key = new Point(origin);

            element = precache(key);
            put(key, element);

            return element;
        }
        return element;
    }
    
    protected abstract ElementType precache(Point origin);
}
