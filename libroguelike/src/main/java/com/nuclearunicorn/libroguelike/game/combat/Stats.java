/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.game.combat;

import java.util.HashMap;

/**
 * This is a wrapper for a character stats
 */
public class Stats {

    public static final String[] stats = {"str","per","end","chr","int","agi","luk"};
    public HashMap<String,Integer> stats_val = new HashMap<String,Integer>(6);

    public Stats(){
        for(int i=0; i< stats.length; i++){
            stats_val.put(stats[i], 5+(int)(Math.random()*5));
        }
    }

    public void set_stat(String stat, int val){
        stats_val.put(stat, val);
    }

    public int get_stat(String stat){
        return stats_val.get(stat);
    }
}
