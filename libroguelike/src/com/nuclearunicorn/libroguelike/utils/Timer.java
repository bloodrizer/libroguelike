/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.utils;

import java.util.Date;

/**
 *
 * @author Administrator
 */
public class Timer {
    private static long time = 0;
    private static int last_fps = 0;
    private static int fps = 0;

    private static long last_tick = 0;

    public static void init(){
        tick();
    }
    
    public static void tick(){
        //time += 1;
        time = new Date().getTime();
        long time_diff = time - last_tick;

        if (time_diff >= 1000){
            //fps = (int)(1000 / time_diff);
            last_fps = fps;
            fps = 0;
            last_tick = time;
        }
        fps++;
       
    }

    public static long get_time(){
        return time;
    }

    public static int get_fps(){
        return last_fps;
    }
}
