package com.nuclearunicorn.serialkiller.game.controllers;

import com.nuclearunicorn.libroguelike.game.ent.controller.NpcController;
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
                this.path = setDebugAdaptivePath(adaptivePath, source, target);
                return;     //fuckup
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
                    if (owner.getLayer().get_tile(point.getX(), point.getY()).isBlocked()){
                        //if bresinham trace vector detects collision, replace it with accurate astar tracer

                        //TODO: rather than re-calculating WHOLE path, we'd better recalculate part of the path with a some kind of prediction
                        /*
                            e.g. :
                            *-*-*X*-*-* <--obstacle
                            *
                            * -*\__/*-*-   <-- re-calculated part
                         */

                        tmpPath = this.getAstarPath(prevNode.point, node.point);
                        if (tmpPath == null){   //safe switch lol
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
                this.path = setDebugAdaptivePath(adaptivePath, source, target);
                return;     //fuckup
            }
            postfixPath.remove(0);
            debugPath.addAll(postfixPath);
        }

        this.path = debugPath;
        this.destination = target;
    }

    private List<Point> setDebugAdaptivePath(List<AdaptiveNode> adaptivePath, Point source, Point target) {
        List<Point> debugPath = new ArrayList<Point>();
        debugPath.add(source);
        for (AdaptiveNode node: adaptivePath){
            debugPath.add(node.point);
        }
        debugPath.add(target);
        return debugPath;
    }

    //------------------------------------------------------------------------------------------------------------------

    @Override
    public void e_on_obstacle(int x, int y) {
        if (owner.getAI() != null ){
            owner.getAI().e_on_obstacle(x, y);
        }
    }
}
