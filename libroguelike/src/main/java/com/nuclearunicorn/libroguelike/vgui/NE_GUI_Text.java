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
    protected static int FONT_SIZE = 16;
    /** Distance between baselines. Both drawing and clicking must use the same one. */
    protected static final int LINE_PITCH = FONT_SIZE + 2;

    TrueTypeFont chat_ttf;

    boolean alignBottom = false;

    public NE_GUI_Text(){
        chat_ttf = OverlaySystem.precacheFont(FONT_SIZE);
    }

    @Override
    public void render(){

        //GL11.glRectf(get_x(), get_y(), get_x() + w, get_y() + h);

        for(int i=lines.size()-max_lines; i<lines.size();i++){
            if (i >= 0 ){
                render_line(i);
            }
        }
    }

    @Override
    public void e_on_mouse_click(EMouseClick e) {
        //super.e_on_mouse_click(e);
        //System.out.3println(this+"::click");

        int clientY = e.get_window_y() - this.get_y();
        //the row on screen, then the line it is showing: rows are drawn LINE_PITCH apart,
        //not FONT_SIZE, and a scrolled list is not showing line 0 at the top. Dividing by
        //the font size counted each row two pixels short, so the further down the list you
        //clicked the further off the answer - a ten-line list could not select its last line
        int lineId = clientY / LINE_PITCH + scrollOffset();

        System.out.println("Clicked on line #" + lineId);
        this.e_on_line_click(lineId, e);
    }

    /** Index of the topmost line on screen; non-zero once the list is longer than it is tall. */
    private int scrollOffset(){
        int offset = lines.size() - max_lines;
        return (offset <= 0 && !alignBottom) ? 0 : offset;
    }

    protected void e_on_line_click(int lineId, EMouseClick event) {
        //override me!
    }

    public void render_line(int i){

        int chat_offset = i - scrollOffset();

        chat_ttf.drawString(
                get_x(),
                get_y() + chat_offset*LINE_PITCH,
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
