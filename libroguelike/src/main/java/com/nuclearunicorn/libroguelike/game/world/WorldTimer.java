/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.game.world;

import com.nuclearunicorn.libroguelike.utils.Rng;

import java.util.Calendar;

/**
 *
 * @author Administrator
 */
public class WorldTimer {

    /** When the sun is up. The light field, the AI schedules and the HUD clock all use these. */
    public static final float DAWN = 7.0f;
    public static final float DUSK = 21.0f;

    public static final Calendar datetime = Calendar.getInstance();
    static {
        setTime(System.getProperty("lrl.time", "21:00"));
    }

    /** {@code -Dlrl.time=HH} or {@code HH:MM} — open the world at that hour, for shots and tests. */
    public static void setTime(String hhmm){
        String[] parts = hhmm.trim().split(":");
        datetime.set(Calendar.HOUR_OF_DAY, Integer.parseInt(parts[0]));
        datetime.set(Calendar.MINUTE, parts.length > 1 ? Integer.parseInt(parts[1]) : 0);
        datetime.set(Calendar.SECOND, 0);
    }


    public static void tick(){
        //datetime.add(Calendar.SECOND,15);
        datetime.add(Calendar.MINUTE, 1);

        if(datetime.get(Calendar.MINUTE) == 0 && datetime.get(Calendar.SECOND) == 0){
            e_on_new_hour();
        }
    }

    /** Time of day as a fraction of an hour: 13:30 is 13.5. */
    public static float hourOfDay(){
        return datetime.get(Calendar.HOUR_OF_DAY) + datetime.get(Calendar.MINUTE)/60.0f;
    }

    public static float get_light_amt(){

        float hour = hourOfDay();
        float amt = 1.0f;

        if (hour < DAWN || hour >= DUSK){
            amt = 0.0f;
        }
        if ( hour >= DAWN && hour <= 10  ) {
            amt = (hour-DAWN)/3.0f;
        }
        if ( hour >= 17 && hour < DUSK){
            amt = (DUSK-hour)/5.0f;
        }
   
        //amt = amt/2.0f;

        return amt;
    }

    public static boolean is_night(){
       float hour = hourOfDay();
       return (hour < DAWN || hour >= DUSK);
    }

    private static void e_on_new_hour() {
        /*if (is_night()){
            //there is slight chance of spawning zombie each hour

            //TODO: check if camera is not centered on this area and spawn a zombie
            //if !(WorldCamera.tile_in_fov()){ //etc

            int chance = (int)(Rng.random(Rng.WORLD)*100);
            if(chance < 90 && Player.get_ent() != null){
                
                Point spawn_point = new Point(
                        Player.get_ent().origin.getX() + (int)(Rng.random(Rng.WORLD)*60-30),
                        Player.get_ent().origin.getY() +(int)(Rng.random(Rng.WORLD)*60-30));
                
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

