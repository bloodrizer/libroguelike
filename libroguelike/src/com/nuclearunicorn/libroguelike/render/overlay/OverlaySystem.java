/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nuclearunicorn.libroguelike.render.overlay;

import com.nuclearunicorn.libroguelike.core.client.ClientGameEnvironment;
import com.nuclearunicorn.libroguelike.game.world.WorldTile;
import com.nuclearunicorn.libroguelike.game.world.WorldView;
import com.nuclearunicorn.libroguelike.game.world.layers.WorldLayer;
import com.nuclearunicorn.libroguelike.render.Render;
import org.lwjgl.util.Point;
import org.newdawn.slick.Color;
import org.newdawn.slick.TrueTypeFont;

import java.awt.*;
import java.io.InputStream;

import static org.lwjgl.opengl.GL11.*;


/**
 * Overlay System features lot's of debug functions and render helpers, that are associated neither with world rendering, no GUI system.
 * 
 * It can be debug text, or lines, or graphs, etc.
 *
 * @author bloodrizer
 */
public class OverlaySystem {

    
    public DebugOverlay debug = null;

    private static Font font = null;
    public  static TrueTypeFont ttf = null;

    public static final int FONT_SIZE = 15;

    public OverlaySystem() {
        font = new Font("Arial", Font.BOLD, FONT_SIZE);
        ttf = new TrueTypeFont(font, true);
    }

    public static TrueTypeFont precache_font(int size){
        Font _font = new Font("Arial", Font.BOLD, size);
        TrueTypeFont _ttf = new TrueTypeFont(_font, true);

        return _ttf;
    }
    
    /**
     * Connects two tiles in given world coordinates
     * That might be useful for AI pathfinding debug
     */
    public static void drawLine(Point tileCoord1, Point tileCoord2, Color color){
        int x1 = WorldView.get_tile_x_screen(tileCoord1);
        int y1 = WorldView.get_tile_y_screen(tileCoord1);
        int x2 = WorldView.get_tile_x_screen(tileCoord2);
        int y2 = WorldView.get_tile_y_screen(tileCoord2);

        WorldTile tileFrom = ClientGameEnvironment.getWorldLayer(WorldLayer.GROUND_LAYER).get_tile(tileCoord1);
        WorldTile tileTo = ClientGameEnvironment.getWorldLayer(WorldLayer.GROUND_LAYER).get_tile(tileCoord2);

        int y_offset1 = WorldView.getYOffset(tileFrom);
        int y_offset2 = WorldView.getYOffset(tileTo);
        
        //System.out.println("drawing line from "+tileCoord1+" to "+tileCoord2);

        drawLine(x1, y1 + y_offset1, x2, y2 + y_offset2, color);
    }
    
    public static void drawLine(int x1, int y1, int x2, int y2, Color color){

        //System.out.println("drawing line from ["+x1+","+y1+"] to ["+x2+","+y2+"]");

        glEnable(GL_POINT_SMOOTH);
        glDisable(GL_TEXTURE_2D);
        
        glLineWidth(4);

        glBegin(GL_LINES);
        glColor3f(color.r, color.g, color.b);
        glVertex2f(x1, y1);
        glVertex2f(x2, y2);
        glEnd();

        glPointSize(8);
        glBegin(GL_POINTS);
        glColor3f(1, 0, 0);
        glVertex2f(x2, y2);
        glEnd();

        glEnable(GL_TEXTURE_2D);
    }

    public static TrueTypeFont precache_font(String filename, int size){
        try {
            InputStream is = Render.class.getResourceAsStream(filename);
            Font __font = Font.createFont(Font.TRUETYPE_FONT, is);

            TrueTypeFont _ttf = new TrueTypeFont(__font.deriveFont(Font.PLAIN, size), true);
            return _ttf;
        }
        catch(Exception e){
            System.err.println(filename + " not loaded.  Using serif font.");
            return precache_font(size);
        }
    }

    //render whole overlay
    public void render() {
        
        DebugOverlay.render();
        TileCoordOverlay.render();
        VersionOverlay.render();
        
    }


}
