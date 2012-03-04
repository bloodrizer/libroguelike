/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.game.ent;

import com.nuclearunicorn.libroguelike.game.items.BaseItem;
import com.nuclearunicorn.libroguelike.game.items.EquipContainer;
import com.nuclearunicorn.libroguelike.render.EntityRenderer;
import com.nuclearunicorn.libroguelike.render.NPCRenderer;

/**
 *  This is generic class for every NPC,
 *  mostly players
 */

public class EntityNPC extends EntityActor {
    //name to show above
    public String name = "undefined";

    public EquipContainer equipment;

    public void EquipItem(BaseItem item){
        //if (BaseItem)
    }
    public String race = "female";
    {
        if (Math.random() > 0.25f){
           race = "male";
        }
    }
    //note, that simle male/female sex would not be enough, as we may have elven shit or zergs, etc

     @Override
     public EntityRenderer build_render(){
        NPCRenderer __render = new NPCRenderer();

        if (race.equals("male")){
            __render.set_texture("player_hd.png");
        }
        else if(race.equals("female"))
         {
            __render.set_texture("player_hd_female.png");
        }

        __render.set_animation_length(7);

        float SPRITE_SCALE = 1.2f;

        __render.get_tileset().sprite_w = (int)(46 * SPRITE_SCALE);
        __render.get_tileset().sprite_h = (int)(78 * SPRITE_SCALE); //78

        return __render;
    }
}
