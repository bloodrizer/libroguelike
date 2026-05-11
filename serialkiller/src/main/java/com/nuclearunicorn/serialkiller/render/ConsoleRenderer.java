package com.nuclearunicorn.serialkiller.render;

import com.nuclearunicorn.libroguelike.core.Input;
import com.nuclearunicorn.libroguelike.game.player.Player;
import com.nuclearunicorn.libroguelike.game.world.WorldChunk;
import com.nuclearunicorn.libroguelike.game.world.WorldTile;
import com.nuclearunicorn.libroguelike.game.world.WorldTimer;
import com.nuclearunicorn.libroguelike.game.world.WorldViewCamera;
import com.nuclearunicorn.libroguelike.render.Render;
import com.nuclearunicorn.libroguelike.render.layers.LayerChunkRenderer;
import com.nuclearunicorn.libroguelike.render.overlay.DebugOverlay;
import com.nuclearunicorn.libroguelike.render.overlay.OverlaySystem;
import com.nuclearunicorn.libroguelike.utils.NLTimer;
import com.nuclearunicorn.serialkiller.game.social.SocialController;
import com.nuclearunicorn.serialkiller.game.world.RLTile;
import com.nuclearunicorn.serialkiller.game.world.entities.EntityRLHuman;
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
public class ConsoleRenderer extends LayerChunkRenderer {
    
    public static int TILE_SIZE = 16;
    NLTimer renderTimer = new NLTimer();
    private static final boolean ENABLE_TEXTURE = false;
    private static final boolean DISABLE_TEXTURE = true;

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
               setFovColor(rltile, tile_x, tile_y);
               //setBgColor(200, 180, 50);
           }else{
               //int dRGB = (int)(WorldTimer.get_light_amt()*255);
               
               int rBlood = (int)lerp(0, 100, rltile.getBloodAmt());

               int r = (int)lerp(60, 50 , WorldTimer.get_light_amt());
               int g = (int)lerp(60, 50 , WorldTimer.get_light_amt());
               int b = (int)lerp(60, 150 , WorldTimer.get_light_amt());

               //-------------bloodlust shader------------
               float bloodlustAmt = ((EntityRLHuman)Player.get_ent()).getBodysim().getBloodlust()/400f;
               r = (int)lerp(r+rBlood, 200, bloodlustAmt);
               g = (int)lerp(g, 50, bloodlustAmt);
               b = (int)lerp(b, 50, bloodlustAmt);
               //-----------------------------------------

               setBgColor(r + rBlood, g - rBlood/2, b - rBlood/2);
           }

           if (rltile.isWall()){
               if (rltile.isVisible()){
                    setBgColor(130, 110, 50);
               }else{
                    setBgColor(0, 0, 100);
               }
           }

            if (rltile.getTileType() == RLTile.TileType.ROAD){
                renderTileQuad(tile_x,tile_y, DISABLE_TEXTURE);

                glColor3f(1.0f,1.0f,1.0f);
                Render.bind_texture("/resources/road_16.png");
                renderTileQuad(tile_x,tile_y, ENABLE_TEXTURE);
            }else{
                renderTileQuad(tile_x,tile_y, DISABLE_TEXTURE);
            }

           if (rltile.isWall()){
                drawChar(tile_x, tile_y, "#");
           }else{
                if(rltile.isOwned()){
                    drawChar(tile_x, tile_y, ".", Color.red);
                }
           }
            
           //crimeplace visualization
           if (SocialController.hasCrimeplace(tile.origin)){
               drawChar(tile_x, tile_y, "X", Color.red);
           }

           if (!rltile.getTileModel().isEmpty() && rltile.getTileModelColor() != null){
                drawChar(tile_x, tile_y, rltile.getTileModel(), rltile.getTileModelColor());
           }
        }
    }

    @Override
    public void beforeRender() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void afterRender() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    static public final float lerp(float start, float stop, float amt) {
        return start + (stop-start) * amt;
    }

    private void setFovColor(RLTile rlTile, int tile_x, int tile_y) {
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


        //int rBlood = (int)lerp(0, 100, rlTile.getBloodAmt());


        float r = lerp(50, 200, amt);
        float g = lerp(50, 180, amt);
        float b = lerp(150, 50, amt);

        //-------------bloodlust shader------------
        float bloodlustAmt = ((EntityRLHuman)Player.get_ent()).getBodysim().getBloodlust()/400f;
        r = (int)lerp(r, 200, bloodlustAmt);
        g = (int)lerp(g, 50, bloodlustAmt);
        b = (int)lerp(b, 50, bloodlustAmt);
        //-----------------------------------------


        glColor3f(
                lerp(r, 250, rlTile.getBloodAmt())/255f,
                lerp(g, 50, rlTile.getBloodAmt())/255f,
                lerp(b, 50, rlTile.getBloodAmt())/255f
        );
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
    
    
    public void renderTileQuad(int i, int j, boolean disableTexture){

        renderTimer.push();

        if (disableTexture){
            glDisable(GL11.GL_TEXTURE_2D);
        }else{
            glEnable(GL11.GL_TEXTURE_2D);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            glEnable(GL_BLEND);
        }


        if ((i % WorldChunk.CHUNK_SIZE == 0) ||
                j % WorldChunk.CHUNK_SIZE == 0){
            glColor3f(1.0f,0.5f,0.5f);
        }

        if (!Input.key_state_alt){
            drawQuad(
                    i * TILE_SIZE,
                    j * TILE_SIZE,
                    TILE_SIZE,
                    TILE_SIZE,
                    disableTexture
            );
        }else{
            drawQuad(
                    i * TILE_SIZE,
                    j * TILE_SIZE,
                    TILE_SIZE -1,
                    TILE_SIZE -1,
                    disableTexture
            );
        }
        if (disableTexture){
            glEnable(GL11.GL_TEXTURE_2D);
        }else{
            //glDisable(GL_BLEND);
            //glEnable(GL_DEPTH_TEST);
        }

        DebugOverlay.renderTime += renderTimer.popDiff();
    }
    
    private void drawChar(int i, int j, String symbol){
        OverlaySystem.ttf.drawString(i*TILE_SIZE,j*TILE_SIZE-2, symbol);
    }

    private void drawQuad(int x, int y, int w, int h, boolean disableTexture) {

            float tx = 0.0f;
            float ty = 0.0f;
            float ts_w = 1.0f;
            float ts_h = 1.0f;

            glBegin(GL_QUADS);
            if (!disableTexture){
                glTexCoord2f(tx, ty);
            }
            glVertex2f( x,   y);
            if (!disableTexture){
                glTexCoord2f(tx+ts_w, ty);
            }
            glVertex2f( x+w, y);
            if (!disableTexture){
                glTexCoord2f(tx+ts_w, ty+ts_h);
            }
            glVertex2f( x+w, y+h);
            if (!disableTexture){
                glTexCoord2f(tx, ty+ts_h);
            }
            glVertex2f( x,   y+h);

            glEnd();
    }
}
