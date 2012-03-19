package com.nuclearunicorn.serialkiller.game.world.entities;

import com.nuclearunicorn.libroguelike.events.Event;
import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.game.items.EquipContainer;
import com.nuclearunicorn.serialkiller.game.bodysim.BodySimulation;
import com.nuclearunicorn.serialkiller.generators.Block;
import com.nuclearunicorn.serialkiller.render.AsciiEntRenderer;
import com.nuclearunicorn.serialkiller.render.RLMessages;
import org.newdawn.slick.Color;

/**
 * Created by IntelliJ IDEA.
 * User: dpopov
 * Date: 07.03.12
 * Time: 15:10
 * To change this template use File | Settings | File Templates.
 */
public class EntityRLHuman extends EntRLActor {

    public enum Sex {
        MALE,
        FEMALE
    }

    enum Race {
        WHITE,
        BLACK,
        ASIAN
    }

    enum Religion {
        ATHEIST
    }

    Sex sex = Sex.MALE;
    int age = 30;
    Race race = Race.WHITE;
    Religion religion = Religion.ATHEIST;

    BodySimulation bodysim;

    public EquipContainer equipment = new EquipContainer();
    //public EquipContainer inventory;  use container instead


    public void setSex(Sex sex){
        this.sex = sex;
    }

    public Sex getSex(){
        return sex;
    }

    public EntityRLHuman(){
        super();

        setBodysim(new BodySimulation());
    }

    //TODO: apartment link
    Block apartment;

    
    public Block getApartment(){
        return apartment;
    }

    public void setApartment(Block apt){
        apartment = apt;
    }
    
    public String getModel(){
        return "@";
    }

    /*
        Since this game is turn-based, entity is allways awake

        TODO: fix possible bug with awakeness
     */
    @Override
    public boolean is_awake(long current_time_ms){
        return true;
    }

    @Override
    public void think(){
        super.think();

        bodysim.think();
    }

    @Override
    public void die(Entity killer){
        //super.die(killer);

        ((AsciiEntRenderer)this.render).symbol = "%";
        RLMessages.message(name + " has died", Color.red);

        set_blocking(false);
    }

    @Override
    public void describe(){
        super.describe();

        RLMessages.message(name + " is " + sex + ", age " + age, Color.lightGray);
        RLMessages.message(name + " is " + race + ", " + religion, Color.lightGray);

        //if (this.apt)

        int maxHp;
        if (combat != null){
            maxHp = combat.get_max_hp();

            if (combat.get_hp() >= maxHp){
                RLMessages.message(name + " is healthy", Color.lightGray);
            } else if (combat.get_hp() >= maxHp * 0.5){
                RLMessages.message(name + " is injured", Color.lightGray);
            } else if (combat.get_hp() > 0 ){
                RLMessages.message(name + " is near death", Color.lightGray);
            } else if (combat.get_hp() <= 0 ){
                RLMessages.message(name + " is dead", Color.lightGray);
            }
        }


    }

    public BodySimulation getBodysim() {
        return bodysim;
    }

    public void setBodysim(BodySimulation bodysim) {
        this.bodysim = bodysim;
        bodysim.setOwner(this);
    }

    @Override
    public void e_on_event(Event event) {
        if (this.ai != null){
            ai.e_on_event(event);
        }
    }
}
