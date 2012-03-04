package com.nuclearunicorn.serialkiller.game.world;

import com.nuclearunicorn.libroguelike.game.player.Player;
import com.nuclearunicorn.libroguelike.game.world.WorldModel;
import com.nuclearunicorn.libroguelike.game.world.WorldTile;
import com.nuclearunicorn.libroguelike.game.world.layers.WorldLayer;
import rlforj.los.ILosBoard;

import java.util.HashMap;

public class RLWorldModel extends WorldModel implements ILosBoard {
    
    //private List<RLTile> fovTiles = new ArrayList<RLTile>();

    /**
        Reset fov-checked tiles.
        Since iterating them all can be heavy operation,
        we will store fov-visited tiles and reset them directly from this list
     */
    /*public void resetFov(){
        for(RLTile tile: fovTiles){
            tile.setFovChecked(false);
        }
    }*/
    
    @Override
    public boolean contains(int x, int y) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isObstacle(int x, int y) {
        RLTile tile = getRLTile(x, y);
        return tile.isWall() || tile.is_blocked();
    }

    @Override
    public void visit(int x, int y) {

    }
    
    private RLTile getRLTile(int x, int y){
        
        WorldLayer layer = ((HashMap<Integer,WorldLayer>)this.getLayers()).get(Player.get_zindex());
        WorldTile tile = layer.get_tile(x,y);

        if(tile instanceof RLTile){
            return (RLTile)tile;
        }
        return null;
    }
}
