package com.nuclearunicorn.negame.client.render.overlays;

import com.nuclearunicorn.libroguelike.game.GameEnvironment;
import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.render.overlay.OverlaySystem;
import com.nuclearunicorn.negame.client.NEGame;
import com.nuclearunicorn.negame.client.NEGameClient;
import com.nuclearunicorn.negame.client.game.world.NEWorldView;
import com.nuclearunicorn.negame.client.render.VoxelEntityRenderer;
import com.nuclearunicorn.negame.common.api.IServer;
import org.lwjgl.util.Point;
import org.newdawn.slick.Color;

import java.util.List;

/**
 * @author bloodrizer
 */
public class NEDebugOverlay {
    public static float wx = 0;
    public static float wy = 0;
    public static float wz = 0;

    static VoxelEntityRenderer debugVoxelRenderer = new VoxelEntityRenderer();

    public static void render(){
        OverlaySystem.ttf.drawString(10, 90, "Mouse coord traced @ " + wx + "," + wy + "," + wz , Color.white);

        Point tileCoord = NEWorldView.getSelectedTileCoord();
        OverlaySystem.ttf.drawString(10, 110, "Tile traced @ " + tileCoord.getX() + "," + tileCoord.getY() , Color.white);

        NEGame game = NEGameClient.getNEGame();
        IServer server = game.getAttachedServerSession();
        if (server != null){
            GameEnvironment serverEnv = server.getWorldEnvironment();

            List<Entity> debugRenderList = (List<Entity>) serverEnv.getEntityManager().getList(NEWorldView.get_zindex()).clone();
            for (Entity serverEnt: debugRenderList){

                debugVoxelRenderer.set_entity(serverEnt);
                debugVoxelRenderer.render();

            }
        }   else {
            OverlaySystem.ttf.drawString(10, 10, "Server is detached, no telemetry signal");
        }
    }
}
