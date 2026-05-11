/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.game.world;

import com.nuclearunicorn.libroguelike.utils.Noise;
import org.lwjgl.util.Point;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;


/**
 *
 * @author Administrator
 */
public class Terrain {
    public static Noise noise = new Noise();
    static {
        noise.noiseSeed(123456);
    }

    public static final int TERRAIN_HEIGHT = 255;    //255

    public static void setup(){
        //noise.noiseDetail(2,0.5f); //<<LOOKS BEST
        noise.noiseDetail(2,0.9f); //<<nice height
        //noise.noiseDetail(3,0.5f);

    }

    static Point utl_point = new Point(0,0);

    //function generates virtual terrain height, based on PerlinNoise Map
    public static int get_height(int x, int y){

        //int val;

        //speeds up biome generation for ~300%

        utl_point.setLocation(x, y);
        Integer val = heightmap_cached.get(utl_point);
        if( val != null){
            return val;
        }

        //todo: call setup();
        noise.noiseDetail(2,0.9f);

        int x_offset = 2000;
        int y_offset = 2000;

        float noiseScale = 0.03f;
        float noiseVal = noise.noise((float)(x+x_offset)*noiseScale,(float)(y+y_offset)*noiseScale);

        //System.out.println(noiseVal);


        val = (int)(noiseVal*TERRAIN_HEIGHT);
        heightmap_cached.put(new Point(x,y), val);
        return val;
    }

    /*
     * This method calculates the biome type
     *
     */

    public static HashMap<Point,Integer> heightmap_cached = new HashMap<Point,Integer>(512);
    public static Collection<Point> aquatic_tiles = new ArrayList<Point>(128);

    public static final float MOST_AMT = 10.0f;

/*
 * This function calculates moisture map in the 32x32 chunk based on 96x96 tile sample
 *
 * To speed up calculation process, this function only uses aquatic type tiles as samples, which fastes
 * iteration process up to 30-40 times
 *
 */
    public static float get_moisture(int x, int y){
        float moistVal = 99999.0f;

        Point[] __aquatic_tiles = aquatic_tiles.toArray(new Point[0]);
        for (int i = 0; i < __aquatic_tiles.length; i++){
            Point tile = __aquatic_tiles[i];
            int dx = x - tile.getX();
            int dy = y - tile.getY();

            //float disst = (float)Math.sqrt(dx*dx+dy*dy);
            float disst = dx*dx+dy*dy;

            float amt =  disst;
            if ( amt < moistVal ){
                moistVal = amt;
            }
        }
        moistVal = (float)Math.sqrt(moistVal);
        moistVal = (float)Math.pow(0.982f, moistVal);


        return moistVal;
    }

    public static float getHumidity(int x, int y){
        noise.noiseDetail(4, 2.221312f);

        int x_offset = 2000;
        int y_offset = 2000;

        float noiseScale = 0.003f;
        float humVal = noise.noise((float)(x+x_offset)*noiseScale,(float)(y+y_offset)*noiseScale);

        return humVal;
    }


    public static int FORREST_HEIGHT = 120;
    public static int LAKE_HEIGHT = 120;
    public static int TREE_RATE = 10;

    public static boolean is_forrest(WorldTile tile){
        if ( tile.terrain_type == WorldTile.TerrainType.TERRAIN_WATER){
            return false;
        }

        switch(tile.biome_type){
            case BIOME_TROPICAL_RAINFOREST:
                return true;

            case BIOME_SEASONAL_FOREST:
                return true;

            case BIOME_DECIDUOS_FOREST:
                return true;

            case BIOME_TEMP_RAINFOREST:
                return true;
        }
        return false;
    }

    public static boolean is_tree(float random, WorldTile tile){

        if (!is_forrest(tile)){
            return false;
        }

        int chance = (int)Math.round(
                random* tile.get_height()
        );
        if (chance < TREE_RATE){
            return true;
        }

        return false;
    }

    public static boolean is_lake(WorldTile tile){
        return is_lake(tile.get_height());
    }

    public static boolean is_lake(int height){
        return height < 60;
    }

}
