package com.nuclearunicorn.serialkiller.game.world.entities;


import com.nuclearunicorn.serialkiller.render.AsciiEntRenderer;
import com.nuclearunicorn.serialkiller.render.LightMap;

public class EntityDoor extends EntityFurniture {

    boolean locked = false;

    public void lock() {
        locked = true;

        ((AsciiEntRenderer)this.render).symbol = "+";
        this.set_blocking(true);
        LightMap.invalidate();  //a shut door casts a shadow, drop the cached light
    }

    public void unlock() {
        locked = false;

        ((AsciiEntRenderer)this.render).symbol = "/";
        this.set_blocking(false);
        LightMap.invalidate();
    }
}
