package com.nuclearunicorn.serialkiller.game;

import com.nuclearunicorn.libroguelike.game.items.BaseItem;
import com.nuclearunicorn.serialkiller.game.world.RLItem;

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
        
        item = RLItem.produce("hammer", 1);
        item.set_slot(SLOT_WEAPON);
        item.setEffect("damage","2");
        item.setEffect("damage_type","dmg_blunt");
        item.setEffect("stun_chance","50");
        item.setEffect("stun_duration","5");

        registerItem("hammer", item);


        item = RLItem.produce("knife",1);
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
