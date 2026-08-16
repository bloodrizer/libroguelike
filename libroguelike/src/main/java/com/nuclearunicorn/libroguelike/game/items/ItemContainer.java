/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.game.items;

import com.nuclearunicorn.libroguelike.events.EContainerUpdate;
import com.nuclearunicorn.libroguelike.game.ent.Entity;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Administrator
 */
public class ItemContainer<T extends BaseItem> {

    //max_item slots
    public int max_items = 10;

    protected List<T> items = new ArrayList<T>();
    private Entity owner;

    public void set_max_items(int count){
         max_items = count;
    }

     /*
      * Add item stack to inventory
      *
      */

    public void add_item(T item){
        //int count = item.get_count();

        BaseItem defCopyItem = item.getItem();

        Object[] elem =  items.toArray();
        for(int i = 0; i<elem.length; i++){
            T tgt = (T)elem[i];
            if ( tgt.get_type().equals(defCopyItem.get_type()) ){  //compair types
                tgt.put_from(defCopyItem);
            }
            
            if (defCopyItem.is_empty()){
                on_update();
                return;
            }
        }
         
        List<T> itemStacks = generateStackList(item); 

        //no similar item found, or every stack is full -  adding new stack
        if(!is_full()){
            T clonedItem = (T)item.getItem();   //doubtable?
            items.add(clonedItem);
            clonedItem.set_container(this);   //required to provide contex-popup callback

            on_update();
        }
    }

    //TODO: implement stackification
    public List<T> generateStackList(T item){
         List<T> stackList = new ArrayList<T>();
         int stackCount = item.get_count() / item.get_max_count();


         return stackList;
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
                     System.out.println("removing "+elem[i].count+"from stack "+i);
                     items.remove(elem[i]); //erase this item stack completely
                     System.out.println("also, stack is erased, move on");
                 }else{
                     System.out.println("removing "+remove_count+"from stack "+i);
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
     public boolean has_item(T item){
         int ref_count = item.count;


         for(T elem: items){
             if (elem.type.equals(item.type)){
                 if(elem.count <= ref_count){
                     ref_count -= elem.count;
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

    public List<T> getItems() {
        return items;
    }
    
    public void setOwner(Entity owner){
        this.owner = owner;
    }

    public Entity getOwner() {
        return owner;
    }
}
