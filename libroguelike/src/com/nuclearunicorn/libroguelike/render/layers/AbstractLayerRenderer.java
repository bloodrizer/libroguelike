/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nuclearunicorn.libroguelike.render.layers;

import com.nuclearunicorn.libroguelike.game.world.WorldChunk;
import com.nuclearunicorn.libroguelike.game.world.layers.WorldLayer;

/**
 *
 * @author bloodrizer
 */
public abstract class AbstractLayerRenderer {
    public abstract void renderChunk(WorldLayer layer, WorldChunk chunk, int i, int j);

    public abstract void beforeRender();
    public abstract void afterRender();
}
