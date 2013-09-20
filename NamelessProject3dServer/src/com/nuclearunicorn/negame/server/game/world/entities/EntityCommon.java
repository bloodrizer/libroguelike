package com.nuclearunicorn.negame.server.game.world.entities;

import com.nuclearunicorn.libroguelike.events.Event;
import com.nuclearunicorn.libroguelike.events.IEventListener;
import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.game.ent.EntityActor;
import com.nuclearunicorn.libroguelike.game.items.BaseItem;
import com.nuclearunicorn.libroguelike.game.items.ItemEnt;
import com.nuclearunicorn.negame.client.render.ASCIISpriteEntityRenderer;
import org.newdawn.slick.Color;

import java.util.List;

/**
 * A tweaked version of entity with specific settings for NE
 * This entity can be used both by client and server
 *
 * Please do not use EntityActor as it is deprecated
 */


public class EntityCommon extends Entity implements IEventListener {

    boolean blockSight = true;


    //this thing should be obviously in the render
    String character;
    Color color;

    //-----------------------------
    public String getCharacter() {
        return character;
    }

    public void setCharacter(String character) {
        this.character = character;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }
    //-----------------------------


    public boolean isBlockSight() {
        return blockSight;
    }

    public void setBlockSight(boolean blockSight) {
        this.blockSight = blockSight;
    }

    @Override
    public void e_on_event(Event event) {
    }

    public void die(Entity killer) {
        this.trash();

        for (BaseItem item: (List<BaseItem>)getContainer().getItems()){
            dropItem(item);
        }
    }
    
    private void dropItem(BaseItem item){

        ItemEnt itemEnt = new ItemEnt();
        itemEnt.set_item( item );
        itemEnt.setLayerId(this.getLayerId());

        itemEnt.setRenderer( new ASCIISpriteEntityRenderer("i", Color.lightGray) );
        itemEnt.setEnvironment( this.getEnvironment());

        itemEnt.set_blocking(false);

        itemEnt.spawn( this.origin );        
    }


    public void kill(EntityActor rlOwner) {
    }

    /**
     * Generally entity is a common
     */
    public void initClient(){
        //TODO: check if client context is applicable
    }
}
