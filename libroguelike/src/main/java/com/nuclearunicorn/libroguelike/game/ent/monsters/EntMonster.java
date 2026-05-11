/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.game.ent.monsters;

import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.game.ent.EntityNPC;
import com.nuclearunicorn.libroguelike.game.items.BaseItem;

/**
 * Monster is a basic class for eny aggressive mob.
 * Note that it is a temporary workaround for chacking if enemy is aggressive.
 */
public class EntMonster extends EntityNPC {
    public void drop_loot(Entity killer, String item_type, int count, int rate){
        if(killer==null || killer.getContainer() == null){
            return;
        }
        int chance = (int)(Math.random()*100);

        if (chance<rate){
            killer.getContainer().add_item(
                BaseItem.produce(item_type, count)
            );
        }
        
    }
}
