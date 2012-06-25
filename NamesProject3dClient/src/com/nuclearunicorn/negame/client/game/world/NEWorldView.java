package com.nuclearunicorn.negame.client.game.world;

import com.nuclearunicorn.libroguelike.core.Input;
import com.nuclearunicorn.libroguelike.events.EMouseDrag;
import com.nuclearunicorn.libroguelike.events.EMouseRelease;
import com.nuclearunicorn.libroguelike.events.Event;
import com.nuclearunicorn.libroguelike.game.world.WorldModel;
import com.nuclearunicorn.libroguelike.game.world.WorldView;
import com.nuclearunicorn.libroguelike.render.TilesetRenderer;
import com.nuclearunicorn.negame.client.render.TilesetVoxelRenderer;
import com.nuclearunicorn.negame.client.render.utils.VAVoxel;
import org.lwjgl.util.Point;

/**
 * Created with IntelliJ IDEA.
 * User: Administrator
 * Date: 24.06.12
 * Time: 21:49
 * To change this template use File | Settings | File Templates.
 */
public class NEWorldView extends WorldView {

    private static float mouseXWorld = 0;
    private static float mouseYWorld = 0;

    public NEWorldView(WorldModel model) {
        super(model);
    }
    //----------------------------EVENTS SHIT-----------------------------------
    public void e_on_event(Event event){

        if (event instanceof EMouseDrag){
            EMouseDrag drag_event = (EMouseDrag)event;
            if (drag_event.type == Input.MouseInputType.RCLICK){
                TilesetVoxelRenderer.getCamera().strafeRight(drag_event.dx * 0.5f);
                TilesetVoxelRenderer.getCamera().walkForward(drag_event.dy * 0.5f);
            }
            if (drag_event.type == Input.MouseInputType.LCLICK){
                TilesetVoxelRenderer.getCamera().yaw(drag_event.dx * 0.5f);
                TilesetVoxelRenderer.getCamera().pitch(-drag_event.dy*0.5f);
            }
        }else if(event instanceof EMouseRelease){
            EMouseRelease drag_event = (EMouseRelease)event;
            if (drag_event.type == Input.MouseInputType.RCLICK){
                if (!Input.key_state_alt){
                }
            }
        }
    }

    public float getMouseXWorld() {
        return mouseXWorld;
    }

    public static void setMouseXWorld(float mouseXWorld) {
        NEWorldView.mouseXWorld = mouseXWorld;
    }

    public static float getMouseYWorld() {
        return mouseYWorld;
    }

    public static void setMouseYWorld(float mouseYWorld) {
        NEWorldView.mouseYWorld = mouseYWorld;
    }

    public static Point getSelectedTileCoord(){
        int mx = (int) (mouseXWorld / VAVoxel.VOXEL_SIZE * TilesetRenderer.TILE_SIZE);
        int my = (int) (mouseYWorld / VAVoxel.VOXEL_SIZE * TilesetRenderer.TILE_SIZE);
        return WorldView.getTileCoordPlain(mx, my);
    }
}
