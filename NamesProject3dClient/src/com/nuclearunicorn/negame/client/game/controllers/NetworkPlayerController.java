package com.nuclearunicorn.negame.client.game.controllers;

import com.nuclearunicorn.libroguelike.core.Input;
import com.nuclearunicorn.libroguelike.events.EMouseClick;
import com.nuclearunicorn.libroguelike.events.Event;
import com.nuclearunicorn.libroguelike.game.ent.controller.PlayerController;
import com.nuclearunicorn.libroguelike.game.world.WorldView;
import com.nuclearunicorn.negame.client.game.world.NEWorldView;
import com.nuclearunicorn.negame.client.render.utils.Raycast;
import com.nuclearunicorn.negame.client.render.utils.VAVoxel;
import org.lwjgl.util.Point;

import java.nio.FloatBuffer;

/**
 * @author bloodrizer
 * Mediator between player ent controller and network layer
 */
public class NetworkPlayerController extends PlayerController {

    @Override
    public void e_on_event(Event event) {

        if (event.is_dispatched()) {
            return;
        }
        super.e_on_event(event);

        if (event instanceof EMouseClick){
            EMouseClick clickEvent = (EMouseClick) event;
            if (clickEvent.type == Input.MouseInputType.LCLICK){
                /*Calculating coords outside of voxel's render cycle can produce inaccurate results*/
                /*It's recommend to calculate tile coord based on pre-defined mouse position*/

                Point tileCoord = NEWorldView.getSelectedTileCoord();
                this.set_destination(tileCoord);
            }
        }

    }
}
