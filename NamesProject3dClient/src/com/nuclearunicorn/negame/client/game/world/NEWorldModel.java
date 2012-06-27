package com.nuclearunicorn.negame.client.game.world;

import com.nuclearunicorn.libroguelike.events.Event;
import com.nuclearunicorn.libroguelike.game.world.WorldModel;
import com.nuclearunicorn.libroguelike.game.world.layers.WorldLayer;
import rlforj.los.ILosBoard;

/**
 * Created with IntelliJ IDEA.
 * User: Administrator
 * Date: 21.06.12
 * Time: 0:19
 * To change this template use File | Settings | File Templates.
 */
public class NEWorldModel extends WorldModel implements ILosBoard {

    public NEWorldModel(int layersCount) {
        this.LAYER_COUNT = layersCount;

        for (int i = 0; i< LAYER_COUNT; i++ ){
            WorldLayer layer = new NEWorldLayer();    //TODO: use NEWorldLayer
            layer.set_zindex(i);
            layer.setModel(this);
            worldLayers.put(i, layer);
        }
    }

    @Override
    public boolean contains(int x, int y) {
        return false;
    }

    @Override
    public boolean isObstacle(int x, int y) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void visit(int x, int y) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void resetFov() {
        //To change body of created methods use File | Settings | File Templates.
    }

    @Override
    public void e_on_event(Event event) {
        super.e_on_event(event);    //To change body of overridden methods use File | Settings | File Templates.
    }
}
