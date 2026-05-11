/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.vgui;

import com.nuclearunicorn.libroguelike.core.client.ClientEventManager;
import com.nuclearunicorn.libroguelike.events.*;
import com.nuclearunicorn.libroguelike.game.items.BaseItem;
import com.nuclearunicorn.libroguelike.game.items.ItemContainer;
import com.nuclearunicorn.libroguelike.render.AreaRenderer;
import com.nuclearunicorn.libroguelike.render.Render;

/**
 *
 * @author Administrator
 */
public class NE_GUI_Inventory extends NE_GUI_FrameModern implements IEventListener {

    NE_GUI_Element inv_layer;


    AreaRenderer area_renderer;


    public NE_GUI_Inventory(){
        super(true);
        set_title("inventory");

        this.set_tw(7);
        this.set_th(5);

        ClientEventManager.subscribe(this);

        /* this is invisible container to store item icons
         * since NE_GUI_Inventory is container itself, we net
         * to separate window-related static elements
         * from dinamic icons */
        inv_layer = new NE_GUI_Element(){
            @Override
            public void e_on_mouse_click(EMouseClick e){
                System.out.println("InventoryLayer::click");
            }

            @Override
            public void render(){
                int qx;
                int qy;

                for(int i=0; i< 15; i++){
                    qx = get_x() + get_item_x(i) * (32+8) - 20;
                    qy = get_y() + get_item_y(i) * (32+8) - 12;
                    area_renderer.render(qx, qy, 40, 40);
                }

                super.render();
            }
        };

        inv_layer.x = 32;
        inv_layer.y = 32;
        inv_layer.w = (this.t_window_w-2)*32;
        inv_layer.h = (this.t_window_h-2)*32;


        //inv_layer.solid = false;  DO NOT USE IT
        inv_layer.dragable = false;
        
        add(inv_layer);

        area_renderer = new AreaRenderer();
        area_renderer.set_rect(0, 96, 48, 48);  //bg
        //area_renderer.set_rect(48, 96, 48, 48);  //invisible bg

    }

    ItemContainer container;
    public void set_container(ItemContainer container){
        this.container = container;
        update(container);  //actualy required
    }

    //returns item tile x position, based on the size of the frame container
    private int get_item_x(int i){
        return i % (this.t_window_w-2);
    }
     //returns item tile y position, based on the size of the frame container
    private int get_item_y(int i){
        return i / (this.t_window_w-2);
    }

    public void update(ItemContainer container){
        if (container != this.container){
            return;
        }

        inv_layer.clear();

        BaseItem[] items = (BaseItem[]) container.getItems().toArray(new BaseItem[0]);
        for(int i = 0; i< items.length; i++){

            BaseItem item = (BaseItem)items[i];

            //HACK HACK HACK (hides problem with container assigment)
            if (item.get_container() == null){
                item.set_container(container);
            }

            NE_GUI_InventoryItem item_control = new NE_GUI_InventoryItem(item);
            inv_layer.add(item_control);
            
            

            String sprite_name = "/render/gfx/items/"+ item.get_type() +".png";
            
            if (Render.class.getResourceAsStream(sprite_name) == null ){
                sprite_name = "/render/gfx/items/undefined.png";
            }
            item_control.sprite_name = sprite_name;

            item_control.x = get_item_x(i) * (32+8) - 16;
            item_control.y = get_item_y(i) * (32+8) - 8;

            item_control.w = 32;
            item_control.h = 32;

            

            //item_control.dragable = false;
        }
    }

    @Override
    public void render(){
        super.render();
    }

    @Override
    public void e_on_grab(EGUIDrop event){
        System.out.println("dispatching event");
        event.dispatch();

        if (event.element instanceof NE_GUI_InventoryItem){
            NE_GUI_InventoryItem item_ctrl = (NE_GUI_InventoryItem)(event.element);

            BaseItem item = item_ctrl.item.getItem();
            container.add_item(item);
            item_ctrl.get_inventory_ctrl().container.remove_item(item);
        }
        //container
        /*if (event.element instanceof NE_GUI_InventoryItem){
            
        }*/
        //TODO: do some shit there
    }

    public void e_on_event(Event event) {
        if (event instanceof EContainerUpdate){
            update( ((EContainerUpdate) event).container);
        }
    }

    public void e_on_event_rollback(Event event) {
        //throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void e_on_mouse_click(EMouseClick e){
        System.out.println("NE_GUI_Inventory::click");
    }
}
