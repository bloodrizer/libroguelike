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

    private static OverlaySystem _instance = null;
    
    public DebugOverlay debug = null;

    public static Font font = null;
    public static TrueTypeFont ttf = null;

    public static final int FONT_SIZE = 15;

    ///resources/ui/window_ui_modern.png
    static String FONT_PATH = "/resources/fonts/arialMonospaced.ttf";

    public OverlaySystem() {

        try {
            /*font = Font.createFont(Font.TRUETYPE_FONT, OverlaySystem.class.getResourceAsStream(FONT_PATH));
            font = font.deriveFont((float)FONT_SIZE); */

            font = new Font("Arial", Font.BOLD, FONT_SIZE);

        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }


        ttf = new TrueTypeFont(font, true);
    }

    public static OverlaySystem getInstance(){
        if (_instance != null){
            return _instance;
        } else {
            _instance = new OverlaySystem();
            return _instance;
        }
    }

    public static TrueTypeFont precacheFont(int size){
        return precacheFont(size, FONT_PATH);
    }

    public static TrueTypeFont precacheFont(int size, String fontPath){
        System.out.println("trying to precache font '"+ fontPath + "'");
        
        Font _font = null;
        try {
            _font = Font.createFont(Font.TRUETYPE_FONT, OverlaySystem.class.getResourceAsStream(FONT_PATH));
            _font = font.deriveFont((float)FONT_SIZE);
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

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

        WorldTile tileFrom = ClientGameEnvironment.getWorldLayer(WorldLayer.GROUND_LAYER).getTile(tileCoord1);
        WorldTile tileTo = ClientGameEnvironment.getWorldLayer(WorldLayer.GROUND_LAYER).getTile(tileCoord2);

        int y_offset1 = WorldView.getYOffset(tileFrom);
        int y_offset2 = WorldView.getYOffset(tileTo);
        
        //System.out.println("drawing line from "+tileCoord1+" to "+tileCoord2);

        int d = 8; //TODO: FIX ME

        drawLine(x1 + d, y1 + y_offset1 + d, x2 + d, y2 + y_offset2 + d, color);
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
            return precacheFont(size);
        }
    }

    //render whole overlay
    public void render() {
        
        DebugOverlay.render();
        TileCoordOverlay.render();
        VersionOverlay.render();
        
    }


}
