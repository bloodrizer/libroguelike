/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.game.items;

import com.nuclearunicorn.libroguelike.events.EContainerUpdate;

import java.util.ArrayList;
import java.util.Collection;

/**
 *
 * @author Administrator
 */
public class ItemContainer {

     //max_item slots
     public int max_items = 10;

     public Collection<BaseItem> items = new ArrayList<BaseItem>();

     public void set_max_items(int count){
         max_items = count;
     }

     /*
      * Add item stack to inventory
      *
      */

     public void add_item(BaseItem item){
        //int count = item.get_count();

        Object[] elem =  items.toArray();
        for(int i = 0; i<elem.length; i++){
            BaseItem tgt = (BaseItem)elem[i];
            if ( tgt.get_type().equals(item.get_type()) ){  //compair types
                tgt.put_from(item);
            }
            
            if (item.is_empty()){
                on_update();
                return;
            }
        }

        //no similar item found, or every stack is full -  adding new stack
        if(!is_full()){
            items.add(item.getItem());
            item.set_container(this);   //required to provide contex-popup callback

            on_update();
        }
     }

     public boolean is_full(){
         return ( items.size() >= max_items );
     }

     /*
      * Remove N items from container
      * with set item type
      * This function can work with multiple stacks
      */

     public void remove_item(String type, int count){
         remove_item(BaseItem.produce(type, count));
     }

     public void remove_item(BaseItem item){
         int remove_count = item.count; //shows how much items left to remove
         System.out.println("removing "+remove_count+" items from container");
         
         BaseItem[] elem =  items.toArray(new BaseItem[0]);
         for(int i = 0; i<elem.length; i++){

             if (elem[i].type.equals(item.type)){
                 if(elem[i].count <= remove_count){
                     remove_count -= elem[i].count;
                     items.remove(elem[i]); //erase this item stack completely
                 }else{
                     elem[i].del_count(remove_count);
                     remove_count = 0;
                 }
             }
         }

         on_update();
     }
      /*
      * Return true if container has N items of set type
      * This function can work with multiple stacks
      */
     public boolean has_item(BaseItem item){
         int ref_count = item.count;

         BaseItem[] elem =  items.toArray(new BaseItem[0]);
         for(int i = 0; i<elem.length; i++){
             if (elem[i].type.equals(item.type)){
                 if(elem[i].count <= ref_count){
                     ref_count -= elem[i].count;
                 }else{
                     ref_count = 0;
                 }
             }
         }

         if( ref_count == 0 ){
             return true;
         }else{
            return false;
         }
     }

     public void on_update(){
         EContainerUpdate event = new EContainerUpdate(this);
         event.post();
     }
}
