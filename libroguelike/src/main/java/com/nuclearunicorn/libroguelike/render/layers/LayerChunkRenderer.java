package com.nuclearunicorn.libroguelike.render.layers;

import com.nuclearunicorn.libroguelike.game.world.WorldChunk;
import com.nuclearunicorn.libroguelike.game.world.WorldTile;
import com.nuclearunicorn.libroguelike.game.world.layers.WorldLayer;

/**
 * Created with IntelliJ IDEA.
 * User: Administrator
 * Date: 22.06.12
 * Time: 20:28
 * To change this template use File | Settings | File Templates.
 */
public class LayerChunkRenderer extends AbstractLayerRenderer{


    @Override
    public void renderChunk(WorldLayer layer, WorldChunk chunk, int i, int j) {

        for (int x = i*WorldChunk.CHUNK_SIZE; x<(i+1)*WorldChunk.CHUNK_SIZE; x++)
            for (int y = j*WorldChunk.CHUNK_SIZE; y<(j+1)*WorldChunk.CHUNK_SIZE; y++)
            {
                //WorldTile tile = getLayer().getTile(currentChunk, i,j);
                WorldTile tile = layer.get_tile(x, y);

                this.render_tile(tile, x, y);
            }

    }

    protected void render_tile(WorldTile tile, int x, int y) {
        //do nothing
    }

    @Override
    public void beforeRender() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void afterRender() {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
