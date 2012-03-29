/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.game.ent;

import com.nuclearunicorn.libroguelike.game.items.BaseItem;
import com.nuclearunicorn.libroguelike.game.player.Player;
import com.nuclearunicorn.libroguelike.render.EntityRenderer;
import com.nuclearunicorn.libroguelike.render.SpriteRenderer;

import java.util.ArrayList;

/*
 * This is an entity wrapper for a item
 *
 *
 */

public class ItemEntity extends Entity{

    protected BaseItem item;
    public String get_item_type(){
        if (item!=null){
            return item.get_type();
        }else{
            return "undefined";
        }
    }
    
    @Override
    public EntityRenderer build_render(){
        SpriteRenderer __render = new SpriteRenderer();
        __render.set_texture("/render/gfx/items/"+get_item_type()+".png");

        __render.get_tileset().sprite_w = 32;
        __render.get_tileset().sprite_h = 32;

        __render.get_tileset().TILESET_W = 1;
        __render.get_tileset().TILESET_H = 1;

        return __render;
    }

    public void set_item(BaseItem items){
        this.item = item;
    }

    @Override
    public ArrayList get_action_list(){

        class ActionPickUp extends BaseEntityAction{
            @Override
            public void execute() {
                System.out.println("You got an awesome shit");

                BaseItem item = BaseItem.produce(get_item_type(), 1);
                Player.get_ent().container.add_item(item);
            }

        }


        ActionList list = new ActionList();
        list.set_owner(this);
        list.add_action(new ActionPickUp(),"Pick up");

        return list.get_action_list();
    }
}
