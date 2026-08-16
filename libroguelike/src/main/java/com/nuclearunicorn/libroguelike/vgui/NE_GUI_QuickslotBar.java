/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.vgui;

import com.nuclearunicorn.libroguelike.events.EGUIDrop;
import com.nuclearunicorn.libroguelike.events.EMouseClick;
import com.nuclearunicorn.libroguelike.game.items.BaseItem;
import com.nuclearunicorn.libroguelike.game.player.Player;
import com.nuclearunicorn.libroguelike.render.Render;

import static org.lwjgl.opengl.GL11.*;

/*
 * This gui element represents series of quickslot boxes
 * in the bottom part of the screen
 */
public class NE_GUI_QuickslotBar extends NE_GUI_Element {

    public static final int SLOT_COUNT = 10;
    public static int active_slot = 0;

    /*
     * This gui element represents single slot in a quickslot bar
     */
    
    public class NE_GUI_Quickslot extends NE_GUI_Sprite {

        BaseItem item;
        public int id;

        @Override
        public void e_on_grab(EGUIDrop event){
            System.out.println("I grabbed "+event.element);
            //
            if (event.element instanceof NE_GUI_InventoryItem){
                NE_GUI_InventoryItem item_control = (NE_GUI_InventoryItem)(event.element);

                assign_item(item_control.item);
            }
        }

        public void assign_item(BaseItem item){
            this.item = item;
            Object[] slots = parent.children.toArray();

            //Iterate other quickslots and remove item, if allready assigned

            for (int i = 0; i<slots.length; i++){
                NE_GUI_Quickslot slot = (NE_GUI_Quickslot) slots[i];

                //note that we probably can not compare items due to defence coping
                if ( slot != this && slot.item != null && slot.item == item){
                    slot.item = null;
                }
            }

            if (NE_GUI_QuickslotBar.active_slot == id){
                Player.get_player_ent().set_active_item(item); //set player item and update data
            }
        }

        //allow click based slot select
        @Override
        public void e_on_mouse_click(EMouseClick e){
            NE_GUI_QuickslotBar.active_slot = id;
            Player.get_player_ent().set_active_item(item); //set player item and update data
        }

        @Override
        public void render(){

                w = 32;
                h = 32;
                x = id*(32 + 10);
                y = 0;
            
            //owerride size to make active quickslot looks larger
            if (id == NE_GUI_QuickslotBar.active_slot){
                w = w+10;
                h = h+10;

                x = x - 5;
                y = y - 5;
            }

            super.render();

            if (item == null){
                return;
            }

            //now render assigned item in the slot

            Render.bind_texture("gfx/items/" + item.get_type() + ".png");

            int x = this.get_x();
            int y = this.get_y();

            glEnable(GL_TEXTURE_2D);
            glColor3f(1.0f,1.0f,1.0f);

            glBegin(GL_QUADS);
                    glTexCoord2f(0.0f, 0.0f);
                glVertex2f( x,   y);
                    glTexCoord2f(0.0f+1.0f, 0.0f);
                glVertex2f( x+w, y);
                    glTexCoord2f(0.0f+1.0f, 0.0f+1.0f);
                glVertex2f( x+w, y+h);
                    glTexCoord2f(0.0f, 0.0f+1.0f);
                glVertex2f( x,   y+h);
            glEnd();
        }


        @Override
        public String toString(){
            return "#Quickslot#";
        }
    }

    public NE_GUI_QuickslotBar(){

        this.w = SLOT_COUNT*32 + (SLOT_COUNT-1)*10;
        this.h = 32;

        for (int i = 0; i<SLOT_COUNT; i++){
            NE_GUI_Quickslot slot = new NE_GUI_Quickslot();
            add(slot);

            slot.x = i * (32 + 10); //size+offset
            slot.y = 0;
            slot.w = 32;
            slot.h = 32;

            slot.sprite_name = "/ui/inv_slot.png";

            slot.dragable = false;

            slot.id = i;

        }
    }




    @Override
    public void render(){
        super.render();
    }

    @Override
    public String toString(){
        return "#QuickSlotBar#";
    }

}
