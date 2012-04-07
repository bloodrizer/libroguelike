package com.nuclearunicorn.serialkiller.game.world.entities;

import com.nuclearunicorn.libroguelike.game.combat.Combat;
import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.serialkiller.game.combat.RLCombat;
import com.nuclearunicorn.serialkiller.render.AsciiEntRenderer;
import com.nuclearunicorn.serialkiller.render.RLMessages;
import org.newdawn.slick.Color;

/**
 * Basic furniture entity for tables, doors, etc
 */
public class EntityFurniture extends EntityRLActor {
    
    public EntityFurniture(){
        Combat combat = new RLCombat();
        set_combat(combat);    
    }

    @Override
    public void die(Entity killer) {
        //super.die(killer);

        ((AsciiEntRenderer)this.render).symbol = "X";
        RLMessages.message(name + " is broken", Color.lightGray);

        set_blocking(false);
    }
}
