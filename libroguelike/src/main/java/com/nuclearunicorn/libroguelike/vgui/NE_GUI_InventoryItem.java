/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.vgui;

import com.nuclearunicorn.libroguelike.core.Game;
import com.nuclearunicorn.libroguelike.core.Input;
import com.nuclearunicorn.libroguelike.events.EMouseClick;
import com.nuclearunicorn.libroguelike.game.actions.IAction;
import com.nuclearunicorn.libroguelike.game.items.BaseItem;
import com.nuclearunicorn.libroguelike.game.items.BaseItemAction;
import com.nuclearunicorn.libroguelike.game.world.WorldView;
import com.nuclearunicorn.libroguelike.render.overlay.OverlaySystem;
import org.lwjgl.input.Mouse;
import org.lwjgl.util.Point;
import org.newdawn.slick.Color;

import java.util.ArrayList;
import java.util.Iterator;

/**
 *
 * @author Administrator
 */
public class NE_GUI_InventoryItem extends NE_GUI_Sprite {

        /*
         * Quickslot pointer is required to check if item was assigned
         * to the other quickslot
         */
    
        //NE_GUI_Quickslot quickslot;

        //AreaRenderer area_renderer;


        BaseItem item;
        NE_GUI_InventoryItem(BaseItem item){
            this.item = item;
            //EventManager.subscribe(this);   //<- exception there
        }
        @Override
        public void render(){
            super.render();

            int xoffset = 8;
            if (item.get_count()>=10){
                xoffset = 16;
            }

            OverlaySystem.ttf.drawString(
                get_x()+w-xoffset,
                get_y()+h-15,
                Integer.toString(item.get_count()),
                Color.black
            );

            render_children();  //<-- required by a popup
        }

        @Override
        public void e_on_mouse_click(EMouseClick e){
            //System.out.println("NE_GUI_Inventory_Item::e_on_mouse_click()");
            if (e.type == Input.MouseInputType.RCLICK){
                context_popup(e);
            }

             //here comes hack, that allows inventory item control to stay on top
            //basicaly, we overwrite root gui pointer to drag parent inventory on top of z-stack

            if (e.type == Input.MouseInputType.LCLICK){
                
                Game.get_ui_root().remove(get_inventory_ctrl());
                Game.get_ui_root().add(get_inventory_ctrl());
            }
        }

        @Override
        public void e_on_drag(){
            this.w = 24;
            this.h = 24;

            int x = Mouse.getX();
            int y = Mouse.getY();

            Point tile_coord = WorldView.getTileCoord(x, y);

            //System.out.println("setting highlight tile @"+tile_coord);
            WorldView.highlight_tile(tile_coord);
        }

        @Override
        public void e_on_drop(){
            this.w = 32;
            this.h = 32;

            WorldView.highlight_tile(null);


            //update container, lol
            NE_GUI_Inventory inventory = get_inventory_ctrl();
            inventory.update(inventory.container);
        }

        public void context_popup(EMouseClick event){
            class ActionDrop extends BaseItemAction {
                BaseItem item;
                public ActionDrop(BaseItem item){
                    this.item = item;
                    this.name = "drop";
                }

                @Override
                public void execute() {
                    System.out.println("Item is dropped");
                    if(item!=null){
                        item.drop();
                    }
                }

            }

            System.out.println("Popup there");

            NE_GUI_Popup __popup = new NE_GUI_Popup();
            NE_GUI_System ui =  Game.get_game_mode().get_ui().get_nge_ui();


            ui.root.add(__popup);
            //add(__popup);
            __popup.x = event.origin.getX();
            __popup.y = event.get_window_y();

            //__popup.x = event.origin.getX();
            //__popup.y = event.get_window_y();
            //-------------------------------------------------
            ArrayList action_list = item.get_action_list();
            Iterator<IAction> itr = action_list.iterator();

            while (itr.hasNext()){
                IAction element = itr.next();
                __popup.add_item(element);
            }

            __popup.add_item(new ActionDrop(item));
        }

        public NE_GUI_Inventory get_inventory_ctrl(){
            //there we getting wrapper layer for inventory item
            //and getting parent of this wrapper

            return (NE_GUI_Inventory)(parent.parent); //once again, hack.
            //just implement inventory control accessor, for god's sake
        }

        public BaseItem get_item(){
            return item.getItem();
        }

        @Override
        public String get_tooltip_message() {
            if ( item != null){
                return item.get_type();
            }
            return "undefined";
        }
    }