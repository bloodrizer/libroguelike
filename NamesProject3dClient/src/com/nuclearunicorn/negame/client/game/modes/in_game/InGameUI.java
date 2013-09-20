package com.nuclearunicorn.negame.client.game.modes.in_game;

import com.nuclearunicorn.libroguelike.core.Input;
import com.nuclearunicorn.libroguelike.core.client.ClientEventManager;
import com.nuclearunicorn.libroguelike.events.EMouseClick;
import com.nuclearunicorn.libroguelike.events.Event;
import com.nuclearunicorn.libroguelike.events.IEventListener;
import com.nuclearunicorn.libroguelike.game.actions.IAction;
import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.game.player.Player;
import com.nuclearunicorn.libroguelike.game.ui.IUserInterface;
import com.nuclearunicorn.libroguelike.game.world.WorldTile;
import com.nuclearunicorn.libroguelike.game.world.WorldView;
import com.nuclearunicorn.libroguelike.render.WindowRender;
import com.nuclearunicorn.libroguelike.vgui.*;
import com.nuclearunicorn.serialkiller.game.world.RLTile;
import com.nuclearunicorn.serialkiller.messages.EConsoleMessage;
import com.nuclearunicorn.serialkiller.vgui.VGUICharacterInfo;
import com.nuclearunicorn.serialkiller.vgui.VGUIContainerView;
import com.nuclearunicorn.serialkiller.vgui.VGUIDetailedNPCInformation;
import org.lwjgl.util.Point;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: Administrator
 * Date: 03.03.12
 * Time: 16:05
 * To change this template use File | Settings | File Templates.
 */
public class InGameUI implements IUserInterface, IEventListener {

    public NE_GUI_System ui;

    private VGUICharacterInfo charInfo;
    private NE_GUI_Label lookAtLabel;
    private NE_GUI_Label lookAtObject;
    private VGUIDetailedNPCInformation npcInfo;
    private VGUIContainerView containerView;

    public InGameUI(){
        ui = new NE_GUI_System();
    }

    @Override
    public void build_ui() {
        ClientEventManager.subscribe(this);

        NE_GUI_FrameModern frame = new NE_GUI_FrameModern();
        ui.root.add(frame);
        frame.set_tw(31);
        frame.set_th(5);
        frame.solid = false;

        frame.set_coord(15,600);
        frame.set_title("Console");

        NE_GUI_Text console = new NE_GUI_Text(){

            {
                FONT_SIZE = 14;
            }
            @Override
            public void notify_event(Event e) {
                super.notify_event(e);
                
                if (e instanceof EConsoleMessage){
                    EConsoleMessage msgEvent = (EConsoleMessage)e;
                    SimpleDateFormat sdf =
                            new SimpleDateFormat("[hh:mm:ss]");
                    Calendar cal = Calendar.getInstance();
                    add_line(sdf.format(cal.getTime()) + " " + msgEvent.message, msgEvent.color);
                }
            }
        };
        console.x = 12;
        console.y = 36;
        console.dragable = false;
        console.max_lines = 7;

        /*console.add_line("Wellcome to the Serial Killer Roguelike");
        console.add_line("Press 'wsad' to move, 'space' to skil turn");
        console.add_line("Use 'ctrl' + direction to attack ");
        console.add_line("Press 'tab' to view your character screen and inventory ");*/

        frame.add(console);
    }

    @Override
    public NE_GUI_System get_nge_ui() {
        return ui;
    }

    @Override
    public void update() {
        //updateLookAt();
    }

    @Override
    public void render() {
        ui.render();
    }

    @Override
    public void init() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    private void updateLookAt(){
        Point tile_coord = WorldView.getTileCoord(Input.get_mx(), WindowRender.get_window_h() - Input.get_my());

        int layerID = Player.get_ent().getLayer().get_zindex();

        WorldTile tile = Player.get_ent().getLayer().get_tile(tile_coord);
        if (tile!=null && ((RLTile)tile).isExplored()){         //isVisible()
            Entity ent = tile.getEntity();

            if (ent != null){
                lookAtObject.text = "layer #"+layerID+": "+ent.getName();
            }else{
                lookAtObject.text = "";
            }
        }
    }


    public void contextPopup(EMouseClick event){


        Point tileCoord = WorldView.getTileCoord(event.origin);
        WorldTile tile = Player.get_ent().getLayer().get_tile(tileCoord);
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
    }

    @Override
    public void e_on_event(Event event) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
