/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.vgui;

import com.nuclearunicorn.libroguelike.render.Render;
import com.nuclearunicorn.libroguelike.render.TilesetRenderer;

import static org.lwjgl.opengl.GL11.*;

/**
 *
 * @author Administrator
 */
public class NE_GUI_Frame extends NE_GUI_Element{

    TilesetRenderer gui_tile;

    NE_GUI_Frame_Close close_button;

    //public boolean closable = true;

    public NE_GUI_Frame(boolean closable){

        gui_tile = new TilesetRenderer();
        //gui_tile.texture_name = "../ui/window_ui_small.png";
        gui_tile.TILESET_W = 4;
        gui_tile.TILESET_H = 4;


        if (closable){
            close_button = new NE_GUI_Frame_Close();

            close_button.w = 32;
            close_button.h = 32;
            close_button.dragable = false;

            add(close_button);
        }


    }

    int t_window_w = 0;
    int t_window_h = 0;
    public static final int WIN_TILE_SIZE = 32;

    /*
     *  Set window size in tiles
     *  recalculate w/h size
     */
    
    public void set_tw(int tw){
        t_window_w = tw;
        w = WIN_TILE_SIZE*t_window_w;

        if (close_button != null){
            close_button.x = w - close_button.w;
            close_button.y = 0;
        }

    }
    public void set_th(int th){
        t_window_h = th;
        h = WIN_TILE_SIZE*t_window_h;
    }

    @Override
    public void render(){
        
        if(!visible){
            return;
        }
        
        Render.bind_texture("/resources/ui/window_ui_small.png");
        glColor3f(1.0f, 1.0f, 1.0f);

        int tile_id = 0;

        for(int i = 0; i<t_window_w; i++)
            for(int j = 0; j<t_window_h; j++){

                //tile_id = 6;    //center

                if (j == 0){
                    if (i==0)
                        tile_id = 0;    //ul
                    else if(i == t_window_w-1)
                        tile_id = 2;    //ur
                    else
                        tile_id = 1;    //uc
                }else if(j == t_window_h-1){
                    
                     if (i==0)
                        tile_id = 8;    //bl
                    else if(i == t_window_w-1)
                        tile_id = 10;    //br
                    else
                        tile_id = 9;    //bc
                }else{
                     if (i==0)
                        tile_id = 4;    //ml
                    else if(i == t_window_w-1)
                        tile_id = 6;    //mr
                    else
                        tile_id = 5;    //mc
                }
                //-------------------

                render_window_tile(i,j,tile_id);
                
            }

            
        
        super.render();
    }

    void render_window_tile(int i, int j, int tile_id){

        float tx = gui_tile.get_texture_x(tile_id);
        float ty = gui_tile.get_texture_y(tile_id);
        float ts = gui_tile.get_texture_w();


        int x = i * WIN_TILE_SIZE + this.x;
        int y = j * WIN_TILE_SIZE + this.y;
        
        int w = WIN_TILE_SIZE;
        int h = WIN_TILE_SIZE;

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

     public void on_close(){
         
     }

}
