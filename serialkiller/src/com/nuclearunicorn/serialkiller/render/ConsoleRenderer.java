package com.nuclearunicorn.serialkiller.render;

import com.nuclearunicorn.libroguelike.core.Input;
import com.nuclearunicorn.libroguelike.game.player.Player;
import com.nuclearunicorn.libroguelike.game.world.WorldChunk;
import com.nuclearunicorn.libroguelike.game.world.WorldTile;
import com.nuclearunicorn.libroguelike.game.world.WorldTimer;
import com.nuclearunicorn.libroguelike.game.world.WorldViewCamera;
import com.nuclearunicorn.libroguelike.render.layers.LayerRenderer;
import com.nuclearunicorn.libroguelike.render.overlay.DebugOverlay;
import com.nuclearunicorn.libroguelike.render.overlay.OverlaySystem;
import com.nuclearunicorn.libroguelike.utils.NLTimer;
import com.nuclearunicorn.serialkiller.game.world.RLTile;
import org.lwjgl.opengl.GL11;
import org.newdawn.slick.Color;

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
           RLTile rltile = (((RLTile)tile));
           if (rltile == null){
                return;
           }
            
           if (!rltile.isExplored() && !Input.key_state_alt){
               return;
           }

           if (rltile.isVisible()){
               setFovColor(tile_x, tile_y);
               //setBgColor(200, 180, 50);
           }else{
               //int dRGB = (int)(WorldTimer.get_light_amt()*255);

               int r = (int)lerp(60, 50 , WorldTimer.get_light_amt());
               int g = (int)lerp(60, 50 , WorldTimer.get_light_amt());
               int b = (int)lerp(60, 150 , WorldTimer.get_light_amt());
               setBgColor(r, g, b);
           }

           if (rltile.isWall()){
               if (rltile.isVisible()){
                    setBgColor(130, 110, 50);
               }else{
                    setBgColor(0, 0, 100);
               }
           }

           renderTileQuad(tile_x,tile_y);

           if (rltile.isWall()){
                drawChar(tile_x, tile_y, "#");
           }else{
                if(rltile.isOwned()){
                    drawChar(tile_x, tile_y, ".", Color.red);
                }
           }

           if (!rltile.getTileModel().isEmpty() && rltile.getTileModelColor() != null){
                drawChar(tile_x, tile_y, rltile.getTileModel(), rltile.getTileModelColor());
           }
        }
    }

    static public final float lerp(float start, float stop, float amt) {
        return start + (stop-start) * amt;
    }

    private void setFovColor(int tile_x, int tile_y) {
        //200, 180, 50
        
        //50, 50, 150
        int dx = Player.get_ent().origin.getX()-tile_x;
        int dy = Player.get_ent().origin.getY()-tile_y;
        //float disst = (float)Math.sqrt(dx*dx+dy*dy);
        float disst = dx*dx+dy*dy;

        //float amt = 1.0f / ((float)Math.sqrt(disst));
        //float amt = 1.0f / (float)Math.sqrt((Math.sqrt(disst)));
        float amt = 32.0f / (disst);
        if (amt > 1.0f){
            amt = 1.0f;
        }

        float r = lerp(50, 200, amt)/255f;
        float g = lerp(50, 180, amt)/255f;
        float b = lerp(150, 50, amt)/255f;

        glColor3f(r,g,b);
    }

    private void drawChar(int i, int j, String tileModel, Color tileModelColor) {
        OverlaySystem.ttf.drawString(i*TILE_SIZE,j*TILE_SIZE-2, tileModel, tileModelColor);
    }

    public void setBgColor(int r, int g, int b){
        float rf = r/255f;
        float gf = g/255f;
        float bf = b/255f;

        glColor3f(rf,gf,bf);
    }

    public void setBgColor(Color color){
        setBgColor((int)color.r, (int)color.g, (int)color.b);
    }
    
    
    public void renderTileQuad(int i, int j){

        renderTimer.push();

        glDisable(GL11.GL_TEXTURE_2D);


        if ((i % WorldChunk.CHUNK_SIZE == 0) ||
                j % WorldChunk.CHUNK_SIZE == 0){
            glColor3f(1.0f,0.5f,0.5f);
        }

        if (!Input.key_state_alt){
            drawQuad(
                    i * TILE_SIZE,
                    j * TILE_SIZE,
                    TILE_SIZE,
                    TILE_SIZE
            );
        }else{
            drawQuad(
                    i * TILE_SIZE,
                    j * TILE_SIZE,
                    TILE_SIZE -1,
                    TILE_SIZE -1
            );
        }
        glEnable(GL11.GL_TEXTURE_2D);

        DebugOverlay.renderTime += renderTimer.popDiff();
    }
    
    private void drawChar(int i, int j, String symbol){
        OverlaySystem.ttf.drawString(i*TILE_SIZE,j*TILE_SIZE-2, symbol);
    }

    private void drawQuad(int x, int y, int w, int h) {

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
