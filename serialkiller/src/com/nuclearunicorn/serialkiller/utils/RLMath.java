package com.nuclearunicorn.serialkiller.utils;

import com.nuclearunicorn.libroguelike.core.client.ClientGameEnvironment;
import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.game.player.Player;
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
            Double disst = Math.pow(anchor.getX()-node.getX(),2) + Math.pow(anchor.getY()-node.getY(),2);
            if (disst < min_disst){
                min_disst = disst;
                nearestPoint = node;
            }
        }

        return nearestPoint;
    }
    
    public static List<Entity> getEntitiesInRadius(Point center, int radius){
        List<Entity> entities = new ArrayList<Entity> ();
        
        for (Entity ent: ClientGameEnvironment.getEnvironment().getEntityManager().getEntities(Player.get_zindex())){
            double squareDisst = Math.pow(center.getX() - ent.origin.getX(), 2) + Math.pow(center.getY() - ent.origin.getY(), 2);
            if (squareDisst <= Math.pow(radius, 2)){
                entities.add(ent);
            }
        }

        return entities;
    }
}
