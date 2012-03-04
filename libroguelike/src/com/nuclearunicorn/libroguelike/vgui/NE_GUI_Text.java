/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.vgui;

import com.nuclearunicorn.libroguelike.render.overlay.OverlaySystem;
import org.newdawn.slick.Color;
import org.newdawn.slick.TrueTypeFont;

import java.util.ArrayList;

/**
 *
 * @author Administrator
 */
class NE_GUI_Text extends NE_GUI_Element{
    ArrayList<String> lines = new ArrayList<String>(5);
    public int max_lines = 5;
    static final int FONT_SIZE = 12;

    TrueTypeFont chat_ttf;

    public NE_GUI_Text(){
        chat_ttf = OverlaySystem.precache_font(FONT_SIZE);
    }

    @Override
    public void render(){

        for(int i=lines.size()-max_lines; i<lines.size();i++){
            if (i >= 0 ){
                render_line(i);
            }
        }
    }

    public void render_line(int i){

        int offset = lines.size()-max_lines;
        int chat_offset = i - offset;

        w = FONT_SIZE;
        h = lines.get(i).length()*FONT_SIZE;

        chat_ttf.drawString(
                get_x(),
                get_y()+ chat_offset*(FONT_SIZE + 2),
                lines.get(i) , Color.black);
    }

    void add_line(String text) {
        lines.add(text);
    }
}
