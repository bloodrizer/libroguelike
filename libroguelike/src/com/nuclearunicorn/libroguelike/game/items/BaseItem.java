/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.game.items;

import com.nuclearunicorn.libroguelike.game.ent.ActionList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Administrator
 */
public class BaseItem{

    protected int count = 1;
    protected int max_count = 64;
    protected String type = "undefined";
    protected String slot = "undefined";

    protected Map<String,String> effects = new HashMap<String,String>();

    protected ItemContainer container;
    
    public void setEffect(String effectId, String value){
        effects.put(effectId,value);
    }

    public String getEffect(String effectId){
        return effects.get(effectId);
    }

    public BaseItem set_slot(String slot){
        this.slot = slot;
        return this;
    }

    public String get_slot(){
        return slot;
    }

    public void set_container(ItemContainer container){
        //System.out.println("setted container to '" + container + "' for item" + this);
        this.container = container;
    }
    //debug only, not safe
    public ItemContainer get_container(){
        return container;
    }

    public static BaseItem produce(String type, int count){
        BaseItem item = new BaseItem();

        item.set_type(type);
        item.set_count(count);

        return item;
    }



    public void set_type(String type){
        this.type = type;
    }

    public String get_type(){
        return type;
    }

    public int get_count() {
        return count;
    }

    public void set_count(int count) {
        if (count>max_count){
            count = max_count;
        }
        this.count = count;
    }

    public int get_space() {
        return max_count - count;
    }

    public int get_max_count() {
        return max_count;
    }

    public boolean has_space(int insertion) {
        return (get_space() > 0);
    }

    public void add_count(int count) {
        set_count(this.count + count);
    }
    public void del_count(int count) {
        set_count(this.count - count);

        if(this.count <= 0){
            System.out.println("clearing expired stack");
            drop();
        }
    }

    /*
     * Get a defensive copy of BaseItem
     */
    public BaseItem getItem() {
        BaseItem item = BaseItem.produce(type, count);

        item.set_container(container);
        item.set_slot(slot);

        item.setEffects(effects);
        
        return item;
    }

    protected void setEffects(Map<String, String> effects) {
        this.effects = effects;
    }


    public boolean hasEffect(String effectName) {
        return effects.containsKey(effectName);
    }

    /*
     * Put as much possible from src entity
     * to this entity to make stack full
     */

    public void put_from(BaseItem src) {

        int to_remove = get_space();
        if ( to_remove > src.get_count() ){
            to_remove = src.get_count();
        }

        System.out.println("to remove:" + Integer.toString(to_remove));
        System.out.println("removing from src:" + Integer.toString(src.count));

        //src.del_count(to_remove); <hell no

        System.out.println("and now it is:" + Integer.toString(src.count));
        this.add_count(to_remove);

        src.drop();
    }

    public boolean is_empty() {
        return (count <= 0);
    }

    public ArrayList get_action_list() {

        ActionList<BaseItem> list = new ActionList<BaseItem>();
        list.set_owner(this);
        return list.get_action_list();

    }

    public void drop() {
        if(container==null){
            System.err.println("Failed to drop item, no container");
            return;
        }
        System.err.println("removing item from container"+container);
        container.remove_item(this);
    }

    @Override
    public String toString() {
        return "Item["+type+"@"+slot+"("+count+")]";
    }


}
