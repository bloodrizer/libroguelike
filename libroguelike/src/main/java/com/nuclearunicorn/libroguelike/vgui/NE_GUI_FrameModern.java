/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.vgui;

import com.nuclearunicorn.libroguelike.render.AreaRenderer;
import com.nuclearunicorn.libroguelike.render.overlay.OverlaySystem;
import org.newdawn.slick.TrueTypeFont;

/**
 * Modern version of GUI Frame, that supports any kind of dimensions
 */
public class NE_GUI_FrameModern extends NE_GUI_Frame {
    AreaRenderer area_renderer;
    public String title = "undefined";

    static final int TITLE_SIZE = 12;
    public static TrueTypeFont title_ttf = OverlaySystem.precacheFont(TITLE_SIZE);

    public void set_title(String title){
        this.title = title;
    }

    public NE_GUI_FrameModern(boolean close_button){
        super(close_button);
        area_renderer = new AreaRenderer();
        area_renderer.w = 34;
        area_renderer.h = 34;
    }

    public NE_GUI_FrameModern(){
        super(false);
        area_renderer = new AreaRenderer();
        area_renderer.w = 32;
        area_renderer.h = 32;
    }

    @Override
    public void render(){
        super.render();

        //area_renderer.render(x,y);
    }

    @Override
    void render_window_tile(int i, int j, int tile_id){
        int tile_x = i * WIN_TILE_SIZE + this.get_x();
        int tile_y = j * WIN_TILE_SIZE + this.get_y();

        switch(tile_id){
            case 0: //ul
                area_renderer.set_rect(0,0, 34, 34);
            break;
            case 1:
                area_renderer.set_rect(34,0, 32, 32);
            break;
            case 2: //ur
                area_renderer.set_rect(64,0, 34, 34);
            break;
            case 4: //ml
                area_renderer.set_rect(0,32, 32, 32);
            break;
            case 5: //mc
                area_renderer.set_rect(34,32, 32, 32);
            break;
            case 6: //mr
                area_renderer.set_rect(64,32, 32,32);
            break;
            case 8: //bl
                area_renderer.set_rect(0,62, 34, 34);
            break;
            case 9: //bc
                area_renderer.set_rect(34,62, 32, 32);
            break;
            case 10: //br
                area_renderer.set_rect(64,62, 34, 34);
            break;
        }
        area_renderer.render(tile_x,tile_y);

        title_ttf.drawString(get_x()+w/2 - title.length()*TITLE_SIZE/4 , get_y(), title);
    }

}
