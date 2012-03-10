package com.nuclearunicorn.serialkiller.game.world.entities;

import com.nuclearunicorn.libroguelike.game.ent.EntityActor;
import com.nuclearunicorn.serialkiller.render.RLMessages;
import org.newdawn.slick.Color;


public class EntRLActor extends EntityActor {
    public void describe(){
        RLMessages.message("You see " + this.name, Color.lightGray);
    }
}
