/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.vgui;

import com.nuclearunicorn.libroguelike.events.EGUIDrop;
import com.nuclearunicorn.libroguelike.events.EMouseClick;
import com.nuclearunicorn.libroguelike.game.items.BaseItem;
import com.nuclearunicorn.libroguelike.game.items.EquipContainer;
import com.nuclearunicorn.libroguelike.game.player.Player;
import com.nuclearunicorn.libroguelike.render.Render;
import org.lwjgl.util.Point;

import java.util.HashMap;

import static org.lwjgl.opengl.GL11.*;


public class NE_GUI_CharEquip extends NE_GUI_FrameModern{
    public class NE_GUI_EquipSlot extends NE_GUI_Sprite {

        BaseItem item;
        public int id;
        String slot_type;

        @Override
        public void e_on_grab(EGUIDrop event){
            System.out.println("I grabbed "+event.element);
            //
            if (event.element instanceof NE_GUI_InventoryItem){
                NE_GUI_InventoryItem item_control = (NE_GUI_InventoryItem)(event.element);

                //assign_item(item_control.item);
                if(item_control.item.get_slot().equals(slot_type)){
                    assign_item(item_control.item);
                    Player.get_ent().getContainer().remove_item(item_control.item);
                }else{
                    System.err.println("Ent type '"+ item_control.item.get_slot()+ "' not equals '"+ slot_type +"'");
                }
            }
        }

        /*
         * This copypaste is priobably not nececery
         *
         */
        public void assign_item(BaseItem item){
            this.item = item.getItem();
            ((NE_GUI_CharEquip)parent).update();
        }

        //allow click based slot select
        @Override
        public void e_on_mouse_click(EMouseClick e){
            NE_GUI_QuickslotBar.active_slot = id;
            //todo: meke slot select method
        }

        @Override
        public void render(){


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

    //--------------------------------------------------------------------------
    public static final int SLOT_COUNT = 4;

    public static NE_GUI_EquipSlot[] equip_slots = new NE_GUI_EquipSlot[SLOT_COUNT];
    public NE_GUI_SpriteArea char_body;
    
    public NE_GUI_CharEquip(){

        super(true);
        set_title("Equipment");

        this.w = SLOT_COUNT*32 + (SLOT_COUNT-1)*10;
        this.h = 32;

        for (int i = 0; i<SLOT_COUNT; i++){
            NE_GUI_EquipSlot slot = new NE_GUI_EquipSlot();
            add(slot);

            equip_slots[i] = slot;  //save slot pointer

            //slot.slot_type = ((EntityPlayer)Player.get_ent()).equipment.slot_list[i];

            //static slot list will be enough for now
            //use npc-related slot list later

            //assign slot type
            slot.slot_type = EquipContainer.slot_list[i];

            slot.x = 20; //size+offset
            slot.y = 20 + i*(32+8);
            slot.w = 32;
            slot.h = 32;

            slot.sprite_name = "/ui/inv_slot.png";

            slot.dragable = false;

            slot.id = i;

        }

       

      

        char_body = new NE_GUI_SpriteArea();
        char_body.area_renderer.texture_name = "/ui/char_body_female.png";
        char_body.set_rect(0, 0, 393, 510); //shitty aestetic tweaks
        char_body.set_size(60, 20, 200, 256);

        char_body.dragable = false;

        add(char_body);
    }

   HashMap<String,Point> attachments = new HashMap<String,Point>(SLOT_COUNT);
   {
        attachments.put("head", new Point(98,34));
   }

    public void update(){

        System.out.println("Updating equipment preview");

        char_body.clear();

        for(int i=0; i<SLOT_COUNT; i++){
            Point attach = attachments.get(equip_slots[i].slot_type);
            if(attach != null && equip_slots[i].item != null){

                 System.out.println("Assigning item sprite");

                 NE_GUI_Sprite equip_sprite = new NE_GUI_Sprite();
                 equip_sprite.dragable = false; //not sure about it, but lock sprite for now

                 equip_sprite.sprite_name = "/render/gfx/equip/"
                      +equip_slots[i].item.get_type()+".png";

                 equip_sprite.set_size(attach.getX() - 32, attach.getY() - 32, 64, 64);
                 char_body.add(equip_sprite);
            }else{
                System.out.println("attach is null or item is null");
            }
        }

    }


}
