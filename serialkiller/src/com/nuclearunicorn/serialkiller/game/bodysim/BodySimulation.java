package com.nuclearunicorn.serialkiller.game.bodysim;

import com.nuclearunicorn.libroguelike.game.combat.Damage;
import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.game.ent.EntityNPC;
import com.nuclearunicorn.serialkiller.render.RLMessages;
import org.newdawn.slick.Color;

import java.util.ArrayList;
import java.util.List;

/**
 * simple body simulation. Bleeding, limb loss, etcetra
 */
public class BodySimulation {
    boolean bleeding = false;
    boolean stunned = false;
    int stun_duration = 0;

    int bloodLevel = 100;

    Entity bleedInflictor = null;
    EntityNPC owner = null;

    List<Limb> limbs = new ArrayList<Limb>();
    
    int stamina = 100;
    int hunger = 100;

    public BodySimulation(){
        limbs.add(new Limb("head",3));
        limbs.add(new Limb("left arm",1));
        limbs.add(new Limb("right arm",1));
        limbs.add(new Limb("left leg",1));
        limbs.add(new Limb("right leg",1));
        limbs.add(new Limb("left eye",0.2f));
        limbs.add(new Limb("right eye",0.2f));
    }

    public void setHunger(int hunger){
        this.hunger = hunger;

        if (this.hunger<0){
            this.hunger = 0;
        }

        if (this.hunger>100){
            this.hunger = 100;
        }
    }


    public void takeDamage(Damage damage){
        switch (damage.type) {
            case DMG_CUT:
                if (!bleeding){
                    RLMessages.message(owner.getName() + " is bleeding", Color.red);
                }
                bleeding = true;
                bleedInflictor = damage.inflictor;
            break;
            case DMG_GENERIC:
            break;
            case DMG_BLUNT:
                //TODO: add stun handling
            break;
        }
    }

    public void think(){

    }
}
