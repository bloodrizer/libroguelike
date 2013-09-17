package com.nuclearunicorn.negame.client.render;


import com.nuclearunicorn.libroguelike.game.world.WorldView;
import com.nuclearunicorn.libroguelike.render.EntityRenderer;
import com.nuclearunicorn.libroguelike.render.Render;
import com.nuclearunicorn.libroguelike.render.WindowRender;
import com.nuclearunicorn.libroguelike.render.overlay.OverlaySystem;
import com.nuclearunicorn.negame.client.render.utils.FBO;
import org.lwjgl.opengl.*;
import org.newdawn.slick.Color;

public class BillboardEntityRenderer extends EntityRenderer {

    private static FBO fbo = new FBO(32, 32);  //smalllll texture to fit ascii characters

    @Override
    public void render(){


        //OverlaySystem.ttf.drawString(0,0, "@", Color.red);
        fbo.render_begin();

        GL11.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

        OverlaySystem.ttf.drawString(4, 8, "@", Color.white);

        fbo.render_end();

        WindowRender.set3DMode();
        TilesetVoxelRenderer.camera.setMatrix();

        int tileHeight = WorldView.getYOffset(ent.tile);


        //point sprites

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL20.GL_POINT_SPRITE);
        GL11.glEnable(GL20.GL_VERTEX_PROGRAM_POINT_SIZE);

        //-----------------------------------------------------------------
        /*
        glEnable(GL_VERTEX_PROGRAM_POINT_SIZE_ARB);
        glHint(GL_POINT_SMOOTH_HINT, GL_NICEST);
        glEnable(GL_POINT_SPRITE);
        glActiveTexture(GL_TEXTURE0);
        glTexEnvi(GL_POINT_SPRITE, GL_COORD_REPLACE, GL_TRUE);

        //Activate shader program here
        // Send pointSize to shader program

        glBegin(GL_POINTS);
        // Render points here
        glVertex3f(...);
        glEnd(GL_POINTS);
         */
        //-----------------------------------------------------------------

        GL11.glEnable(GL20.GL_VERTEX_PROGRAM_POINT_SIZE); //_ARB?
        GL11.glHint(GL11.GL_POINT_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glEnable(GL20.GL_POINT_SPRITE);

        //Render.bind_texture("some texture");


        GL11.glBindTexture(GL11.GL_TEXTURE_2D, fbo.fbo_texture_id);

        

        GL11.glTexEnvi(GL20.GL_POINT_SPRITE, GL20.GL_COORD_REPLACE, GL11.GL_TRUE);

        //Replace me with shader
        GL11.glPointSize(tileHeight/2.0f);


        GL11.glEnable(GL11.GL_POINT_SMOOTH);
        GL11.glBegin(GL11.GL_POINTS);

        GL11.glColor3f(1.0f, 1.0f, 1.0f);
        //GL11.gl

        //GL20.glUniform1f(20, 20f);
        GL11.glVertex3f(ent.origin.getX() * 1.00005f, (tileHeight + 20 ) * 0.05f, ent.origin.getY() * 1.00005f);

        
        GL11.glEnd();

        WindowRender.set2DMode();
    }
}
