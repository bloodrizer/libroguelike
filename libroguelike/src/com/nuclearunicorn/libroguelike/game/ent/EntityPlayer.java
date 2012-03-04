/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.game.ent;

import com.nuclearunicorn.libroguelike.events.ENotificationMessage;
import com.nuclearunicorn.libroguelike.game.combat.BasicCombat;
import com.nuclearunicorn.libroguelike.game.combat.Combat;
import com.nuclearunicorn.libroguelike.game.world.layers.WorldLayer;
import com.nuclearunicorn.libroguelike.render.EntityRenderer;
import com.nuclearunicorn.libroguelike.render.NPCRenderer;

/**
 *  Entity, representing player on a game map
 *
 * Note that due to the buggy player ent checking, this class
 * must be used *ONLY!!!!* for main player instancing
 *
 */
public class EntityPlayer extends EntityNPC {

    public EntityPlayer(){

        Combat __combat = new BasicCombat();
        __combat.set_hp(500);
        

        set_blocking(true);
        set_combat(__combat);

        //inventory shit - debug only
        //BaseItem branch = BaseItem.produce("branch", 20);
        /*this.container.add_item(
                BaseItem.produce("branch", 20)
        );
        this.container.add_item(
                BaseItem.produce("stone", 20)
        );
        this.container.add_item(
                BaseItem.produce("stone_axe", 1)
        );
        this.container.add_item(
                BaseItem.produce("wood_block", 64)
        );
        this.container.add_item(
                BaseItem.produce("wood_wall", 64)
        );
        this.container.add_item(
                BaseItem.produce("fire", 2)
        );
        this.container.add_item(
                BaseItem.produce("signpost", 64)
        );
        this.container.add_item(
                BaseItem.produce("totem", 1)
        );
        this.container.add_item(
                BaseItem.produce("copper_coin", 10)
        );
        this.container.add_item(
                BaseItem.produce("ladder", 1)
        );*/
        //System.out.println(branch.get_container());


        //----------- equip test -----------
        /*this.container.add_item(
                BaseItem.produce("valkyrie_helmet", 1).set_slot("head")
        );*/
    }

    @Override
     public boolean isPlayerEnt(){
        return true;
    }

     @Override
     public EntityRenderer build_render(){
        /*NPCRenderer __render = new NPCRenderer();
        __render.set_texture("player_hd.png");

        __render.set_animation_length(7);*/

        NPCRenderer __render = (NPCRenderer)(super.build_render());

        

        return __render;
    }

    /*@Override
     public void update(){
         super.update();

     }*/

    @Override
    public void e_on_change_item(){
        if (get_active_item() != null && get_active_item().get_type().equals("torch")){
             light_amt = 4.0f;
             WorldLayer.invalidate_light();
        }else{
             if (light_amt != 0.0f){
                light_amt = 0.0f;
                WorldLayer.invalidate_light();
            }
        }

        
    }
    
    @Override
    public void die(Entity killer){
        super.die(killer);
        ENotificationMessage msg = new ENotificationMessage("You were killed by a "+killer.getName());
        msg.setManager(env.getEventManager());
        msg.post();
    }

}
