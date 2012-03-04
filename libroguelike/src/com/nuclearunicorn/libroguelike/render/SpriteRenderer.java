/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.render;

import com.nuclearunicorn.libroguelike.game.world.WorldView;

import static org.lwjgl.opengl.GL11.*;

/**
 *
 * @Simple simple sprite renderer for inanimated objects like trees / constructions
 */
public class SpriteRenderer extends EntityRenderer {

    public TilesetRenderer tileset = null;
    protected int tile_id = 0;

    public SpriteRenderer(){
        tileset = new TilesetRenderer();
    }

    public TilesetRenderer get_tileset(){
        return tileset;
    }

    public void set_texture(String name){
        tileset.texture_name = name;
    }

    public void set_tile_id(int tile_id){
        this.tile_id = tile_id;
    }

    @Override
    public void render(){

        glEnable (GL_BLEND);
        glBlendFunc (GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        glTexParameteri(
                GL_TEXTURE_2D, 
                GL_TEXTURE_MAG_FILTER,
                GL_NEAREST);
        
        glTexParameteri(
                GL_TEXTURE_2D,
                GL_TEXTURE_MIN_FILTER,
                GL_NEAREST);

        tileset.render_sprite(
            ent.origin.getX(),
            ent.origin.getY(),
            tile_id,
            0,
            WorldView.getYOffset(ent.tile)
        );
    }
}
