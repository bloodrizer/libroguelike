/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nuclearunicorn.libroguelike.game.world.generators;

import com.nuclearunicorn.libroguelike.game.GameEnvironment;
import com.nuclearunicorn.libroguelike.game.world.layers.WorldLayer;
import org.lwjgl.util.Point;



/**
 *
 * @author bloodrizer
 */
public abstract class ChunkGenerator {

    protected int z_index;
    protected GameEnvironment environment;

    public void set_zindex(int z_index){
        this.z_index = z_index;
    }

    public void setEnvironment(GameEnvironment environment){
        this.environment = environment;
    }

    protected WorldLayer getLayer() {
        return environment.getWorldLayer(z_index);
    }
    
    public void generate(Point origin){
    }
}
