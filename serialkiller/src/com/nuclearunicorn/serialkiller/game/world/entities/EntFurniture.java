package com.nuclearunicorn.serialkiller.game.world.entities;

import com.nuclearunicorn.libroguelike.game.combat.Combat;
import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.serialkiller.game.combat.RLCombat;

/**
 * Basic furniture entity for tables, doors, etc
 */
public class EntFurniture extends Entity {
    
    public EntFurniture(){
        Combat combat = new RLCombat();
        set_combat(combat);    
    }

}
