package com.nuclearunicorn.negame.client.render;

import com.nuclearunicorn.libroguelike.game.world.WorldView;
import com.nuclearunicorn.libroguelike.render.EntityRenderer;
import com.nuclearunicorn.libroguelike.render.FBO;
import com.nuclearunicorn.libroguelike.render.WindowRender;
import com.nuclearunicorn.libroguelike.render.overlay.OverlaySystem;
import org.lwjgl.opengl.*;
import org.newdawn.slick.Color;
import org.newdawn.slick.opengl.TextureImpl;

import static org.lwjgl.opengl.GL11.*;

public class ASCIISpriteEntityRenderer extends EntityRenderer {

    private static FBO fbo = null;

    private String symbol = "?";
    private Color color = Color.white;

    public ASCIISpriteEntityRenderer(String symbol, Color color){
        this.symbol = symbol;
        this.color = color;
    }

    @Override
    public void render(){

        if (fbo == null){
            fbo = new FBO(32, 32);
            FBO.initGL();
        }

        //------------------- FBO BEGIN ----------------------
        fbo.renderBegin();

        glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        glClear(GL11.GL_COLOR_BUFFER_BIT);

        if (symbol == null || color == null){
            throw new RuntimeException("Cant render entity");
        }
        OverlaySystem.ttf.drawString(4, 8, "@", Color.red);

        fbo.renderEnd();

        TextureImpl.unbind();
        //------------------- FBO END ----------------------

        WindowRender.set3DMode();
        TilesetVoxelRenderer.camera.setMatrix();

        int tileHeight = WorldView.getYOffset(ent.tile);

        GL11.glEnable(GL20.GL_POINT_SPRITE);
        GL11.glEnable(GL20.GL_VERTEX_PROGRAM_POINT_SIZE);

        GL11.glHint(GL11.GL_POINT_SMOOTH_HINT, GL11.GL_NICEST);

        //  bind texture
        GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_COLOR);

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, fbo.fbo_texture_id);
        GL30.glGenerateMipmap(GL_TEXTURE_2D);
        GL11.glTexEnvi(GL20.GL_POINT_SPRITE, GL20.GL_COORD_REPLACE, GL11.GL_TRUE);

        //----------------
        GL11.glPointSize(tileHeight/2.0f);

        GL11.glBegin(GL11.GL_POINTS);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        GL11.glVertex3f(ent.origin.getX() * 1.00005f, (tileHeight + 20 ) * 0.05f, ent.origin.getY() * 1.00005f);
        GL11.glEnd();

        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        //If we don't unbind texture, all consequent FBO calls will fail
        glBindTexture(GL11.GL_TEXTURE_2D, 0);

        WindowRender.set2DMode();
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }
}
