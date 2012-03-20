/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.vgui;

import com.nuclearunicorn.libroguelike.render.overlay.OverlaySystem;
import org.newdawn.slick.Color;

/**
 *
 * @author Administrator
 */
public class NE_GUI_Label extends NE_GUI_Element {
    public String text = "Login";
    private Color color = Color.black;

    public NE_GUI_Label(){
        h = 10;
        w = 30;
    }

    public void set_text(String text){
        this.text = text;
        w = text.length()*9;
    }

    @Override
    public void render(){

        w = OverlaySystem.FONT_SIZE;
        h = text.length()*OverlaySystem.FONT_SIZE;

        OverlaySystem.ttf.drawString(
                get_x(),
                get_y(),
                text , color);

    }

    public void setColor(Color color) {
        this.color = color;
    }
}
