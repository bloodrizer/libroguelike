/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nuclearunicorn.libroguelike.render.layers;

import com.nuclearunicorn.libroguelike.game.world.WorldTile;

/**
 *
 * @author bloodrizer
 */
public abstract class LayerRenderer {
    public abstract void render_tile(WorldTile tile, int tile_x, int tile_y);

    public abstract void beforeRender();
    public abstract void afterRender();
}
