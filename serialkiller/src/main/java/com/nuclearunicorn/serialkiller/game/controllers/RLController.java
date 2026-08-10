package com.nuclearunicorn.serialkiller.game.controllers;

import com.nuclearunicorn.libroguelike.game.ai.AI;
import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.game.ent.controller.NpcController;
import com.nuclearunicorn.serialkiller.game.ai.behavior.SleepAction;
import com.nuclearunicorn.serialkiller.game.world.RLTile;
import com.nuclearunicorn.serialkiller.game.world.entities.EntityRLHuman;
import com.nuclearunicorn.serialkiller.game.world.RLWorldChunk;
import com.nuclearunicorn.serialkiller.utils.pathfinder.adaptive.AdaptiveNode;
import com.nuclearunicorn.serialkiller.utils.pathfinder.adaptive.AdaptivePathfinder;
import com.nuclearunicorn.serialkiller.utils.pathfinder.adaptive.BresinhamLine;
import org.lwjgl.util.Point;

import java.util.ArrayList;
import java.util.List;

//------------------------------------------------------------------------------------------------------------------
/*
   Time for some hot shit like adaptive pathfinding
*/
//------------------------------------------------------------------------------------------------------------------

public class RLController extends NpcController {

    /*
        Calculate normal a-star path.
        If failed to find optimal route, use adaptive pathfinding, which works better on longer routes
     */
    @Override
    public void set_destination(Point destination) {
        super.set_destination(destination);

        /*if (!this.hasPath()){
            calculateAdaptivePath(owner.origin, destination);
        }*/
    }

    /*
    * Calculate path, using pre-calculated milestone graph as base for pathfinding route
    */
    public void calculateAdaptivePath(Point source, Point target) {

        source = new Point(source);
        target = new Point(target);

        //every A* below is a leg of the milestone route, and every one of them has
        //fallbackDirect behind it - a failed leg is not worth a line of log
        quietAstar = true;

        Point fromMS = new Point(((RLWorldChunk)owner.get_chunk()).getNearestMilestone(source));
        Point toMS =   new Point(((RLWorldChunk)owner.get_chunk()).getNearestMilestone(target));

        AdaptivePathfinder.resetState();
        AdaptivePathfinder.calculateAdaptiveRoutes(fromMS);
        List<AdaptiveNode> adaptivePath = AdaptivePathfinder.getShortestPathTo(toMS);


        /*System.out.println("moving from " + owner.origin + " to " + target + " path: " + adaptivePath);
        System.out.println("(using " + fromMS + " to " + toMS + " as adaptive nodes)");
        System.out.println("");*/


        List<Point> debugPath = new ArrayList<Point>();

        if (!owner.origin.equals(fromMS)){
            List<Point> prefixPath = this.getAstarPath(owner.origin, fromMS);
            if (prefixPath == null){
                fallbackDirect(target);
                return;
            }
            prefixPath.remove(0);
            debugPath.addAll(prefixPath);
        }

        this.adaptivePathPool.clear();

        //divide adaptive path into small steps based on bresinham algorithm
        AdaptiveNode prevNode = adaptivePath.get(0);
        for (AdaptiveNode node: adaptivePath){

            this.adaptivePathPool.add(node.point);

            if (prevNode != node){
                List<Point> tmpPath = BresinhamLine.line(prevNode.point.getX(), prevNode.point.getY(), node.point.getX(), node.point.getY());
                for(Point point: tmpPath){
                    RLTile tile = (RLTile) owner.getLayer().get_tile(point.getX(), point.getY());
                    //a passer-by on the line is not worth an A* detour; solid geometry is
                    if (tile != null && tile.isPathBlocked()){
                        //if bresinham trace vector detects collision, replace it with accurate astar tracer

                        //TODO: rather than re-calculating WHOLE path, we'd better recalculate part of the path with a some kind of prediction
                        /*
                            e.g. :
                            *-*-*X*-*-* <--obstacle
                            *
                            * -*\__/*-*-   <-- re-calculated part
                         */

                        tmpPath = this.getAstarPath(prevNode.point, node.point);
                        if (tmpPath == null){
                            fallbackDirect(target);
                            return;
                        }
                        tmpPath.remove(0);
                    }
                }
                debugPath.addAll(tmpPath);
            }
            prevNode = node;
        }

        //debugPath.add(target);

        if (!toMS.equals(target)){
            List<Point> postfixPath = this.getAstarPath(toMS, target);
            if (postfixPath == null){
                fallbackDirect(target);
                return;
            }
            postfixPath.remove(0);
            debugPath.addAll(postfixPath);
        }

        quietAstar = false;
        this.path = debugPath;
        this.destination = target;
    }

    /**
     * The milestone route did not come together. Ask A* for the whole trip instead, and if even
     * that fails, admit there is no path.
     *
     * <p>What used to happen here was a "path" made of the milestone points themselves plus the
     * target — a debug polyline, walked by {@link #follow_path} as a series of straight lines
     * through whatever stood in between. That is where the wild cross-country routes came from,
     * and every wall, window and crate on the diagonal got walked into on the way.
     */
    private void fallbackDirect(Point target) {
        quietAstar = false;   //last resort: if this one fails, it is worth hearing about
        List<Point> direct = this.getAstarPath(owner.origin.getX(), owner.origin.getY(),
                target.getX(), target.getY());
        if (direct == null || direct.isEmpty()){
            this.path = null;
            this.destination = null;
            return;
        }
        direct.remove(0);   //first element is where we already stand
        this.path = direct;
        this.destination = target;
        this.step = null;
    }

    /**
     * Who a townsperson may edge past. Anyone awake and on their feet: the alternative is a
     * hallway that stays plugged all night, because the person in front has nowhere to go
     * either and re-planning cannot invent a second corridor.
     *
     * <p>Two exceptions. The player is never shoved — where they stand is their decision. And
     * nobody is tipped out of their own bed to make room.
     */
    @Override
    protected boolean squeezePast(Entity blocker) {
        if (owner.isPlayerEnt() || blocker.isPlayerEnt() || !(blocker instanceof EntityRLHuman)) {
            return false;
        }
        AI blockerAI = blocker.getAI();
        if (blockerAI != null && SleepAction.STATE.equals(blockerAI.getState())) {
            return false;
        }
        return swapWith(blocker);
    }

    //------------------------------------------------------------------------------------------------------------------

    @Override
    public void e_on_obstacle(int x, int y) {
        if (owner.getAI() != null ){
            owner.getAI().e_on_obstacle(x, y);
        }
    }
}
