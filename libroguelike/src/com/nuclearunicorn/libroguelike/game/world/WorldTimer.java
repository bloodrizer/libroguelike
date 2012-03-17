/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.game.world;

import java.util.Calendar;

/**
 *
 * @author Administrator
 */
public class WorldTimer {

    public static final Calendar datetime = Calendar.getInstance();
    static {
        //java.util.Timer timer = new java.util.Timer();
        datetime.set(Calendar.HOUR_OF_DAY, 7);
        datetime.set(Calendar.SECOND, 0);
    }


    public static void tick(){
        //datetime.add(Calendar.SECOND,15);
        datetime.add(Calendar.MINUTE, 1);

        if(datetime.get(Calendar.MINUTE) == 0 && datetime.get(Calendar.SECOND) == 0){
            e_on_new_hour();
        }
    }

    public static float get_light_amt(){

        float hour = datetime.get(Calendar.HOUR_OF_DAY) + datetime.get(Calendar.MINUTE)/60.0f;
        float amt = 1.0f;

        if (hour < 7 || hour >= 21){
            amt = 0.0f;
        }
        if ( hour >=7 && hour <= 10  ) {
            amt = (hour-7)/3.0f;
        }
        if ( hour >= 17 && hour < 21){
            amt = (21.0f-hour)/5.0f;
        }
   
        //amt = amt/2.0f;

        return amt;
    }

    public static boolean is_night(){
       float hour = datetime.get(Calendar.HOUR_OF_DAY) + datetime.get(Calendar.MINUTE)/60.0f;
       return (hour < 7 || hour >= 21);
    }

    private static void e_on_new_hour() {
        /*if (is_night()){
            //there is slight chance of spawning zombie each hour

            //TODO: check if camera is not centered on this area and spawn a zombie
            //if !(WorldCamera.tile_in_fov()){ //etc

            int chance = (int)(Math.random()*100);
            if(chance < 90 && Player.get_ent() != null){
                
                Point spawn_point = new Point(
                        Player.get_ent().origin.getX() + (int)(Math.random()*60-30),
                        Player.get_ent().origin.getY() +(int)(Math.random()*60-30));
                
                //do not allow zombie to spawn outside of the player cluster or in the camera rect
                if (WorldCluster.tile_in_cluster(spawn_point.getX(), spawn_point.getY())){
                    if (!WorldViewCamera.tile_in_fov(spawn_point)){
                        return;
                    }
                }else{
                    return;
                }
                
                WorldTile tile = ClientGameEnvironment.getWorldLayer(Player.get_zindex()).get_tile(spawn_point.getX(), spawn_point.getY());
                if (tile ==null ||  tile.light_level > 0.5f){
                    return;
                    //todo: change so we would not waste our spawn chance
                }
            }
        }*/
    }

    public void stop_timer() {
        //timer.cancel();
    }

}

