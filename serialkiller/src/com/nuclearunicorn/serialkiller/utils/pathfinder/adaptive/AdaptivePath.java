package com.nuclearunicorn.serialkiller.utils.pathfinder.adaptive;

/**

 */
public class AdaptivePath {
    final public AdaptiveNode from;
    final public AdaptiveNode to;
    final public int cost;

    public AdaptivePath(AdaptiveNode from, AdaptiveNode to, int cost) {
        this.from = from;
        this.to = to;
        this.cost = cost;
    }
}
