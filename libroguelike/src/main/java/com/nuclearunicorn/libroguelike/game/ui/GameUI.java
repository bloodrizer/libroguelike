/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.game.ui;

import com.nuclearunicorn.libroguelike.core.Input;
import com.nuclearunicorn.libroguelike.core.client.ClientEventManager;
import com.nuclearunicorn.libroguelike.events.EKeyPress;
import com.nuclearunicorn.libroguelike.events.EMouseClick;
import com.nuclearunicorn.libroguelike.events.Event;
import com.nuclearunicorn.libroguelike.events.IEventListener;
import com.nuclearunicorn.libroguelike.render.WindowRender;
import com.nuclearunicorn.libroguelike.vgui.*;


/**
 *
 * @author Administrator
 */
public class GameUI implements IUserInterface, IEventListener {

    public static NE_GUI_Inventory inventory;
    //public static NE_GUI_Craft craft;
    public static NE_GUI_Chat chat_box;
    public static NE_GUI_CharEquip char_equip;

    public static NE_GUI_Healthbar healthbar;
    //public static NE_GUI_Minimap minimap;


    public GameUI(){
        //NOTE: using constructor of UI is deprecated. Use build_ui instead
    }

/*
 *
 * Create all game controls - menus, inventory, slots, etc.
 *
 */
    public void build_ui(){
        ClientEventManager.subscribe(this);
        ui = new NE_GUI_System();


        
       
        NE_GUI_QuickslotBar quickslots = new NE_GUI_QuickslotBar();
        ui.root.add(quickslots);

        quickslots.x = WindowRender.get_window_w() / 2  - quickslots.w /2;
        quickslots.y = WindowRender.get_window_h() - quickslots.h - 10;


        //----------------------------------------------------------------------
        //here is debug shit
        char_equip = new NE_GUI_CharEquip();
        ui.root.add(char_equip);
        char_equip.set_tw(6);
        char_equip.set_th(6);


        char_equip.x = WindowRender.get_window_w()-280;
        char_equip.y = 15;

        char_equip.visible = false;


        /*
         * Inventory goes after quickslots, as items of inventory should have higher
         * z-axis index
         * Otherwise quickslot drag-n-drop would look lame
         *
         * Same for equip shit
         */

        inventory = new NE_GUI_Inventory();
        ui.root.add(inventory);

        inventory.x = 10;
        inventory.y = 130;

        /*craft = new NE_GUI_Craft();
        ui.root.add(craft);

        craft.x = 10;
        craft.y = 300; */

        //----------------------------------------------------------------------
        //chat debug

        chat_box = new NE_GUI_Chat();
        ui.root.add(chat_box);
        chat_box.set_tw(16);
        chat_box.set_th(4);

        chat_box.x = 260;
        chat_box.y = WindowRender.get_window_h() - 200;

        //----------------------------------------------------------------------
        NE_GUI_CharScreen char_screen = new NE_GUI_CharScreen();
        char_screen.set_tw(5);
        char_screen.set_th(6);

        char_screen.x = 850;
        char_screen.y = 300;

        ui.root.add(char_screen);

        healthbar = new NE_GUI_Healthbar();
        ui.root.add(healthbar);
        healthbar.x = WindowRender.get_window_w()/2 - healthbar.w/2;
        healthbar.y = 5;


        //----------------------------------------------------------------------
        //potential crash there
        
        /*minimap = new NE_GUI_Minimap();
        minimap.visible = false;
        ui.root.add(minimap);*/

    }

    public void e_on_event(Event event) {
        if (event instanceof EKeyPress){
  
            /*EKeyPress key_event = (EKeyPress) event;
            switch(key_event.key){
                case Keyboard.KEY_F1:
                    //toggle_console();
                break;
                case Keyboard.KEY_TAB:
                    ui.root.toggle();
                break;
                case Keyboard.KEY_I:
                    inventory.toggle();
                break;
                case Keyboard.KEY_Q:
                    craft.toggle();
                break;
                case Keyboard.KEY_E:
                    char_equip.toggle();
                break;
                case Keyboard.KEY_M:
                    minimap.toggle();
                break;
                case Keyboard.KEY_G:    //toggle grid
                    WorldView.DRAW_GRID = !WorldView.DRAW_GRID;
                break;
                case Keyboard.KEY_L:
                    chat_box.toggle();
                break;
                case Keyboard.KEY_DOWN:
                    WorldView.ISOMETRY_TILE_SCALE -= 0.1f;
                break;
                case Keyboard.KEY_UP:
                    WorldView.ISOMETRY_TILE_SCALE += 0.1f;
                break;

                case Keyboard.KEY_LBRACKET:
                    WorldView.set_zindex(WorldView.get_zindex()+1);
                break;
                case Keyboard.KEY_RBRACKET:
                    WorldView.set_zindex(WorldView.get_zindex()-1);
                break;*/


                /*
                * Although this may be counter-intuitive,
                * higher layer index actualy means lower geometry layer
                */

        }else if(event instanceof EMouseClick){
           //e_on_mouse_click(((EMouseClick)event));
            EMouseClick click_event = (EMouseClick)event;
            if (click_event.type == Input.MouseInputType.RCLICK){
                context_popup(click_event);
            }
       }
    }

    public void context_popup(EMouseClick event){

        /*class DropItem extends BaseItemAction{

            @Override
            public void execute() {
                System.out.println("ActionCutTree");
                System.out.print(get_owner());
            }

        }

        Point tile_origin = WorldView.getTileCoord(event.origin);
        WorldTile tile = ClientGameEnvironment.getWorldLayer(Player.get_zindex()).get_tile(tile_origin.getX(), tile_origin.getY());
        if (tile == null){
            System.out.println("no loaded tile at this position");
            return;
        }

        Entity ent = tile.get_active_object();
        if (ent == null){
            System.out.println("no active object in tile");
            return;
        }

        NE_GUI_Popup __popup = new NE_GUI_Popup();

        ui.root.add(__popup);
        __popup.x = event.origin.getX();
        __popup.y = event.get_window_y();

        //-------------------------------------------------
        ArrayList action_list = ent.get_action_list();
        //IAction<Entity>[] actions = (IAction<Entity>[]) action_list.toArray();
        Iterator<IAction> itr = action_list.iterator();

        System.out.println("Fetched "+Integer.toString(action_list.size())+" actions");


        while (itr.hasNext()){
            IAction element = itr.next();
            __popup.add_item(element);
        }

        */
        
    }

    public void e_on_event_rollback(Event event) {
        //throw new UnsupportedOperationException("Not supported yet.");
    }



    NE_GUI_System ui;
    public NE_GUI_System get_nge_ui() {
        return ui;
    }

    public void update() {
        //throw new UnsupportedOperationException("Not supported yet.");
    }

    public void render() {
        //throw new UnsupportedOperationException("Not supported yet.");
        get_nge_ui().render();
    }

    @Override
    public void init() {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
