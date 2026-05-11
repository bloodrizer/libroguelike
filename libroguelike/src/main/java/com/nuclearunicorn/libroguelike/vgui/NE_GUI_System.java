/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.vgui;

import com.nuclearunicorn.libroguelike.events.EGUIDrop;
import com.nuclearunicorn.libroguelike.events.Event;
import com.nuclearunicorn.libroguelike.render.WindowRender;

/**
 *
 * @author Administrator
 */
public class NE_GUI_System {
    
    public final TooltipSystem tooltip = new TooltipSystem();
    {
        tooltip.set_gui(this);
    }
    
    public final NE_GUI_Element root = new NE_GUI_Element(){

        {
            w = WindowRender.get_window_w();
            h = WindowRender.get_window_h();

            dragable = false;
            solid = false;

        }

        @Override
        public String toString(){
            return "Root GUIElement";
        }

        @Override
        public void e_on_grab(EGUIDrop event){
            /*if (event.element instanceof NE_GUI_InventoryItem){

                    BaseItem item = ((NE_GUI_InventoryItem) event.element).item;
                    System.out.println("WorldArea: grabed item " + item.get_type());
                    Class building = BuildManager.get_building(item.get_type());
                    
                    int ex = event.coord.getX();
                    int ey = WindowRender.get_window_h()-event.coord.getY();
                    Point tile_coord = WorldView.getTileCoord(ex,ey);

                    if (building!= null){
                        
                        System.out.println("spawning building");

                        if (WorldLayer.tile_blocked(tile_coord)){
                            return; //do not allow to build on blocked tile
                        }

                        EBuildStructure build_event = new EBuildStructure(tile_coord,item.get_type());
                        build_event.post();

                    }else{
                        //this item is not a building-related item, so just spawn an item container
                        ItemEntity item_ent = new ItemEntity();
                        item_ent.set_item(item);
                        
                        item_ent.spawn(tile_coord);
                    }
                    
                    Player.get_ent().container.remove_item(
                            BaseItem.produce(item.get_type(), 1)
                    );
                    
                    //remove from quickslot
            }*/
        }


    }; //big invisible container

    public void render(){

        //Tooltip system is extended version of fx system, that renders atop of usual UI

        root.render();

        tooltip.update();
        tooltip.render();

    }

    public void clear(){
        root.clear();
    }


    public NE_GUI_System(){

        //init root coord system
        root.x = 0;
        root.y = 0;

        //EventManager.subscribe(this);

    }

    public void e_on_event(Event event) {
        //throw new UnsupportedOperationException("Not supported yet.");
        root.notify_event(event);
    }

    public void e_on_event_rollback(Event event) {
        //throw new UnsupportedOperationException("Not supported yet.");
    }

    public void show_message(String title, String text) {
        NE_GUI_FrameModern messagebox = new NE_GUI_FrameModern(true);
        root.add(messagebox);
        
        messagebox.set_title(title);

        messagebox.set_tw( text.length()*9/messagebox.TITLE_SIZE + 2 );
        messagebox.set_th( 2 );
        messagebox.center();


        NE_GUI_Label message = new NE_GUI_Label();
        message.set_text(text);
        messagebox.add(message);
        message.center();
        //message.y = 20;
    }
    
    /*
     * This function should return topmost gui element, if on presents in gui layer
     * Otherwise, it should get access to the game world view layer and return some gui proxy
     */
    
    public NE_GUI_Element get_gui_element(int mx, int my){
        //return root.get_gui_element(int mx, int my);
        NE_GUI_Element elem = root.get_gui_element(mx, my);
        
        if (elem !=null && elem != root ){
            return elem;
        }
        //Point tile_coord = WorldView.getTileCoord(mx, my);
        //WorldTile tile = WorldModel.get_tile(tile_coord.getX(), tile_coord.getY());

        //TODO: check if debug enabled, couse proxies are heavy objects
        NE_GUI_Proxy proxy = null;
        /*if (tile!= null){
            proxy = new NE_GUI_Proxy();
            proxy.solid = false;
            proxy.set_tile(tile);
            
            root.add(proxy);
            

            System.out.println("proxy origin is set to"+proxy.x+","+proxy.y);
        }*/

        
        return proxy;
    }
}
