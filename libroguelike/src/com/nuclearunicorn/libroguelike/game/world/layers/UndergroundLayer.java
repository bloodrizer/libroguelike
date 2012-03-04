/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.game.world.layers;

import com.nuclearunicorn.libroguelike.game.world.generators.ChunkGenerator;
import com.nuclearunicorn.libroguelike.game.world.generators.ChunkUndergroundGenerator;
import org.lwjgl.util.Point;

/**
 *
 * @author Administrator
 */
public class UndergroundLayer extends WorldLayer{

    protected void build_chunk(Point origin, int z_index){
        ChunkGenerator underground_gen = new ChunkUndergroundGenerator();
        underground_gen.set_zindex(z_index);

        underground_gen.generate(origin);

        terrain_outdated = true;
    }

}
