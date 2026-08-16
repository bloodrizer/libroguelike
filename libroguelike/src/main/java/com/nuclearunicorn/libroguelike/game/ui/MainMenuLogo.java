/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nuclearunicorn.libroguelike.game.ui;

import com.nuclearunicorn.libroguelike.render.overlay.OverlaySystem;
import com.nuclearunicorn.libroguelike.utils.Timer;
import org.newdawn.slick.Color;
import org.newdawn.slick.TrueTypeFont;

/**
 * 
 * Simple main menu logo placeholder
 *
 * @author bloodrizer
 */
public class MainMenuLogo {
    private static final int LOGO_SIZE = 64;
    private TrueTypeFont logo_font;
    
    public int x;
    public int y;
    
    public String text = "NAMELESS  GAME";
    
    public MainMenuLogo(){
        logo_font = OverlaySystem.precache_font("minim_o.ttf", LOGO_SIZE);
    }

    public void render(){
        logo_font.drawString(x, y, text, color);
    }
    
    public int get_w(){
        return LOGO_SIZE*text.length();
    }
    
    public int get_h(){
        return LOGO_SIZE;
    }
    
    private int i;
    private Color color = Color.yellow;
    private long last_tick = Timer.get_time();
    
    public void update(){

        if ((Timer.get_time()-last_tick) < 50 ){
            return;
        }
        last_tick = Timer.get_time();

        i++;
        double a = (double)i*0.01f;
        color.r = (float)((128.0d+Math.sin(a)*128.0d)/255.0d);
        color.g = (float)((128.0d+Math.cos(a+2*Math.PI/3)*128.0f)/255.0d);
        color.b = (float)((128.0d+Math.sin(a+4*Math.PI/3)*128.0f)/255.0d); 
        
        //System.out.println("rgb("+color.r+","+color.g+","+color.b+")");
    }
}
