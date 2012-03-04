/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nuclearunicorn.libroguelike.vgui;

import com.nuclearunicorn.libroguelike.game.world.WorldTile;
import com.nuclearunicorn.libroguelike.game.world.WorldView;

/*
 * NE_GUI_Proxy acts like a wrapper for simple access to the game world objects as gui elements
 * (for correct tooltip display, etc)
 */
public class NE_GUI_Proxy extends NE_GUI_Element {

    WorldTile tile = null;

    void set_tile(WorldTile tile) {
        this.tile = tile;

        this.x = WorldView.get_tile_x_screen(tile.origin);
        this.y = WorldView.get_tile_y_screen(tile.origin);

        //set virtual x and y position based on viewable tile coord
    }

    @Override
    public String toString(){
        return "GUIProxy["+x+","+y+"]";
    }
    
}
