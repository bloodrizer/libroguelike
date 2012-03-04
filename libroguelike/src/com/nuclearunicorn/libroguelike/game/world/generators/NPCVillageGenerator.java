/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.game.world.generators;

import java.util.Random;
import org.lwjgl.util.Point;

/**
 *
 * @author Administrator
 */
public class NPCVillageGenerator extends ChunkGenerator {
    @Override
    public void generate(Point origin){
        //NLTimer.push();

        Random chunk_random = new Random();
        chunk_random.setSeed(origin.getX()*1000+origin.getY());    //set chunk-specific seed
    }
}
