/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


package com.nuclearunicorn.libroguelike.render;

import com.nuclearunicorn.libroguelike.core.Game;
import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Cursor;
import org.lwjgl.input.Mouse;
import org.newdawn.slick.opengl.Texture;
import org.newdawn.slick.opengl.TextureLoader;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.IntBuffer;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 *
 * 
 */
public class Render {
    private static java.util.Map<String,Texture> texture_cache = Collections.synchronizedMap(new java.util.HashMap<String,Texture>(32));


    public static Texture precache_texture(String name, String format){
        try {
            /*Texture texture = TextureLoader.getTexture("PNG", new FileInputStream(
                Render.class.getResource(name).getPath()
            ));*/
            Texture texture = null;

            InputStream texture_stream = Render.class.getResourceAsStream(name);
            if (texture_stream != null){
                texture = TextureLoader.getTexture(format, texture_stream);
            }else{
                System.err.println("missing texture('"+name+"') - using default");

                texture = getInvalidTexture();
            }

            texture_cache.put(name, texture);
            return texture;
        }
        catch (IOException ex) {
            System.err.println(ex.getMessage() + '('+ Render.class.getResource(name).getPath()+ ')');
            Logger.getLogger(Render.class.getName()).log(Level.SEVERE,
                    ex.getMessage() + '('+ Render.class.getResource(name).getPath()+ ')',
            ex);
            Game.running = false;
        }
        return null;
    }
    
    public static Texture getInvalidTexture() throws IOException {
        Texture texture = null;

        InputStream texture_stream = Render.class.getResourceAsStream("/resources/invalid_texture.png");
        if (texture_stream != null){
            return TextureLoader.getTexture("PNG", texture_stream);
        }
        throw new IOException("Failed to load default texture placeholder");
    }

    public static Texture precache_texture(String name){
        return precache_texture(name,"PNG");
    }

    public static Texture get_texture(String name){
        Texture texture = texture_cache.get(name);
        
        if(texture != null){
            return texture;
        }else{
            return precache_texture(name);
        }
    }

    public static void bind_texture(String name){
        Texture texture = get_texture(name);
        if (texture != null){
            texture.bind();
        }
    }

   static String cursor_name = "";

   public static void set_cursor(String cursor_name){

       /*
        This is little security check to prevent memory overhype
        */

       if(Render.cursor_name.equals(cursor_name)){
           return;
       }
       Render.cursor_name = cursor_name;


        
        IntBuffer ib = getHandMousePointer(cursor_name);
        Texture newCursor = get_texture(cursor_name);
        try {
            
            Cursor c = new Cursor(
                    newCursor.getImageWidth(),
                    newCursor.getImageHeight(),
                    0, 
                    newCursor.getImageHeight()-1,
                    1, ib, null);
            Mouse.setNativeCursor(c);
            
        } catch (LWJGLException ex) {
            Logger.getLogger(Render.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static final int CURSOR_SIZE = 32;

    public static IntBuffer getHandMousePointer(String cursor_name)
    {
        Image c=Toolkit.getDefaultToolkit().getImage(Icon.class.getResource(cursor_name));
        BufferedImage biCursor=new BufferedImage(CURSOR_SIZE,CURSOR_SIZE,BufferedImage.TYPE_INT_ARGB);
        while(!biCursor.createGraphics().drawImage(c,0,
                CURSOR_SIZE-1,
                CURSOR_SIZE-1,0,0,0,
                CURSOR_SIZE-1,
                CURSOR_SIZE-1,null))
          try
          {
            Thread.sleep(5);
          }
          catch(InterruptedException e)
          {
          }

        int[] data=biCursor.getRaster().getPixels(0,0,CURSOR_SIZE,CURSOR_SIZE,(int[])null);

        IntBuffer ib=BufferUtils.createIntBuffer(CURSOR_SIZE*CURSOR_SIZE);
        for(int i=0;i<data.length;i+=4)
          ib.put(data[i] |
                 data[i+1]<<8|
                 data[i+2]<<16|
                 data[i+3]<<24
          );
        ib.flip();
        return ib;
    }

}
