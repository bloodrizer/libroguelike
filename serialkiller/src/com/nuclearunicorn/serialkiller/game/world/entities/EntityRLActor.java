package com.nuclearunicorn.serialkiller.game.world.entities;

import com.nuclearunicorn.libroguelike.events.Event;
import com.nuclearunicorn.libroguelike.events.IEventListener;
import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.game.ent.EntityActor;
import com.nuclearunicorn.libroguelike.game.items.BaseItem;
import com.nuclearunicorn.libroguelike.game.items.ItemEnt;
import com.nuclearunicorn.serialkiller.render.AsciiEntRenderer;
import com.nuclearunicorn.serialkiller.render.RLMessages;
import org.newdawn.slick.Color;

import java.util.List;


public class EntityRLActor extends EntityActor implements IEventListener {

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
    }

    @Override
    public void die(Entity killer) {
        //super.die(killer);

        for (BaseItem item: (List<BaseItem>)getContainer().getItems()){
            dropItem(item);
        }
    }
    
    private void dropItem(BaseItem item){

        ItemEnt itemEnt = new ItemEnt();
        itemEnt.set_item( item );
        itemEnt.setLayerId(this.getLayerId());

        itemEnt.setRenderer( new AsciiEntRenderer("i", Color.lightGray) );
        itemEnt.setEnvironment( this.getEnvironment());

        itemEnt.set_blocking(false);

        itemEnt.spawn( this.origin );        
    }
}
