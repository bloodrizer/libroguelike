package com.nuclearunicorn.serialkiller.utils.pathfinder.adaptive;

import com.nuclearunicorn.serialkiller.game.world.RLTile;
import com.nuclearunicorn.serialkiller.game.world.RLWorldChunk;
import org.lwjgl.util.Point;

import java.util.*;

/**
 */
public class AdaptivePathfinder {

    public static List<AdaptiveNode> nodes = new ArrayList<AdaptiveNode>(32);
    public static List<AdaptivePath> edges = new ArrayList<AdaptivePath>(16);

    private static AdaptiveNode getNode(Point point){
        for (AdaptiveNode node : nodes){
            if (node.isNodeOf(point)){
                return node;
            }
        }
        AdaptiveNode node = new AdaptiveNode(point);
        nodes.add(node);
        return node;
    }
    
    public static void addLink(AdaptiveNode from, AdaptiveNode to, int cost){

        AdaptivePath path = new AdaptivePath(
                from,
                to,
                cost);
        edges.add(path);
        from.nb.add(path);
    }

    public static void addPoint(RLWorldChunk chunk, Point newNode){

        List<Point> mst = chunk.getMilestones();
        
        for (Point registeredNode: mst){
            if (registeredNode.equals(newNode)){
                return;
            }

            int pathCost = tracePathLength(chunk, registeredNode, newNode);
            if (pathCost > 0){
                //System.out.println("adding links from point " + newNode + " to "+nodes.size()+" nodes");
                addLink(getNode(registeredNode), getNode(newNode), pathCost);
                addLink(getNode(newNode), getNode(registeredNode), pathCost);    //?
            }
        }

       }

    /*
        Trace distance between two milestones, return -1 if obstacle is blocking direct line movement from ms 1 to ns 2
     */
    private static int tracePathLength(RLWorldChunk chunk, Point samplePoint, Point point) {
        //return false;  //To change body of created methods use File | Settings | File Templates.
        List<Point> line = BresinhamLine.line(samplePoint.getX(), samplePoint.getY(), point.getX(), point.getY());
        //System.out.println("tracing bresingam line of size "+line.size());
        int i = 0;
        for (Point step: line){
            i++;
            RLTile rlTile = (RLTile)chunk.tile_data.get(step);
            if (rlTile.isWall()){
                //System.out.println("debug: bresinham collision on step #"+i);
                return -1;
            }
        }

        return line.size();
    }
    
    public static void calculateAdaptiveRoutes(Point from){


        AdaptiveNode source = getNode(from);
        source.minDistance = 0;

        PriorityQueue<AdaptiveNode> vertexQueue = new PriorityQueue<AdaptiveNode>();
        vertexQueue.add(source);

        while (!vertexQueue.isEmpty()) {
            AdaptiveNode u = vertexQueue.poll();

            //v?
            //d?

            for (AdaptivePath e : u.nb)
            {
                AdaptiveNode v = e.to;
                double weight = e.cost;
                //relax the edge
                double distanceThroughU = u.minDistance + weight;
                if (distanceThroughU < v.minDistance) {
                    //if (v.minDistance != Double.POSITIVE_INFINITY) {
                        vertexQueue.remove(v);
                    //}

                    v.minDistance = distanceThroughU ;
                    v.prev = u;
                    vertexQueue.add(v);
                }
            }
        }
    }

    public static List<AdaptiveNode> getShortestPathTo(Point target){

        AdaptiveNode targetNode = getNode(target);

        List<AdaptiveNode> path = new ArrayList<AdaptiveNode>();
        for (AdaptiveNode vertex = targetNode; vertex != null; vertex = vertex.prev){
            path.add(vertex);
        }
        Collections.reverse(path);
        return path;
    }

    public static void resetState() {
        for (AdaptiveNode node: nodes){
            node.minDistance = Double.POSITIVE_INFINITY;
            node.prev = null;
        }
    }

}
