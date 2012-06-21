/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nuclearunicorn.libroguelike.render.layers;

import com.nuclearunicorn.libroguelike.game.world.WorldTile;
import com.nuclearunicorn.libroguelike.game.world.WorldTimer;
import com.nuclearunicorn.libroguelike.game.world.WorldView;
import com.nuclearunicorn.libroguelike.render.TilesetRenderer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author bloodrizer
 */
public class GroundLayerRenderer extends LayerRenderer{
    
    public TilesetRenderer bg_tileset_renderer;
    TilesetRenderer tileSprite;
    
    String textureName = "/resources/terrain/grassland.png";

    public GroundLayerRenderer(){
        bg_tileset_renderer = new TilesetRenderer();
        bg_tileset_renderer.texture_name = "terrain_sprites.png";
        
        tileSprite = new TilesetRenderer();
        tileSprite.texture_name = textureName;
        tileSprite.sprite_w = 64;
        tileSprite.sprite_h = 68;
        tileSprite.TILESET_W = 1;
        tileSprite.TILESET_H = 1;
    }

    @Override
    public void render_tile(WorldTile tile, int tile_x, int tile_y) {
        if (tile != null){

            //lil hack for terrain rendering visualization
            if (tile.terrain_type != WorldTile.TerrainType.TERRAIN_WATER){
                Vector3f tile_color = get_tile_color(tile);
                GL11.glColor3f(tile_color.x,tile_color.y,tile_color.z);
            }else{
                GL11.glColor3f(1.0f,1.0f,1.0f);
            }

            render_bg_tile(tile_x, tile_y, tile);
       }
    }

    @Override
    public void beforeRender() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void afterRender() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    static Vector3f utl_tile_color = new Vector3f();
    public static Vector3f get_tile_color(WorldTile tile){


        float grass_r = (1.0f - (tile.moisture / 5.0f)) * 0.5f ;

        float lightLevelMtp = ((float) WorldView.getYOffset(tile)/96.0f)/5.0f;

        utl_tile_color.set(
                0.3f + grass_r + tile.light_level - lightLevelMtp   + WorldTimer.get_light_amt(),
                0.5f - grass_r/2.0f  + tile.light_level - lightLevelMtp   + WorldTimer.get_light_amt(),
                0.5f  + tile.light_level - lightLevelMtp     + WorldTimer.get_light_amt()
        );

        if (utl_tile_color.getY() > 0.6f){
            utl_tile_color.setY(0.6f);
        }

        return utl_tile_color;
    }
    
    private void render_bg_tile(int i, int j, WorldTile tile) {

        if (i == 0 || j == 0){
            tileSprite.texture_name = "/render/terrain/grid_debug.png";
        }else{
            tileSprite.texture_name = textureName;
        }

        if (tile.isBlocked()){
            GL11.glColor3f(0.1f,0.1f,0.1f);
        }

        tileSprite.render_sprite(i, j, 1, 0, WorldView.getYOffset(tile)+32);    //replace 32 with actual magic constant based on tile sprite height

    }
    
}
