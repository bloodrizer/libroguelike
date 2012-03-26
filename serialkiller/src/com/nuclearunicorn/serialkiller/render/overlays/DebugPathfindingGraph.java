package com.nuclearunicorn.serialkiller.render.overlays;

import com.nuclearunicorn.libroguelike.core.Input;
import com.nuclearunicorn.libroguelike.core.client.ClientGameEnvironment;
import com.nuclearunicorn.libroguelike.game.world.WorldView;
import com.nuclearunicorn.libroguelike.render.overlay.OverlaySystem;
import com.nuclearunicorn.serialkiller.utils.pathfinder.adaptive.AdaptiveNode;
import com.nuclearunicorn.serialkiller.utils.pathfinder.adaptive.AdaptivePath;
import com.nuclearunicorn.serialkiller.utils.pathfinder.adaptive.AdaptivePathfinder;
import org.lwjgl.util.Point;
import org.newdawn.slick.Color;

/**
 */
public class DebugPathfindingGraph {

    public static void debugAdaptiveGraph(){
        if (!Input.key_state_alt){
            return;
        }

        Point tileFrom = new Point(0,0);
        Point tileTo = new Point(0,0);

        for (AdaptiveNode node: AdaptivePathfinder.nodes){
            for (AdaptivePath path: node.nb){

                tileFrom.setLocation(node.point);
                tileFrom = ClientGameEnvironment.getWorldLayer(WorldView.get_zindex()).tile_map.local2world(tileFrom);

                tileTo.setLocation(path.to.point);
                tileTo = ClientGameEnvironment.getWorldLayer(WorldView.get_zindex()).tile_map.local2world(tileTo);

                OverlaySystem.drawLine(tileFrom, tileTo, Color.orange);
            }
        }
    }
}
