package com.nuclearunicorn.serialkiller.game.world.entities;

import com.nuclearunicorn.libroguelike.game.combat.BasicCombat;
import com.nuclearunicorn.libroguelike.game.combat.Combat;
import com.nuclearunicorn.libroguelike.game.ent.Entity;

/**
 * Basic furniture entity for tables, doors, etc
 */
public class EntFurniture extends Entity {
    
    public EntFurniture(){
        Combat combat = new BasicCombat();
        set_combat(combat);    
    }

}
