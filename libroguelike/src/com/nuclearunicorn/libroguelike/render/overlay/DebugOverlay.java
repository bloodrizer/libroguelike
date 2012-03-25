/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.render.overlay;

import com.nuclearunicorn.libroguelike.core.Input;
import com.nuclearunicorn.libroguelike.core.client.ClientGameEnvironment;
import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.game.ent.controller.IEntityController;
import com.nuclearunicorn.libroguelike.game.ent.controller.NpcController;
import com.nuclearunicorn.libroguelike.game.player.Player;
import com.nuclearunicorn.libroguelike.game.world.WorldTimer;
import com.nuclearunicorn.libroguelike.game.world.WorldView;
import com.nuclearunicorn.libroguelike.game.world.WorldViewCamera;
import com.nuclearunicorn.libroguelike.render.WindowRender;
import com.nuclearunicorn.libroguelike.utils.Timer;
import org.lwjgl.util.Point;
import org.newdawn.slick.Color;


public class DebugOverlay {

    public static float frameTime;
    public static float updateTime;
    public static float renderTime;

    public static void render(){
        long total = Runtime.getRuntime().totalMemory();
        long free  = Runtime.getRuntime().freeMemory();

        OverlaySystem.ttf.drawString(10, 5,
                "Memory: " +
                    String.format("%.2f", free /(1024.0f*1024)
                    )+
                "MB free of " +
                    Float.toString( total/(1024.0f*1024) ) +
                "MB"
        , Color.white);

        String timePostifx = "";
        if (WorldTimer.is_night()){
            timePostifx = " (night)";
        }
        OverlaySystem.ttf.drawString(10, 25, "time: "
                    + WorldTimer.datetime.getTime() + timePostifx
            , Color.white);

        
        
        //-----------------------------------

        OverlaySystem.ttf.drawString(10, 70, "FPS: " + Integer.toString( Timer.get_fps() ), Color.white);


        if (Input.key_state_alt) {

        //debug camera shit

            OverlaySystem.ttf.drawString(10, 90, "Camera @: " +
                (int) WorldViewCamera.camera_x +
                "," + 
                (int)WorldViewCamera.camera_y +
                " - " + WorldViewCamera.target.toString() +
                "tile @:"
                ,Color.white);
        
        //debug render shit

            OverlaySystem.ttf.drawString(10, 110, "Render profile:");
            OverlaySystem.ttf.drawString(10, 130, "Avg frame:    " + frameTime + "ms");
            OverlaySystem.ttf.drawString(10, 150, "Update calls: " + updateTime + "ms");
            OverlaySystem.ttf.drawString(10, 170, "Render calls: " + renderTime + "ms");

            OverlaySystem.ttf.drawString(WindowRender.get_window_w()-100 , 10, "z-index: " + WorldView.get_zindex(), Color.white);
        }

    }



    public static void debugPathfinding() {
        if (!Input.key_state_alt){
            return;
        }


        Point tileFrom = new Point(0,0);
        Point tileTo = new Point(0,0);

        Entity[] entList = ClientGameEnvironment.getEntityManager().getEntities(WorldView.get_zindex());
        for (Entity ent: entList){
            IEntityController controller = ent.controller;
            //SO FAR WE ONLY DEBUG PLAYER
            if (controller != null && controller instanceof NpcController){
                NpcController npc_controller = (NpcController)controller;

                if (npc_controller.path == null){
                    continue;
                }
                Point prevStep = new Point(ent.origin.getX(), ent.origin.getY());

                for (int i=0; i<npc_controller.path.size();i++){

                    Point step = (Point)npc_controller.path.get(i);

                    tileFrom.setLocation(prevStep);
                    if (i>0){
                        //root step is entity origin, which is allready in world coord system. So we do not recalculdate it.
                        tileFrom = ClientGameEnvironment.getWorldLayer(WorldView.get_zindex()).tile_map.local2world(tileFrom);
                    }

                    tileTo.setLocation(step);
                    tileTo = ClientGameEnvironment.getWorldLayer(WorldView.get_zindex()).tile_map.local2world(tileTo);

                    if (ent == Player.get_ent()){
                        OverlaySystem.drawLine(tileFrom, tileTo, Color.red);
                    }else{
                        OverlaySystem.drawLine(tileFrom, tileTo, Color.green);
                    }

                    prevStep = step;
                }
            }
        }
    }

}
