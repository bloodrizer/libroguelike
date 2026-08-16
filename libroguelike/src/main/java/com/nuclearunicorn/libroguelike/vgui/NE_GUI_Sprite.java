/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.vgui;

import com.nuclearunicorn.libroguelike.render.Render;

import static org.lwjgl.opengl.GL11.*;

/**
 *
 * @author Administrator
 */
public class NE_GUI_Sprite extends NE_GUI_Element {

    public String sprite_name = "gfx/items/undefined.png";

    @Override
    public void render(){

        Render.bind_texture(sprite_name);

        int x = this.get_x();
        int y = this.get_y();

        glEnable(GL_TEXTURE_2D);
        glColor3f(1.0f,1.0f,1.0f);
        
        glBegin(GL_QUADS);
                glTexCoord2f(0.0f, 0.0f);
            glVertex2f( x,   y);
                glTexCoord2f(0.0f+1.0f, 0.0f);
            glVertex2f( x+w, y);
                glTexCoord2f(0.0f+1.0f, 0.0f+1.0f);
            glVertex2f( x+w, y+h);
                glTexCoord2f(0.0f, 0.0f+1.0f);
            glVertex2f( x,   y+h);
        glEnd();

        super.render();
    }
}
