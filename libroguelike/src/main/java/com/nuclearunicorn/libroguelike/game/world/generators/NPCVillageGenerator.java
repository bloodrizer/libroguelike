/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.game.world.generators;

import com.nuclearunicorn.libroguelike.utils.Rng;

import com.nuclearunicorn.libroguelike.game.world.WorldChunk;

import java.util.Random;

/**
 *
 * @author Administrator
 */
public class NPCVillageGenerator extends ChunkGenerator {
    @Override
    public void generate(WorldChunk chunk){
        //NLTimer.push();

        Random chunk_random = Rng.derive(Rng.WORLDGEN);
        chunk_random.setSeed(chunk.origin.getX()*1000+chunk.origin.getY());    //set chunk-specific seed
    }
}
