/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.game.items;

import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.render.EntityRenderer;
import com.nuclearunicorn.libroguelike.render.SpriteRenderer;

/**
 *
 * @author Administrator
 */
public class ItemEnt extends Entity {
    BaseItem item;
    public void set_item(BaseItem item){
        this.item = item.getItem();
    }

    public BaseItem get_item(){
        return this.item.getItem();
    }

     @Override
     public EntityRenderer build_render(){

        if(this.item == null){
            return null;
        }

        SpriteRenderer __render = new SpriteRenderer();
        __render.set_texture("render/gfx/items/"+this.item.get_type()+".png");

        __render.get_tileset().TILESET_W = 1;
        __render.get_tileset().TILESET_H = 1;

        __render.get_tileset().sprite_w = 32;
        __render.get_tileset().sprite_h = 32;


        return __render;
    }
}
