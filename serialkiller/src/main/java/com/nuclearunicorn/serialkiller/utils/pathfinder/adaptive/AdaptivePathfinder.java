package com.nuclearunicorn.serialkiller.utils.pathfinder.adaptive;

import com.nuclearunicorn.serialkiller.game.world.RLTile;
import com.nuclearunicorn.serialkiller.game.world.RLWorldChunk;
import com.nuclearunicorn.serialkiller.game.world.entities.EntityTree;
import org.lwjgl.util.Point;

import java.util.*;

/**
 */
public class AdaptivePathfinder {

    public static List<AdaptiveNode> nodes = new ArrayList<AdaptiveNode>(32);
    public static List<AdaptivePath> edges = new ArrayList<AdaptivePath>(16);

    private static AdaptiveNode getNode(Point point){
        AdaptiveNode existing = findNode(point);
        if (existing != null){
            return existing;
        }
        AdaptiveNode node = new AdaptiveNode(point);
        nodes.add(node);
        return node;
    }

    /**
     * Look a milestone up in the graph without inventing it.
     *
     * <p>Asking a question used to answer it: {@link #getNode} is how routing found both ends
     * of a trip, so a milestone the graph builder never linked was silently added as an
     * island the moment someone tried to route through it. Dijkstra then had nothing to
     * relax, and the caller got back a "path" that did not start where it asked.
     */
    private static AdaptiveNode findNode(Point point){
        for (AdaptiveNode node : nodes){
            if (node.isNodeOf(point)){
                return node;
            }
        }
        return null;
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
        int best = -1;
        for (Point from: approaches(chunk, samplePoint)){
            for (Point to: approaches(chunk, point)){
                int cost = traceLine(chunk, from, to);
                if (cost < 0){
                    continue;
                }
                //the step in and out of the doorway, where the trace did not start on the milestone
                cost += (from.equals(samplePoint) ? 0 : 1) + (to.equals(point) ? 0 : 1);
                if (best < 0 || cost < best){
                    best = cost;
                }
            }
        }
        return best;
    }

    /**
     * Where an edge to this milestone may start. Normally the milestone itself — but for one
     * standing in a doorway, the open tiles either side of it.
     *
     * <p>A door is a hole in a wall, so every straight line out of it that is not straight
     * through it runs along the masonry it is cut into. Traced from its own tile, a front
     * door therefore reaches no other milestone at all and never enters the graph — and
     * because it is nonetheless the <i>nearest</i> milestone to anyone standing at that door,
     * every trip they plan starts or ends on a node that has no edges. That is one house per
     * town whose occupants can route nowhere.
     */
    private static List<Point> approaches(RLWorldChunk chunk, Point milestone) {
        RLTile tile = (RLTile)chunk.tile_data.get(milestone);
        if (tile == null || !tile.isWallGap()){
            return Collections.singletonList(milestone);
        }
        List<Point> open = new ArrayList<Point>(2);
        for (int[] dir: new int[][]{{1,0},{-1,0},{0,1},{0,-1}}){
            Point side = new Point(milestone.getX() + dir[0], milestone.getY() + dir[1]);
            if (!blocksEdge((RLTile)chunk.tile_data.get(side))){
                open.add(side);
            }
        }
        //a doorway with nothing open beside it is a bricked-up door; trace it as any other tile
        return open.isEmpty() ? Collections.singletonList(milestone) : open;
    }

    /** Length of the straight line between two tiles, or -1 if masonry stands on it. */
    private static int traceLine(RLWorldChunk chunk, Point from, Point to) {
        List<Point> line = BresinhamLine.line(from.getX(), from.getY(), to.getX(), to.getY());
        for (Point step: line){
            if (blocksEdge((RLTile)chunk.tile_data.get(step))){
                return -1;
            }
        }
        return line.size();
    }

    /**
     * Deliberately coarse: only masonry and trees break an edge. A lamppost or a crate on the
     * line is routed around per-segment by RLController.calculateAdaptivePath - rejecting the
     * edge over one instead shreds the road graph and strands NPCs.
     */
    private static boolean blocksEdge(RLTile tile) {
        return tile == null || tile.isWall() || tile.has_ent(EntityTree.class);
    }

    public static void calculateAdaptiveRoutes(Point from){

        AdaptiveNode source = findNode(from);
        if (source == null){
            return;     //not a milestone of this graph: every target stays unreachable
        }
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

    /**
     * The milestone route to a target, after {@link #calculateAdaptiveRoutes}, or an empty
     * list if there is none.
     *
     * <p>Emptiness is the only honest answer to "unreachable". Walking the {@code prev} chain
     * of a node Dijkstra never relaxed yields the node on its own — a one-element list that
     * looks exactly like "you are already there", except it names the far end of the trip
     * rather than the near one. Spliced into a route by the caller it becomes a straight line
     * across town, through every wall on the diagonal.
     */
    public static List<AdaptiveNode> getShortestPathTo(Point target){

        AdaptiveNode targetNode = findNode(target);
        if (targetNode == null || targetNode.minDistance == Double.POSITIVE_INFINITY){
            return Collections.emptyList();
        }

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

    public static void reset() {
        nodes.clear();
        edges.clear();
    }
}
