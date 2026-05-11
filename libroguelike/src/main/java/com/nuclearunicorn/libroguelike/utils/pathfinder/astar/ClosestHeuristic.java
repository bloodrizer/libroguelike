/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.utils.pathfinder.astar;

/**
 *
 * @author Administrator
 */
/**
 * A heuristic that uses the tile that is closest to the target
 * as the next best tile.
 *
 * @author Kevin Glass
 */
public class ClosestHeuristic implements AStarHeuristic {
	/**
	 * @see AStarHeuristic#getCost(TileBasedMap, Mover, int, int, int, int)
	 */
	public float getCost(TileBasedMap map, Mover mover, int x, int y, int tx, int ty) {
		float dx = tx - x;
		float dy = ty - y;

		//float result = (float) (Math.sqrt((dx*dx)+(dy*dy)));
        //use Mathattan distance instead of sqrt
        float result = map.getScaleFactor() * (Math.abs(x-tx) + Math.abs(y-ty));

		return result;
	}

}