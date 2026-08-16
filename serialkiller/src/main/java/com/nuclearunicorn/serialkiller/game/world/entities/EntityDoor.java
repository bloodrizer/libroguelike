package com.nuclearunicorn.serialkiller.game.world.entities;


import com.nuclearunicorn.serialkiller.game.sound.SoundConfig;
import com.nuclearunicorn.serialkiller.game.world.RLTile;
import com.nuclearunicorn.serialkiller.render.AsciiEntRenderer;
import com.nuclearunicorn.serialkiller.render.LightMap;

public class EntityDoor extends EntityFurniture {

    boolean locked = false;

    public void lock() {
        locked = true;

        ((AsciiEntRenderer)this.render).symbol = "+";
        this.set_blocking(true);
        LightMap.invalidate();  //a shut door casts a shadow, drop the cached light
        applySoundLoss();
    }

    public void unlock() {
        locked = false;

        ((AsciiEntRenderer)this.render).symbol = "/";
        this.set_blocking(false);
        LightMap.invalidate();
        applySoundLoss();
    }

    public boolean isLocked() {
        return locked;
    }

    /**
     * Whether this door is standing open.
     *
     * <p>{@code locked} is this class's only state and it is really "shut": {@link #lock}
     * draws "+", blocks movement and casts a shadow, {@link #unlock} draws "/", lets people
     * walk through and lets light past. So an unlocked door is an <i>open</i> door — which
     * is also why most doors in a generated town are open, {@code punchDoor} only passes
     * {@code locked=true} for bank/office entrances and vault rooms.
     */
    public boolean isOpen() {
        return !locked;
    }

    /**
     * Push this door's transmission loss down onto its tile, so the acoustic flood can
     * read it as a plain array value (SOUND_DESIGN.md 9.2).
     *
     * <p>An open door is a hole in a wall and has to sound like one — anything else means
     * two people talking either side of a doorway cannot hear each other. A shut one costs
     * the same whether or not it is locked: mass is what stops sound, and the lock only
     * decides whether you can walk through. If reinforced doors ever become their own
     * object, that is when they earn their own constant.
     */
    public void applySoundLoss() {
        if (!(tile instanceof RLTile)) {
            return;     //not spawned yet; punchDoor calls lock()/unlock() after spawn
        }
        ((RLTile) tile).setSoundLoss(
                locked ? SoundConfig.TL_DOOR_SHUT : SoundConfig.TL_DOOR_OPEN);
    }
}
