/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.render.overlay;

import com.nuclearunicorn.libroguelike.core.Input;
import com.nuclearunicorn.libroguelike.core.client.ClientGameEnvironment;
import com.nuclearunicorn.libroguelike.game.player.Player;
import com.nuclearunicorn.libroguelike.game.world.WorldChunk;
import com.nuclearunicorn.libroguelike.game.world.WorldTile;
import com.nuclearunicorn.libroguelike.game.world.WorldView;
import com.nuclearunicorn.libroguelike.render.WindowRender;
import org.lwjgl.input.Mouse;
import org.lwjgl.util.Point;
import org.newdawn.slick.Color;

/**
 *
 * @author Administrator
 */
public class TileCoordOverlay {
    public static void render(){

        if (!Input.key_state_alt){
            return;
        }

        int x = Mouse.getX();
        int y = Mouse.getY();


        Point tile_coord = WorldView.getTileCoord(x, y);
        Point chunk_coord = WorldChunk.get_chunk_coord(tile_coord);

        y = WindowRender.get_window_h() - y;

        OverlaySystem.ttf.drawString(x+20, y-10,
                "Mouse: ["+x+
                ","+y+
                "] - World: ["+
                    tile_coord.getX()+
                ","+tile_coord.getY()+
                "] - Chunk@: ["
                   +chunk_coord.getX()+
                ","+chunk_coord.getY()+
                "]"

        , Color.white);


        WorldTile tile = ClientGameEnvironment.getWorldLayer(Player.get_zindex()).get_tile(tile_coord.getX(), tile_coord.getY());

        if (tile != null){
            Object[] ent_list = tile.ent_list.toArray();
            OverlaySystem.ttf.drawString(x+20, y+10,
                    /*"entities:" + Integer.toString(ent_list.length) +
                    "blocked:" + Boolean.toString(tile.isBlocked()) +*/
                    "light level:" + tile.light_level +
                    " height:" + tile.get_height() + "("+tile.get_elevation_zone()+")" +
                    " moist:" + tile.moisture + "("+tile.get_moisture_zone()+")" +
                    " biome:" + tile.biome_type
            );
        }

    }
}
