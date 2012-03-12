package com.nuclearunicorn.serialkiller.game.world.entities;


import com.nuclearunicorn.serialkiller.render.AsciiEntRenderer;

public class EntDoor extends EntFurniture {

    boolean locked = false;

    public void lock() {
        locked = true;

        ((AsciiEntRenderer)this.render).symbol = "+";
        this.set_blocking(true);
    }

    public void unlock() {
        locked = false;

        ((AsciiEntRenderer)this.render).symbol = "/";
        this.set_blocking(false);
    }
}
