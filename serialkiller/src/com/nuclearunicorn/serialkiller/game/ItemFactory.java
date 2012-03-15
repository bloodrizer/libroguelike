package com.nuclearunicorn.serialkiller.game;

import com.nuclearunicorn.libroguelike.game.items.BaseItem;

import java.util.HashMap;
import java.util.Map;

/**
    Item generator that stores all pre-defined items
 */
public class ItemFactory {
    
    static Map<String, BaseItem> itemMap = new HashMap<String, BaseItem>();

    static {

        BaseItem item;
        
        item = BaseItem.produce("hammer",1);
        //set effects etc
        registerItem("hammer", item);


        item = BaseItem.produce("knife",1);
        //set effects etc
        registerItem("knife", item);
    }
    
    private static void registerItem(String key, BaseItem item){
        itemMap.put(key, item);
    }

    public static BaseItem produce(String itemId) {
        return itemMap.get(itemId).getItem();
    }
}
