package com.nuclearunicorn.serialkiller.utils;

import com.nuclearunicorn.libroguelike.core.client.ClientGameEnvironment;
import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.game.player.Player;
import com.nuclearunicorn.libroguelike.game.world.layers.WorldLayer;
import com.nuclearunicorn.serialkiller.utils.pathfinder.adaptive.BresinhamLine;
import org.lwjgl.util.Point;

import java.util.ArrayList;
import java.util.List;

/**

 */
public class RLMath {
    public static Point getNearestPoint(List<Point> list, Point anchor){

        double min_disst = Double.MAX_VALUE;
        Point nearestPoint = null;
        for (Point node : list){
            /*if (node == null || anchor == null){
                return null;    //unexpected behavior, panic flee
            }*/
            Double disst = Math.pow(anchor.getX()-node.getX(),2) + Math.pow(anchor.getY()-node.getY(),2);
            if (disst < min_disst){
                min_disst = disst;
                nearestPoint = node;
            }
        }

        return nearestPoint;
    }
    
    public static boolean isPointInRadius(Point center, Point target, int radius){
        double squareDisst = Math.pow(center.getX() - target.getX(), 2) + Math.pow(center.getY() - target.getY(), 2);
        return (squareDisst <= Math.pow(radius, 2));
    }
    
    public static List<Entity> getEntitiesInRadius(Point center, int radius){
        List<Entity> entities = new ArrayList<Entity> ();
        
        for (Entity ent: ClientGameEnvironment.getEnvironment().getEntityManager().getEntities(Player.get_zindex())){
            if (isPointInRadius(center, ent.origin, radius)){
                entities.add(ent);
            }
        }

        return entities;
    }

    //Trace bresinham vector and return false, if collision in LOS or outside of visibility radius
    public static boolean pointInLOS(Point origin, Point target, int radius) {
        if (!isPointInRadius(origin, target, radius)){
            return false;
        }

        WorldLayer layer = ClientGameEnvironment.getEnvironment().getWorldLayer(Player.get_zindex());
        List<Point> line = BresinhamLine.line(origin.getX(), origin.getY(), target.getX(), target.getY());
        for (Point tile: line){
            if (layer.get_tile(tile).isBlocked()){
                return false;
            }
        }
        
        return true;
    }
}
