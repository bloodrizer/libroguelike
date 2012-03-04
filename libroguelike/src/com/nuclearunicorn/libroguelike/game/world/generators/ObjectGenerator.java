/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.game.world.generators;

import com.nuclearunicorn.libroguelike.game.GameEnvironment;
import com.nuclearunicorn.libroguelike.game.world.WorldTile;

import java.util.Random;

/**
 *
 * @author Administrator
 */
public abstract class ObjectGenerator {
    protected GameEnvironment environment;
    
    public void setEnvironment(GameEnvironment environment){
        this.environment = environment;
    }

    public void generate_object(int x, int y, WorldTile tile, Random chunk_random){
        
    }
}
