package com.nuclearunicorn.serialkiller.game.bodysim;

import com.nuclearunicorn.libroguelike.game.combat.Damage;
import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.serialkiller.game.combat.RLCombat;
import com.nuclearunicorn.serialkiller.game.world.RLTile;
import com.nuclearunicorn.serialkiller.game.world.entities.EntityRLHuman;
import com.nuclearunicorn.serialkiller.render.RLMessages;
import org.newdawn.slick.Color;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * simple body simulation. Bleeding, limb loss, etcetra
 */
public class BodySimulation {
    boolean bleeding = false;
    boolean stunned = false;
    boolean fainted = false;

    int stun_duration = 0;

    int bloodLevel = 100;

    Entity bleedInflictor = null;
    EntityRLHuman owner = null;

    List<Limb> limbs = new ArrayList<Limb>();

    //TODO: replace with a list of uniform float attributes
    Map<String, Float> attributes = new HashMap<String, Float>(4);
    {
        attributes.put("stamina",100f);
        attributes.put("hunger",100f);
        attributes.put("bloodlust",0.0f);
        attributes.put("libido",0f);
    }
    
    boolean infected = false;

    public BodySimulation(){
        limbs.add(new Limb("head",3));
        limbs.add(new Limb("left arm",1));
        limbs.add(new Limb("right arm",1));
        limbs.add(new Limb("left leg",1));
        limbs.add(new Limb("right leg",1));
        limbs.add(new Limb("left eye",0.2f));
        limbs.add(new Limb("right eye",0.2f));
    }

    public boolean isBleeding(){
        return bleeding;
    }

    public boolean isStunned(){
        return stunned;
    }

    public void setAttribute(String attr, float value){

        if (value<0){
            value = 0;
        }

        if (value>100){
            value = 100;
        }

        attributes.put(attr, value);
    }

    public void adjustAttribute(String key, float amt){
        Float attrVal = attributes.get(key);
        setAttribute(key, attrVal + amt);
    }

    public void restoreHunger(float amt){
        adjustAttribute("hunger", amt);
    }

    public void takeDamage(Damage damage){
        
        //System.out.println("Bosysim: taking damage of type '" + damage.type.name()+"'");
        
        switch (damage.type) {
            case DMG_CUT:
                if (!bleeding){
                    RLMessages.message(owner.getName() + " is bleeding", Color.red);
                }
                bleeding = true;
                bleedInflictor = damage.inflictor;

                //TODO: add different cut severity

            break;
            case DMG_GENERIC:
            break;

            case DMG_BLUNT:
            case DMG_NONLETHAL:

                RLTile tile = (RLTile)owner.tile;
                float bloodAmt = tile.getBloodAmt();
                tile.setBloodAmt(bloodAmt + 0.35f);


                if (damage.inflictor.get_combat() == null){
                    return;
                }

                RLCombat inflictorCombat = ((RLCombat)damage.inflictor.get_combat());

                int stunChance = inflictorCombat.getEquipBonus("stun_chance");
                int chance = (int)(Math.random()*100);

                System.out.println("stun chance: "+chance+"/"+stunChance);

                if (chance < stunChance){
                    stunned = true;
                    stun_duration = inflictorCombat.getEquipBonus("stun_duration");

                    RLMessages.message(owner.getName() + " is stunned", Color.orange);
                }
            break;
        }
    }

    public void think(){

        if (bleeding){
            bloodLevel -= 5;

            if (bloodLevel <=0 && owner.get_combat().is_alive()){
                Damage damage = new Damage(10, Damage.DamageType.DMG_BLOODLOSS);
                damage.set_inflictor(bleedInflictor);

                owner.get_combat().take_damage(damage);
            }


            if (owner.get_combat().is_alive()){

                //make blood more like drops rather than trail
                if (Math.random()*100 < 75){
                    RLTile tile = (RLTile)owner.tile;
                    float bloodAmt = tile.getBloodAmt();
                    tile.setBloodAmt(bloodAmt + 0.5f);
                }

            }else{
                for (int i = owner.origin.getX() - 1; i <= owner.origin.getX() + 1; i++)
                    for (int j = owner.origin.getY() - 1; j <= owner.origin.getY() + 1; j++) {
                        RLTile tile = (RLTile)owner.getLayer().get_tile(i,j);

                        if (tile != null){
                            float bloodAmt = tile.getBloodAmt();
                            tile.setBloodAmt(bloodAmt + 0.25f);
                        }
                    }
            }

            if (bloodLevel <= - 50){
                bleeding = false;
            }
        }
        if (stunned) {

            //lying unconcious will restore a bit of stamina
            adjustAttribute("stamina", 2.5f);

            stun_duration -= 1;

            if (stun_duration <= 0){
                stunned = false;
            }
        }

        if (fainted) {
            fainted = false;
        }

        adjustAttribute("hunger",-0.05f);
        adjustAttribute("stamina", -0.1f);
        adjustAttribute("bloodlust", 0.05f);
        adjustAttribute("libido", 0.5f);

        if (getStamina() <= 20){         //stamina < 20% - you start skipping turns
            if ( (int)(Math.random()*100) <= 10 ){        //10% chance to skip turn
                setFainted(true);
                if (owner.isPlayerEnt()){
                    RLMessages.message("You feel tired", Color.orange);
                }
            }
        }
        if (getStamina() == 0){
            stunned = true;
            stun_duration = 10;

            if (owner.isPlayerEnt()){
                RLMessages.message("You are unconscious", Color.orange);
            }
        }
    }

    public float getAttribute(String attr) {
        return attributes.get(attr);
    }

    public void setOwner(EntityRLHuman owner) {
        this.owner = owner;
    }

    public List<Limb> getLimbs() {
        return limbs;
    }

    public float getStamina() {
        return getAttribute("stamina");
    }

    public float getHunger() {
        return getAttribute("hunger");
    }

    public boolean isFainted() {
        return fainted;
    }

    public void setFainted(boolean fainted) {
        this.fainted = fainted;
    }

    public void setInfected(boolean infected) {
        this.infected = infected;
    }

    public boolean isInfected() {
        return infected;
    }

    public float getBloodlust() {
        return getAttribute("bloodlust");
    }


    public void depleteBloodlust(float amt) {
        this.adjustAttribute("bloodlust", - amt);
    }
}
