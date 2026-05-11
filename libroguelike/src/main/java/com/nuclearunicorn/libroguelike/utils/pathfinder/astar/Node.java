package com.nuclearunicorn.libroguelike.utils.pathfinder.astar;

/**

 */

    public class Node implements Comparable {

        public int x;
        public int y;

        public float cost;   //cost
        public Node parent;
        public float heuristic;    //heuristic
        public int depth;  //expanding counter

        /**
         * Create a new node
         *
         * @param x The x coordinate of the node
         * @param y The y coordinate of the node
         */
        public Node(int x, int y) {
            this.x = x;
            this.y = y;
        }

        /**
         * Set the parent of this node
         *
         * @param parent The parent node which lead us to this node
         * @return The depth we have no reached in searching
         */
        public int setParent(Node parent) {
            depth = parent.depth + 1;
            this.parent = parent;

            return depth;
        }

        /**
         * @see Comparable#compareTo(Object)
         */
        public int compareTo(Object other) {
            Node o = (Node) other;

            float f = this.heuristic + cost;
            float of = o.heuristic + o.cost;

            if (f < of) {
                return -1;
            } else if (f > of) {
                return 1;
            } else {
                return 0;
            }
    }
}
