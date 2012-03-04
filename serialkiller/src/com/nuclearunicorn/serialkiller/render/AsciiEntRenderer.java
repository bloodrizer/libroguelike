package com.nuclearunicorn.serialkiller.render;

import com.nuclearunicorn.libroguelike.render.EntityRenderer;
import com.nuclearunicorn.libroguelike.render.overlay.OverlaySystem;
import org.lwjgl.util.vector.Vector3f;

/**
 * Created by IntelliJ IDEA.
 * User: Administrator
 * Date: 03.03.12
 * Time: 21:25
 * To change this template use File | Settings | File Templates.
 */
public class AsciiEntRenderer extends EntityRenderer {

    String symbol = "?";
    Vector3f color;

    public AsciiEntRenderer(String s) {
        super();
        
        this.symbol = s;
    }

    public void render(){
        int x = ent.x();
        int y = ent.y();
        int w = ConsoleRenderer.TILE_SIZE;

        OverlaySystem.ttf.drawString(x*w,y*w-2, symbol);
    }
    
    
}
