package com.nuclearunicorn.serialkiller.utils.pathfinder.adaptive;

/**

 */
public class AdaptivePath {
    final public AdaptivePathNode from;
    final public AdaptivePathNode to;
    final public int cost;

    public AdaptivePath(AdaptivePathNode from, AdaptivePathNode to, int cost) {
        this.from = from;
        this.to = to;
        this.cost = cost;
    }
}
