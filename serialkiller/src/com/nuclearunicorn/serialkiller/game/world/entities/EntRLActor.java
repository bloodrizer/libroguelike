package com.nuclearunicorn.serialkiller.game.world.entities;

import com.nuclearunicorn.libroguelike.events.Event;
import com.nuclearunicorn.libroguelike.events.IEventListener;
import com.nuclearunicorn.libroguelike.game.ent.EntityActor;
import com.nuclearunicorn.serialkiller.render.RLMessages;
import org.newdawn.slick.Color;


public class EntRLActor extends EntityActor implements IEventListener {

    boolean blockSight = true;

    public boolean isBlockSight() {
        return blockSight;
    }

    public void setBlockSight(boolean blockSight) {
        this.blockSight = blockSight;
    }

    public void describe(){
        RLMessages.message("You see " + this.name, Color.lightGray);
    }

    @Override
    public void e_on_event(Event event) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
