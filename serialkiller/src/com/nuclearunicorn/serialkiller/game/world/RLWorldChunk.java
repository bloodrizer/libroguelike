package com.nuclearunicorn.serialkiller.game.world;

import com.nuclearunicorn.libroguelike.game.world.WorldChunk;
import org.lwjgl.util.Point;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Administrator
 * Date: 09.03.12
 * Time: 14:55
 * To change this template use File | Settings | File Templates.
 */
public class RLWorldChunk extends WorldChunk {

    List<Point> milestones = new ArrayList<Point>();

    public boolean hasMilestone(Point ms){
        return milestones.contains(ms);
    }
    
    public void addMilestone(Point ms){
        milestones.add(ms);
    }
    
    public List<Point> getMilestones(){
        return this.milestones;
    }

    public RLWorldChunk(int x, int y) {
        super(x, y);
    }
}
