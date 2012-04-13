package com.nuclearunicorn.serialkiller.game.bodysim;

import com.nuclearunicorn.libroguelike.game.combat.Damage;
import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.serialkiller.game.combat.RLCombat;
import com.nuclearunicorn.serialkiller.game.world.RLTile;
import com.nuclearunicorn.serialkiller.game.world.entities.EntityRLHuman;
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
    boolean fainted = false;

    int stun_duration = 0;

    int bloodLevel = 100;

    Entity bleedInflictor = null;
    EntityRLHuman owner = null;

    List<Limb> limbs = new ArrayList<Limb>();
    
    float stamina = 100f;
    float hunger = 100f;
    float bloodlust = 0f;
    
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

    public void setHunger(float hunger){
        this.hunger = hunger;

        if (this.hunger<0){
            this.hunger = 0;
        }

        if (this.hunger>100){
            this.hunger = 100;
        }
    }

    public void restoreHunger(float amt){
        setHunger(hunger + amt);
    }

    public void setStamina(float hunger){
        this.stamina = hunger;

        if (this.stamina<0){
            this.stamina = 0;
        }

        if (this.stamina>100){
            this.stamina = 100;
        }
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
            setStamina(stamina + 2.5f);

            stun_duration -= 1;

            if (stun_duration <= 0){
                stunned = false;
            }
        }

        if (fainted) {
            fainted = false;
        }

        setHunger(hunger-0.05f);
        setStamina(stamina - 0.1f);
        setBloodlust(bloodlust + 0.05f);

        if (stamina <= 20){         //stamina < 20% - you start skipping turns
            if ( (int)(Math.random()*100) <= 10 ){        //10% chance to skip turn
                setFainted(true);
                if (owner.isPlayerEnt()){
                    RLMessages.message("You feel tired", Color.orange);
                }
            }
        }
        if (stamina == 0){
            stunned = true;
            stun_duration = 10;

            if (owner.isPlayerEnt()){
                RLMessages.message("You are unconscious", Color.orange);
            }
        }
    }

    public void setOwner(EntityRLHuman owner) {
        this.owner = owner;
    }

    public List<Limb> getLimbs() {
        return limbs;
    }

    public float getStamina() {
        return stamina;
    }

    public float getHunger() {
        return hunger;
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
        return bloodlust;
    }

    public void setBloodlust(float bloodlust) {
        this.bloodlust = bloodlust;
        if (this.bloodlust > 100f){
            this.bloodlust = 100f;
        }
        if (this.bloodlust < 0f){
            this.bloodlust = 0f;
        }
    }

    public void depleteBloodlust(float amt) {
        this.setBloodlust(bloodlust - amt);
    }
}
