/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.game.items;

/*
 * This shit is very similar to ItemContainer, except it uses slots for item assigment
 */
public class EquipContainer {

    public static final String[] slot_list = {"head","body","legs","foot"};

    public java.util.Map<String,BaseItem> slots = new java.util.HashMap<String,BaseItem>(slot_list.length);

    /*
     *  Check if equipment set has that type of slot
     */
     public boolean has_slot(String slot){
        for (int i=0; i<slot_list.length; i++){
            if (slot_list[i].equals(slot)){
                return true;
            }
        }
        return false;
    }

    public void equip_item(BaseItem item){
        String item_slot = item.get_slot();

        if (has_slot(item_slot)){
            if(slots.get(item_slot).is_empty()){
                slots.put(item_slot, item);
            }
        }
    }

    public void unequip(BaseItem item){
        String item_slot = item.get_slot();
        if (has_slot(item_slot)){
            if(slots.get(item_slot) == item){
                slots.put(item_slot, null); //erase this slot
            }
        }
    }


    public boolean hasItem(BaseItem item) {
        for (BaseItem equippedItem : slots.values()){
            if (equippedItem.get_type().equals(item.get_type())){
                return true;
            }
        }
        return false;
    }
}
