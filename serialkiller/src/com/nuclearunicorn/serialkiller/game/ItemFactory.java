package com.nuclearunicorn.serialkiller.game;

import com.nuclearunicorn.libroguelike.game.items.BaseItem;

import java.util.HashMap;
import java.util.Map;

/**
    Item generator that stores all pre-defined items
 */
public class ItemFactory {
    
    static Map<String, BaseItem> itemMap = new HashMap<String, BaseItem>();

    private static final String SLOT_WEAPON = "weapon";

    static {

        BaseItem item;
        
        item = BaseItem.produce("hammer",1);
        item.set_slot(SLOT_WEAPON);
        item.setEffect("damage","5");
        item.setEffect("damage_type","dmg_blunt");
        item.setEffect("stun_chance","15");

        registerItem("hammer", item);


        item = BaseItem.produce("knife",1);
        item.set_slot(SLOT_WEAPON);
        item.setEffect("damage","3");
        item.setEffect("damage_type","dmg_cut");

        registerItem("knife", item);
    }
    
    private static void registerItem(String key, BaseItem item){
        itemMap.put(key, item);
    }

    public static BaseItem produce(String itemId) {
        return itemMap.get(itemId).getItem();
    }
}