/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.render;

import org.lwjgl.opengl.*;

import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.util.Properties;

import static org.lwjgl.opengl.EXTFramebufferObject.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL12.GL_TEXTURE_MAX_LEVEL;

public class FBO {

    public static boolean fbo_enabled = true;
    private static String use_fbo = null;

    int fbo_id;
    public int fbo_texture_id;

    private int textureW;
    private int textureH;
    
    int currentTextureId = 0;

    public static void initGL(){
        fbo_enabled = GLContext.getCapabilities().GL_EXT_framebuffer_object;
    }

    public FBO(int textureW, int textureH){

        this.textureW = textureW;
        this.textureH = textureH;

        if (fbo_enabled){

            //create textures
            fbo_texture_id = GL11.glGenTextures();

            GL11.glBindTexture(GL11.GL_TEXTURE_2D, fbo_texture_id);

            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

            glTexParameteri(GL_TEXTURE_2D, GL14.GL_GENERATE_MIPMAP, GL_TRUE); // automatic mipmap

            //glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);

            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, 0);          //1 mipmap level?

            glTexImage2D(GL_TEXTURE_2D, 0,
                    GL_RGBA8, textureW, textureH, 0, GL_RGBA,
                    GL_UNSIGNED_BYTE, (ByteBuffer)null);
            //------------------------------------------------

            //unbind texture
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

            fbo_id = glGenFramebuffersEXT();
            glBindFramebufferEXT( GL_FRAMEBUFFER_EXT, fbo_id );
            //attach a texture to FBO color channel
            glFramebufferTexture2DEXT(GL_FRAMEBUFFER_EXT, GL_COLOR_ATTACHMENT0_EXT, GL_TEXTURE_2D, fbo_texture_id, 0);
            //------------------------------------------------------------------
            //check FBO status and shit
            int status = glCheckFramebufferStatusEXT(GL_FRAMEBUFFER_EXT);
            if(status != GL_FRAMEBUFFER_COMPLETE_EXT) {
                throw new RuntimeException("Unsupported fbo status:"+status);
            }

            if (glGetError() != 0){
                throw new RuntimeException("Failed to init FBO, gl error code:" + glGetError());
            }

            //todo: viewport?

            //unbind everything
            EXTFramebufferObject.glBindRenderbufferEXT(EXTFramebufferObject.GL_RENDERBUFFER_EXT, 0);
            EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, 0);
        }
    }

    /*
    * Prepares FBO for rendering path
    */
    public void renderBegin(){
        EXTFramebufferObject.glBindFramebufferEXT( EXTFramebufferObject.GL_FRAMEBUFFER_EXT, fbo_id );

        glViewport(0, 0, textureW, textureH);

        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();

        //0 , w, h, 0 if you want to flip ortho
        glOrtho(0, textureW, 0, textureH, -1.0, 1.0);
        glMatrixMode(GL_MODELVIEW);

        glPushMatrix();
        glLoadIdentity();
    }

    public void renderEnd(){
        glBindFramebufferEXT( GL_FRAMEBUFFER_EXT, 0);
        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
        glMatrixMode(GL_MODELVIEW);
        glPopMatrix();

    }
}
