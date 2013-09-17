package com.nuclearunicorn.negame.client.render.utils;

import org.lwjgl.opengl.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;

import static org.lwjgl.opengl.EXTFramebufferObject.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL14.*;

/**
 *
 * @author Administrator
 */
public class FBO {

    /*
    * Sometimes fbo minimap rendering can result in buggy blinking screen.
    * In such case we must disable fbo minimap directly from the config file
    */

    public static boolean fbo_enabled = false;
    private static String use_fbo = null;

    static
    {
        /*try {
            Properties p = new Properties();
            p.load(new FileInputStream("client.ini"));
            use_fbo = p.getProperty("fbo_enabled");

            if (use_fbo == null){
                use_fbo = "1";
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        fbo_enabled = GLContext.getCapabilities().GL_EXT_framebuffer_object && (!use_fbo.equals("0"));
        */
        fbo_enabled = GLContext.getCapabilities().GL_EXT_framebuffer_object;
    }

    int fbo_id;
    public int fbo_texture_id;
    
    private int textureW;
    private int textureH;

    public FBO(int textureW, int textureH){

        this.textureW = textureW;
        this.textureH = textureH;

        if (fbo_enabled){
            //create texture
            fbo_texture_id = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, fbo_texture_id);

            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, 0);

            glTexImage2D(GL_TEXTURE_2D, 0,
                    GL_RGBA8, textureW, textureH, 0, GL_RGBA,
                    GL_UNSIGNED_BYTE, (ByteBuffer)null);

            glBindTexture(GL_TEXTURE_2D, 0);

            //now create framebuffer
            fbo_id = EXTFramebufferObject.glGenFramebuffersEXT();
            EXTFramebufferObject.glBindFramebufferEXT( EXTFramebufferObject.GL_FRAMEBUFFER_EXT, fbo_id );

            //attach a texture to FBO collor channel
            EXTFramebufferObject.glFramebufferTexture2DEXT( EXTFramebufferObject.GL_FRAMEBUFFER_EXT, EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT,
                    GL11.GL_TEXTURE_2D, fbo_texture_id, 0);

            //------------------------------------------------------------------
            //check FBO status and shit
            int status = glCheckFramebufferStatusEXT(GL_FRAMEBUFFER_EXT);
            if(status != GL_FRAMEBUFFER_COMPLETE_EXT) {
                throw new RuntimeException("Unsupported fbo status:"+status);
            }

            // switch back to window-system-provided framebuffer
            glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, 0);
        }
    }

    /*
    * Prepares FBO for rendering path
    */
    public void render_begin(){

        EXTFramebufferObject.glBindFramebufferEXT( EXTFramebufferObject.GL_FRAMEBUFFER_EXT, fbo_id );

        glPushAttrib(GL_VIEWPORT_BIT | GL_TRANSFORM_BIT | GL_COLOR_BUFFER_BIT | GL_SCISSOR_BIT);
        glDisable(GL_SCISSOR_TEST);
        glViewport(0, 0, textureW, textureH);

        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();
        glOrtho(0, textureW, textureH, 0, -1.0, 1.0);
        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();
        glDisable(GL_SCISSOR_TEST);
    }

    public void render_end(){
        EXTFramebufferObject.glBindFramebufferEXT( EXTFramebufferObject.GL_FRAMEBUFFER_EXT, 0);

        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
        glMatrixMode(GL_MODELVIEW);
        glPopMatrix();
        glPopAttrib();
    }
}