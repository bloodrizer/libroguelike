package com.nuclearunicorn.serialkiller.game.modes.in_game;

import com.nuclearunicorn.libroguelike.core.Input;
import com.nuclearunicorn.libroguelike.core.client.ClientEventManager;
import com.nuclearunicorn.libroguelike.events.EKeyPress;
import com.nuclearunicorn.libroguelike.events.EMouseClick;
import com.nuclearunicorn.libroguelike.events.Event;
import com.nuclearunicorn.libroguelike.events.IEventListener;
import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.game.player.Player;
import com.nuclearunicorn.libroguelike.game.ui.IUserInterface;
import com.nuclearunicorn.libroguelike.game.world.WorldTile;
import com.nuclearunicorn.libroguelike.game.world.WorldView;
import com.nuclearunicorn.libroguelike.vgui.NE_GUI_FrameModern;
import com.nuclearunicorn.libroguelike.vgui.NE_GUI_System;
import com.nuclearunicorn.libroguelike.vgui.NE_GUI_Text;
import com.nuclearunicorn.serialkiller.game.world.entities.EntRLActor;
import com.nuclearunicorn.serialkiller.messages.EConsoleMessage;
import com.nuclearunicorn.serialkiller.vgui.VGUICharacterInfo;
import org.lwjgl.input.Keyboard;
import org.lwjgl.util.Point;

import java.text.SimpleDateFormat;
import java.util.Calendar;

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

    public InGameUI(){
        ui = new NE_GUI_System();
    }

    @Override
    public void e_on_event(Event event) {
        if (event instanceof EMouseClick){
            EMouseClick clickEvent = (EMouseClick)event;

            if (clickEvent.type == Input.MouseInputType.LCLICK){
                Point tile_coord = WorldView.getTileCoord(clickEvent.origin.getX(), clickEvent.origin.getY());
                
                
                WorldTile tile = Player.get_ent().getLayer().get_tile(tile_coord);
                if (tile!=null){
                    Entity ent = tile.getEntity();
                    if (ent instanceof EntRLActor){
                        ((EntRLActor)ent).describe();
                    }

                    //RLMessages.message("You see", Color.lightGray);
                }
            }
        }
        if (event instanceof EKeyPress){
            EKeyPress key_event = (EKeyPress) event;
            switch(key_event.key){
                case Keyboard.KEY_TAB:
                    charInfo.toggle();
                break;
            }
        }
    }

    @Override
    public void build_ui() {
        ClientEventManager.eventManager.subscribe(this);

        NE_GUI_FrameModern frame = new NE_GUI_FrameModern();
        ui.root.add(frame);
        frame.set_tw(31);
        frame.set_th(5);
        frame.solid = false;

        frame.set_coord(15,600);
        frame.set_title("Console");

        NE_GUI_Text console = new NE_GUI_Text(){
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
        console.y = 9;
        console.dragable = false;
        console.max_lines = 8;

        console.add_line("Wellcome to the Serial Killer Roguelike");
        console.add_line("Press 'wsad' to move, 'space' to skil turn");
        console.add_line("Use 'ctrl' + direction to attack ");
        console.add_line("Press 'tab' to view your character screen and inventory ");

        frame.add(console);

        //Inventory
        charInfo = new VGUICharacterInfo();
        charInfo.set_tw(25);
        charInfo.set_th(17);
        charInfo.visible = true;
        charInfo.center();
        ui.root.add(charInfo);
    }

    @Override
    public NE_GUI_System get_nge_ui() {
        return ui;
    }

    @Override
    public void update() {

    }

    @Override
    public void render() {
        ui.render();
    }
}
