package com.nuclearunicorn.serialkiller.game.bodysim;

import com.nuclearunicorn.libroguelike.game.combat.Damage;
import com.nuclearunicorn.libroguelike.game.ent.EntityNPC;
import com.nuclearunicorn.serialkiller.render.RLMessages;
import org.newdawn.slick.Color;

/**
 */
public class Limb {
    String name = "none";
    Boolean damaged = false;
    float dmgMultiply = 1;
    EntityNPC owner = null;

    public Limb(){

    }

    public Limb(String name, float dmgMultipy){
        this.name = name;
        this.dmgMultiply = dmgMultipy;
    }

    public void takeDamage(Damage damage){
        if (!damaged){
            damaged = true;
            RLMessages.message( owner.getName() + "'s" + name + " is damaged", Color.magenta);
        }
    }

    public String getName() {
        return name;
    }
}
