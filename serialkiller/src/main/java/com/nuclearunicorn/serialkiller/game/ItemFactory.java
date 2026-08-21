package com.nuclearunicorn.serialkiller.game;

import com.nuclearunicorn.libroguelike.game.items.BaseItem;
import com.nuclearunicorn.serialkiller.game.world.items.RLItem;

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

        //TODO: move to xml or what?
        
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

        item = RLItem.produce("taser",1);
        item.set_slot(SLOT_WEAPON);
        item.setEffect("damage","0");
        item.setEffect("stun_chance","80");
        item.setEffect("stun_duration","5");
        item.setEffect("damage_type","dmg_nonlethal");

        registerItem("taser", item);

        item = RLItem.produce("valium",10);
        item.setEffect("restore_hunger","0");
        item.setEffect("restore_bloodlust","-10");

        registerItem("valium", item);

        //-------------------------------------------------
        //  starting kit for the character presets (see CharacterPresets)
        //-------------------------------------------------

        item = RLItem.produce("pepper spray", 1);
        item.set_slot(SLOT_WEAPON);
        item.setEffect("damage","0");
        item.setEffect("stun_chance","60");
        item.setEffect("stun_duration","3");
        item.setEffect("damage_type","dmg_nonlethal");

        registerItem("pepper spray", item);

        item = RLItem.produce("crowbar", 1);
        item.set_slot(SLOT_WEAPON);
        item.setEffect("damage","4");
        item.setEffect("damage_type","dmg_blunt");
        item.setEffect("stun_chance","30");
        item.setEffect("stun_duration","3");

        registerItem("crowbar", item);

        item = RLItem.produce("bottle", 1);
        item.set_slot(SLOT_WEAPON);
        item.setEffect("damage","2");
        item.setEffect("damage_type","dmg_cut");

        registerItem("bottle", item);

        //no effects, all cover: the reason a stranger's door opens
        registerItem("parcel", RLItem.produce("parcel", 1));
        registerItem("keys", RLItem.produce("keys", 1));

        //the food every start used to get by name
        item = RLItem.produce("generic food", 1);
        item.setEffect("restore_hunger","10");

        registerItem("food", item);
    }
    
    private static void registerItem(String key, BaseItem item){
        itemMap.put(key, item);
    }

    public static BaseItem produce(String itemId) {
        BaseItem item = itemMap.get(itemId);
        return item == null ? null : item.getItem();
    }

    /**
     * A stack of {@code count} of the named item, or null if nothing is registered under
     * that name - the presets name their kit by string, and a typo should say so rather
     * than throw out of world generation.
     */
    public static BaseItem produce(String itemId, int count) {
        if ("cash".equals(itemId)) {
            return produceMoney(count);   //cash carries its value in the stack size
        }
        BaseItem item = produce(itemId);
        if (item != null) {
            item.set_count(count);
        }
        return item;
    }

    public static BaseItem produceFood(String name, int restoreHungerAmt){
        BaseItem food = RLItem.produce(name,1);
        food.setEffect("restore_hunger",String.valueOf(restoreHungerAmt));

        return food;
    }

    /** Loot for vaults/safes: a stack of cash worth {@code amount}. */
    public static BaseItem produceMoney(int amount){
        BaseItem money = RLItem.produce("cash", amount);
        money.set_count(amount);
        money.setEffect("value", String.valueOf(amount));

        return money;
    }
}
