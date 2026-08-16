/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.render;
import static org.lwjgl.opengl.GL11.*;
/**
 *
 * Works similar to SpriteRender, but operates a part of texture
 */
public class AreaRenderer {

    public String texture_name = "/resources/ui/window_ui_modern.png";

    public int x = 0;
    public int y = 0;
    public int w = 32;
    public int h = 32;

    public int TEXTURE_W = 256;
    public int TEXTURE_H = 256;

    public AreaRenderer(){
        
    }

    public void set_size(int w, int h){
        TEXTURE_W = w;
        TEXTURE_H = h;
    }

    public void set_coord(int x, int y){
        this.x = x;
        this.y = y;
    }
    public void set_rect(int x, int y, int w, int h){
        set_coord(x,y);
        this.w = w;
        this.h = h;
    }

    public float get_texture_x(){
        return (float) x / (float)TEXTURE_W;
    }
    public float get_texture_y(){
        return (float)y / (float)TEXTURE_H;
    }
    public float get_texture_w(){
        return (float)w / (float)TEXTURE_W;
    }
    public float get_texture_h(){
        return (float)h / (float)TEXTURE_H;
    }

    public void draw_quad(int x, int y, int w, int h){
        float tx = get_texture_x();
        float ty = get_texture_y();
        float ts_w = get_texture_w();
        float ts_h = get_texture_h();

        glBegin(GL_QUADS);
            glTexCoord2f(tx, ty);
        glVertex2f( x,   y);
            glTexCoord2f(tx+ts_w, ty);
	glVertex2f( x+w, y);
            glTexCoord2f(tx+ts_w, ty+ts_h);
	glVertex2f( x+w, y+h);
            glTexCoord2f(tx, ty+ts_h);
	glVertex2f( x,   y+h);

        glEnd();
    }

    public void render(int quad_x, int quad_y){
        render(quad_x,quad_y,w,h);
    }
    public void render(int quad_x, int quad_y, int quad_w, int quad_h){
        glColor3f(1.0f, 1.0f, 1.0f);
        Render.bind_texture(texture_name);
        draw_quad(quad_x,quad_y,quad_w,quad_h);
    }
    
    public void render(int quad_x, int quad_y, int quad_w, int quad_h, float alpha){
        glColor4f(1.0f, 1.0f, 1.0f, alpha);
        Render.bind_texture(texture_name);
        draw_quad(quad_x,quad_y,quad_w,quad_h);
    }
}
