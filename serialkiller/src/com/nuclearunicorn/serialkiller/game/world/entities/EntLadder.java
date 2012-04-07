package com.nuclearunicorn.serialkiller.game.world.entities;

import com.nuclearunicorn.libroguelike.game.ent.ActionList;
import com.nuclearunicorn.libroguelike.game.ent.BaseEntityAction;
import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.game.player.Player;
import com.nuclearunicorn.libroguelike.game.world.WorldView;
import com.nuclearunicorn.serialkiller.render.AsciiEntRenderer;

import java.util.ArrayList;

/**
 */
public class EntLadder extends EntityFurniture {

    boolean isDescLadder = false;

    public EntLadder(){
        super();
    }

    public void setDescending(boolean isDescLadder) {
        this.isDescLadder = isDescLadder;

        if (this.isDescLadder){
            ((AsciiEntRenderer)this.render).symbol = "<";
        }else{
            ((AsciiEntRenderer)this.render).symbol = ">";
        }

        this.set_blocking(false);
    }

    @Override
    public ArrayList get_action_list() {
        //return super.get_action_list();

         class ActionChangeLayer extends BaseEntityAction {
            protected void changeLayer(int layerFromID, int layerToID){
                Player.set_zindex(layerToID);
                env.getEntityManager().remove_entity(Player.get_ent(), layerFromID);
                env.getEntityManager().add(Player.get_ent(), Player.get_zindex());

                WorldView.set_zindex(Player.get_zindex());  //the fuck is that?
            }
        }

        class ActionDescend extends ActionChangeLayer {
            @Override
            public void execute() {
                if (assert_range()){
                    int layerFromID = Player.get_zindex();
                    changeLayer(layerFromID, layerFromID+1);
                }
            }
        }

        class ActionAscend extends ActionChangeLayer {
            @Override
            public void execute() {
                if (assert_range()){
                    int layerFromID = Player.get_zindex();
                    changeLayer(layerFromID, layerFromID-1);
                }
            }
        }


        ActionList<Entity> list = new ActionList();
        list.set_owner(this);
        if (isDescLadder){
            list.add_action(new ActionDescend(),"Go down");
        }else{
            list.add_action(new ActionAscend(),"Go up");
        }


        return list.get_action_list();
    }
}
