/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nuclearunicorn.libroguelike.vgui.effects;

import com.nuclearunicorn.libroguelike.events.ENotificationMessage;
import com.nuclearunicorn.libroguelike.render.WindowRender;
import com.nuclearunicorn.libroguelike.render.overlay.OverlaySystem;
import org.newdawn.slick.Color;

/**
 *
 * @author bloodrizer
 */
public class FXMessage extends Effect_Element{
    String msg;
    public FXMessage(ENotificationMessage message) {
        this.msg = message.msg;
    }
     
     
    @Override
    public void render(){
        if (msg == null || msg.isEmpty()){
            return;
        }

        float life_amt = (float)get_life_left()/(float)life_time;

        int x = WindowRender.get_window_w()/2 - msg.length()*9/2;
        int y = WindowRender.get_window_h()/2 - 8;

        Color text_color = new Color(100,0,0, (int)(life_amt*255) );

        OverlaySystem.ttf.drawString(x,y,msg, text_color);

    }
}
