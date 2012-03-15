/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.vgui;

import com.nuclearunicorn.libroguelike.events.EMouseClick;
import com.nuclearunicorn.libroguelike.render.overlay.OverlaySystem;
import org.newdawn.slick.Color;
import org.newdawn.slick.TrueTypeFont;

import java.util.ArrayList;

/**
 *
 * @author Administrator
 */
public class NE_GUI_Text extends NE_GUI_Element{

    class NE_GUI_TextLine {
        public String message;
        public Color color;

        public NE_GUI_TextLine(String message, Color color){
            this.message = message;
            this.color = color;
        }
    }
    
    ArrayList<NE_GUI_TextLine> lines = new ArrayList<NE_GUI_TextLine>(5);
    public int max_lines = 5;
    static final int FONT_SIZE = 16;

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

    @Override
    public void e_on_mouse_click(EMouseClick e) {
        //super.e_on_mouse_click(e);
        System.out.println(this+"::click");

        int clientY = e.get_window_y() - this.get_y();
        int lineId = clientY / FONT_SIZE;
        
        //System.out.println("Clicked on line #" + lineId);
        this.e_on_line_click(lineId);
    }

    protected void e_on_line_click(int lineId) {
        //override me!
    }

    public void render_line(int i){

        //int offset = lines.size()-max_lines;
        //int chat_offset = i - offset;
        int chat_offset = i;

        w = FONT_SIZE;
        h = lines.get(i).message.length()*FONT_SIZE;

        chat_ttf.drawString(
                get_x(),
                get_y()+ chat_offset*(FONT_SIZE + 2),
                lines.get(i).message , lines.get(i).color);
    }

    public void add_line(String text){
        add_line(text,Color.lightGray);
    }
    
    public void add_line(String text, Color color) {
        lines.add(new NE_GUI_TextLine(text, color));
    }

    public void clearLines() {
        lines.clear();
    }

}
