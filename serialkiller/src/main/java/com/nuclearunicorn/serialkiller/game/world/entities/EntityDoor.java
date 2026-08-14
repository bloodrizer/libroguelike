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
     * Push this door's transmission loss down onto its tile, so the acoustic flood can
     * read it as a plain array value (SOUND_DESIGN.md 9.2).
     *
     * <p>An unlocked door is still a <i>shut</i> door as far as sound is concerned. There
     * is no open/closed state in this class — {@code unlocked} means "passable", and
     * everyone walks through without ever opening anything — so treating unlocked as open
     * would make every interior in town acoustically roofless. Shut is the right default;
     * a real open/close action can lower it later.
     */
    public void applySoundLoss() {
        if (!(tile instanceof RLTile)) {
            return;     //not spawned yet; punchDoor calls lock()/unlock() after spawn
        }
        ((RLTile) tile).setSoundLoss(
                locked ? SoundConfig.TL_DOOR_LOCKED : SoundConfig.TL_DOOR_SHUT);
    }
}
