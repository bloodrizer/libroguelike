/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.render;

import com.nuclearunicorn.libroguelike.core.Game;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.util.glu.GLU;

import java.awt.*;

import static org.lwjgl.opengl.GL11.*;

/**
 *
 * All low-level render routine should go there
 *
 *
 * note, it's not implementing IRenderer, as it is not designed for entity render
 */


public class WindowRender {

    static int WINDOW_W = 1024;
    static int WINDOW_H = 768;

    public static void create() throws LWJGLException {

        Canvas parent = Game.display_parent;

        if(parent != null){
            System.out.println("setting render parent");
            Display.setParent(parent);
        }

        Display.setDisplayMode(new DisplayMode(WINDOW_W, WINDOW_H));
        Display.create();
        Display.setTitle("The Nameless Engine");
        Display.setVSyncEnabled(true);

        WindowRender.initGL(WINDOW_W, WINDOW_H);

        if (GLContext.getCapabilities().GL_ARB_vertex_buffer_object == false) {
            throw new RuntimeException("OpenGL Vertex Buffer Objects are not supported by Graphics Card. Unable to run program.");
        }
        
    }

    public static int get_window_h(){
        return WINDOW_H;
    }
    
    public static int get_window_w(){
        return WINDOW_W;
    }

    public static void initGL(int w, int h){

        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        
        GL11.glViewport(0, 0, w, h);
        GL11.glOrtho(0.0f, w, h, 0.0f, -1.0f, 1.0f);

        GL11.glClearColor(0, 0, 0, 1);

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        //GL11.glShadeModel(GL11.GL_SMOOTH);

        GL11.glEnable (GL11.GL_BLEND);
        GL11.glBlendFunc (GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        //wtf?
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();

        
    }

    public static void set3DMode(){
        /*  SET 3D RENDER MODE */

        glViewport(0,0,WindowRender.get_window_w(),WindowRender.get_window_h());
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();

        GLU.gluPerspective(45.0f, ((float) WindowRender.get_window_w()) / ((float) WindowRender.get_window_h()), 0.1f, 100.0f);
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();

        GL11.glClearDepth(1.0f);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
    }

    public static void set2DMode(){

        //glEnable(GL11.GL_TEXTURE_2D);


        GL11.glLoadIdentity();

        GL11.glViewport(0, 0, WindowRender.get_window_w(), WindowRender.get_window_h());
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();


        GL11.glOrtho(0.0f, WindowRender.get_window_w(), WindowRender.get_window_h(), 0.0f, -1.0f, 1.0f);
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
    }

    public static void destroy(){
        Display.destroy();
    }
}
