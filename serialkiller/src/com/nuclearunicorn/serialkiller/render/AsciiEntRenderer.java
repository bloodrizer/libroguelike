package com.nuclearunicorn.serialkiller.render;

import com.nuclearunicorn.libroguelike.core.Input;
import com.nuclearunicorn.libroguelike.game.world.WorldViewCamera;
import com.nuclearunicorn.libroguelike.render.EntityRenderer;
import com.nuclearunicorn.libroguelike.render.overlay.OverlaySystem;
import com.nuclearunicorn.serialkiller.game.world.RLTile;
import org.lwjgl.util.vector.Vector3f;

/**
 * Created by IntelliJ IDEA.
 * User: Administrator
 * Date: 03.03.12
 * Time: 21:25
 * To change this template use File | Settings | File Templates.
 */
public class AsciiEntRenderer extends EntityRenderer {

    public String symbol = "?";
    Vector3f color;

    public AsciiEntRenderer(String s) {
        super();
        
        this.symbol = s;
    }

    public void render(){
        int x = ent.x();
        int y = ent.y();
        int w = ConsoleRenderer.TILE_SIZE;

        if (!((RLTile)this.ent.tile).isExplored() && !Input.key_state_alt){
            return;
        }

        if (!WorldViewCamera.tile_in_fov(x, y)){
            return;
        }

        OverlaySystem.ttf.drawString(x*w,y*w-2, symbol);
    }
    
    
}
