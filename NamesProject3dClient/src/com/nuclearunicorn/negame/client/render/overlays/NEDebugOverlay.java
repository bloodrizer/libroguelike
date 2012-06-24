package com.nuclearunicorn.negame.client.render.overlays;

import com.nuclearunicorn.libroguelike.render.overlay.OverlaySystem;
import com.nuclearunicorn.negame.client.game.world.NEWorldView;
import org.lwjgl.util.Point;
import org.newdawn.slick.Color;

/**
 * Created with IntelliJ IDEA.
 * User: Administrator
 * Date: 25.06.12
 * Time: 0:09
 * To change this template use File | Settings | File Templates.
 */
public class NEDebugOverlay {
    public static float wx = 0;
    public static float wy = 0;
    public static float wz = 0;

    public static void render(){
        OverlaySystem.ttf.drawString(10, 90, "Mouse coord traced @ " + wx + "," + wy + "," + wz , Color.white);

        Point tileCoord = NEWorldView.getSelectedTileCoord();
        OverlaySystem.ttf.drawString(10, 110, "Tile traced @ " + tileCoord.getX() + "," + tileCoord.getY() , Color.white);
    }
}
