/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.game.world.generators;

import com.nuclearunicorn.libroguelike.game.world.WorldTile;
import java.util.Random;

/**
 *
 * @author Administrator
 */

public class ChestGenerator extends ObjectGenerator {

      public void generate_object(int x, int y, WorldTile tile, Random chunk_random){
        float rate = 0.01f;

        
        float chance = (float)(chunk_random.nextFloat()*100);
        if (chance<rate){
           add_chest(x,y);
        }
    }

    public static void add_chest(int i, int j){
        /*EntChest chest = new EntChest();
        chest.setLayerId(WorldLayer.GROUND_LAYER);
        chest.spawn(new Point(i,j));

        chest.set_blocking(true);    //obstacle


        //chest loot
        int chance = (int)(Math.random()*100.0f);
        if (chance<70){
            int count = (int)(Math.random()*15.0f);
            chest.container.add_item(
                    BaseItem.produce("copper_coin", count)
            );
        }*/
    }

}
