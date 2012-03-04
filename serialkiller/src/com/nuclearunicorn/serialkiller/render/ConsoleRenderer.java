package com.nuclearunicorn.serialkiller.render;

import com.nuclearunicorn.libroguelike.game.world.WorldChunk;
import com.nuclearunicorn.libroguelike.game.world.WorldTile;
import com.nuclearunicorn.libroguelike.game.world.WorldViewCamera;
import com.nuclearunicorn.libroguelike.render.layers.LayerRenderer;
import com.nuclearunicorn.libroguelike.render.overlay.DebugOverlay;
import com.nuclearunicorn.libroguelike.utils.NLTimer;
import org.lwjgl.opengl.GL11;

import static org.lwjgl.opengl.GL11.*;

/**
 * Created by IntelliJ IDEA.
 * User: Administrator
 * Date: 03.03.12
 * Time: 20:35
 * To change this template use File | Settings | File Templates.
 */
public class ConsoleRenderer extends LayerRenderer{
    
    public static int TILE_SIZE = 16;
    NLTimer renderTimer = new NLTimer();
    
    @Override
    public void render_tile(WorldTile tile, int tile_x, int tile_y) {
        //this may be slow, but anyway, it's faster than render all those quads

        if (WorldViewCamera.tile_in_fov(tile_x,tile_y)){

            if (tile!= null && tile.get_height() != 100){
                glColor3f(0.5f,0.5f,1.0f);
            }else{
                glColor3f(1.0f,0.0f,0.0f);
            }

            renderTileQuad(tile_x,tile_y);
        }
    }
    
    
    public void renderTileQuad(int i, int j){

        renderTimer.push();

        glDisable(GL11.GL_TEXTURE_2D);


        if ((i % WorldChunk.CHUNK_SIZE == 0) ||
                j % WorldChunk.CHUNK_SIZE == 0){
            glColor3f(1.0f,0.5f,0.5f);
        }

        draw_quad(
                i*TILE_SIZE,
                j*TILE_SIZE,
                TILE_SIZE-1,
                TILE_SIZE-1
        );
        glEnable(GL11.GL_TEXTURE_2D);

        DebugOverlay.renderTime += renderTimer.popDiff();
    }

    private void draw_quad(int x, int y, int w, int h) {

            float tx = 0.0f;
            float ty = 0.0f;
            float ts_w = 1.0f;
            float ts_h = 1.0f;

            glBegin(GL_QUADS);
            //glTexCoord2f(tx, ty);
            glVertex2f( x,   y);
            //glTexCoord2f(tx+ts_w, ty);
            glVertex2f( x+w, y);
            //glTexCoord2f(tx+ts_w, ty+ts_h);
            glVertex2f( x+w, y+h);
            //glTexCoord2f(tx, ty+ts_h);
            glVertex2f( x,   y+h);

            glEnd();
    }
}
