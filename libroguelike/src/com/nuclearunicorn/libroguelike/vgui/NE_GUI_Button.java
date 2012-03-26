/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.vgui;

import com.nuclearunicorn.libroguelike.events.EMouseClick;
import com.nuclearunicorn.libroguelike.render.Render;
import com.nuclearunicorn.libroguelike.render.TilesetRenderer;
import com.nuclearunicorn.libroguelike.render.overlay.OverlaySystem;
import org.newdawn.slick.Color;

import static org.lwjgl.opengl.GL11.*;
/**
 *
 * @author Administrator
 */
public class NE_GUI_Button extends NE_GUI_Element{
    
    TilesetRenderer gui_tile;
    public Color color = Color.black;

    public String text = "Login";
    //public boolean closable = true;

    public NE_GUI_Button(){

        gui_tile = new TilesetRenderer();
        //gui_tile.texture_name = "../ui/window_ui_small.png";
        gui_tile.TILESET_W = 4;
        gui_tile.TILESET_H = 4;

        h = 32;

    }

    int button_w = 0;
    int BTN_TILE_SIZE = 32;

    /*
     *  Set window size in tiles
     *  recalculate w/h size
     */
    
    public void set_tw(int tw){
        button_w = tw;
        w = BTN_TILE_SIZE*button_w;
    }

    @Override
    public void render(){

        if(!visible){
            return;
        }
        
        //
        Render.bind_texture("/resources/ui/window_ui_small.png");
        

        int tile_id = 0;

        for(int i = 0; i<button_w; i++){

            if (i == 0){
                tile_id = 12;
            }else if (i == button_w-1){
                tile_id = 13;
            }else{
                tile_id = 14;
            }

            render_tile(i,0,tile_id);

        }

        int x = this.x + parent.x;
        int y = this.y + parent.y;

        OverlaySystem.ttf.drawString(
                x + w/2 - text.length()*9 /2 ,
                y + 7
                , text , color);

        super.render();
    }

    void render_tile(int i, int j, int tile_id){

        float tx = gui_tile.get_texture_x(tile_id);
        float ty = gui_tile.get_texture_y(tile_id);
        float ts = gui_tile.get_texture_w();


        int x = (i * BTN_TILE_SIZE) + this.x + parent.x;
        int y = (j * BTN_TILE_SIZE) + this.y + parent.y;

        int w = BTN_TILE_SIZE + 1;
        int h = BTN_TILE_SIZE + 1;

        glEnable(GL_TEXTURE_2D);
        glColor3f(1.0f,1.0f,1.0f);

        glBegin(GL_QUADS);
                glTexCoord2f(tx, ty);
            glVertex2f( x,   y);
                glTexCoord2f(tx+ts, ty);
            glVertex2f( x+w, y);
                glTexCoord2f(tx+ts, ty+ts);
            glVertex2f( x+w, y+h);
                glTexCoord2f(tx, ty+ts);
            glVertex2f( x,   y+h);
        glEnd();
    }

    @Override
    public void e_on_mouse_click(EMouseClick e){
          System.out.println("NE_GUI_Button::click");
    }

}

