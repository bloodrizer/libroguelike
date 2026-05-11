/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.game.ent.monsters;

import com.nuclearunicorn.libroguelike.game.combat.BasicCombat;
import com.nuclearunicorn.libroguelike.game.combat.Damage;
import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.game.ent.controller.MobController;
import com.nuclearunicorn.libroguelike.game.world.WorldTimer;
import com.nuclearunicorn.libroguelike.render.EntityRenderer;
import com.nuclearunicorn.libroguelike.render.NPCRenderer;

/**
 *
 * @author Administrator
 */
public class Zombie extends EntMonster {
     @Override
     public EntityRenderer build_render(){
        NPCRenderer __render = (NPCRenderer)(super.build_render());
        __render.set_texture("zombie.png");


        return __render;
    }

    public Zombie(){
        MobController __controller = new MobController();
        __controller.NEXT_FRAME_DELAY = 200;
        __controller.MOVE_SPEED = 0.02f;

        set_controller(__controller);
        set_combat(new BasicCombat());

        set_blocking(true);

        
    }

    @Override
     public void think(){
         super.think();
         
         if(!WorldTimer.is_night()){
             combat.take_damage(new Damage(1, Damage.DamageType.DMG_FIRE));
         }

         if ((int)(Math.random()*100) < 3){
             say_message("Braaains!");
         }
         
         sleep(500);
     }

    @Override
    public void die(Entity killer){
        drop_loot(killer,"bone",1,20);
        
        int coin_count = (int)(Math.random()*5);
        drop_loot(killer,"copper_coin",1,10);
    }
}
