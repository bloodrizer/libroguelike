package com.nuclearunicorn.libroguelike.events;

import org.lwjgl.util.Point;

/**

 */
public class PointBasedEvent extends Event {
    protected Point origin;

    public PointBasedEvent(Point origin){
        this.origin = origin;
    }
    
    public Point getOrigin(){
        return origin;
    }
}
