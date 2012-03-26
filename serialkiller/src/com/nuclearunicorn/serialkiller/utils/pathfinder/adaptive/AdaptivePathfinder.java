package com.nuclearunicorn.serialkiller.utils.pathfinder.adaptive;

import com.nuclearunicorn.serialkiller.game.world.RLTile;
import com.nuclearunicorn.serialkiller.game.world.RLWorldChunk;
import org.lwjgl.util.Point;

import java.util.*;

/**
 */
public class AdaptivePathfinder {
    //private static Map<Point, ArrayList<AdaptivePath>> links = new HashMap<Point,ArrayList<Point>>(32);
    //private static Map<AdaptivePathNode ,ArrayList<AdaptivePath>> linkedNodes = new HashMap<AdaptivePathNode,ArrayList<AdaptivePath>>(32);
    public static List<AdaptivePathNode> nodes = new ArrayList<AdaptivePathNode>(32);
    public static List<AdaptivePath> edges = new ArrayList<AdaptivePath>(16);



    private static AdaptivePathNode getNode(Point point){
        for (AdaptivePathNode node : nodes){
            if (node.isNodeOf(point)){
                return node;
            }
        }
        AdaptivePathNode node = new AdaptivePathNode(point);
        nodes.add(node);
        return node;
    }
    
    public static void addLink(AdaptivePathNode from, AdaptivePathNode to, int cost){
        //AdaptivePathNode nodeFrom = getNode(from);
        //AdaptivePathNode nodeTo = getNode(to);


        AdaptivePath path = new AdaptivePath(
                from,
                to,
                cost);
        edges.add(path);
        from.nb.add(path);
    }

    public static void addPoint(RLWorldChunk chunk, Point newNode){
        //AdaptivePathNode node = new AdaptivePathNode(newNode);
        //nodes.add(node);


        List<Point> mst = chunk.getMilestones();
        
        for (Point registeredNode: mst){
            if (registeredNode.equals(newNode)){
                return;
            }

            int pathCost = tracePathLength(chunk, registeredNode, newNode);
            if (pathCost > 0){
                System.out.println("adding links from point " + newNode + " to "+nodes.size()+" nodes");
                addLink(getNode(registeredNode), getNode(newNode), pathCost);
                //addLink(point, samplePoint, pathCost);    //?
            }
        }

       }

    public static void buildGraph(RLWorldChunk chunk){
        for (AdaptivePathNode node1: nodes){
            for (AdaptivePathNode node2: nodes){
                if (node1.point.equals(node2.point)){
                    return;
                }

                int pathCost = tracePathLength(chunk, node1.point, node2.point);
                if (pathCost > 0){
                    System.out.println("adding links from point " + node1 + " to "+node2);
                    addLink(node1, node2, pathCost);
                    addLink(node2, node1, pathCost);    //?
                }
            }
        }
    }

    /*
        Trace distance between two milestones, return -1 if obstacle is blocking direct line movement from ms 1 to ns 2
     */
    private static int tracePathLength(RLWorldChunk chunk, Point samplePoint, Point point) {
        //return false;  //To change body of created methods use File | Settings | File Templates.
        List<Point> line = BresinhamLine.line(samplePoint.getX(), samplePoint.getY(), point.getX(), point.getY());
        System.out.println("tracing bresingam line of size "+line.size());
        int i = 0;
        for (Point step: line){
            i++;
            RLTile rlTile = (RLTile)chunk.tile_data.get(step);
            if (rlTile.isWall()){
                System.out.println("debug: bresinham collision on step #"+i);
                return -1;
            }
        }

        return line.size();
    }
    
    public static void calculateAdaptiveRoutes(Point from){

        AdaptivePathNode source = getNode(from);
        source.minDistance = 0;

        PriorityQueue<AdaptivePathNode> vertexQueue = new PriorityQueue<AdaptivePathNode>();
        vertexQueue.add(source);

        while (!vertexQueue.isEmpty()) {
            AdaptivePathNode u = vertexQueue.poll();

            //v?
            //d?

            for (AdaptivePath e : u.nb)
            {
                AdaptivePathNode v = e.to;
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

    public static List<AdaptivePathNode> getShortestPathTo(Point target){

        AdaptivePathNode targetNode = getNode(target);

        List<AdaptivePathNode> path = new ArrayList<AdaptivePathNode>();
        for (AdaptivePathNode vertex = targetNode; vertex != null; vertex = vertex.prev){
            path.add(vertex);
        }
        Collections.reverse(path);
        return path;
    }

    public static void resetState() {
        for (AdaptivePathNode node: nodes){
            node.minDistance = Double.POSITIVE_INFINITY;
            node.prev = null;
        }
    }

    /*
             public static void main(String[] args){
                Graph<Integer> list = new Graph<Integer>();
                Edge<Integer> i = new Edge<Integer>(1, 0);
                Edge<Integer> j = new Edge<Integer>(2, 0);
                Edge<Integer> k = new Edge<Integer>(3, 0);
                Edge<Integer> l = new Edge<Integer>(4, 0);
                Edge<Integer> m = new Edge<Integer>(5, 0);
                Edge<Integer> n = new Edge<Integer>(6, 0);

                list.addEdge(i);
                list.addEdge(j);
                list.addEdge(k);
                list.addEdge(l);
                list.addEdge(m);
                list.addEdge(n);

                i.connectTo(j, 7);
                i.connectTo(k, 9);
                i.connectTo(n, 14);

                j.connectTo(k, 10);
                j.connectTo(l, 15);

                k.connectTo(n, 2);
                k.connectTo(l, 11);

                l.connectTo(m, 6);
                n.connectTo(m, 9);

                Dijkstra<Integer> test = new Dijkstra<Integer>(list);

            }
        }
     */
}
