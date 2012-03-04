package com.nuclearunicorn.serialkiller.game.modes.main_menu;

import com.nuclearunicorn.libroguelike.core.client.ClientEventManager;
import com.nuclearunicorn.libroguelike.events.Event;
import com.nuclearunicorn.libroguelike.events.IEventListener;
import com.nuclearunicorn.libroguelike.game.ui.IUserInterface;
import com.nuclearunicorn.libroguelike.vgui.NE_GUI_FrameModern;
import com.nuclearunicorn.libroguelike.vgui.NE_GUI_System;

/**
 * Created by IntelliJ IDEA.
 * User: Administrator
 * Date: 03.03.12
 * Time: 16:05
 * To change this template use File | Settings | File Templates.
 */
public class MainMenuUI implements IUserInterface, IEventListener {

    public NE_GUI_System ui;

    public MainMenuUI(){
        ui = new NE_GUI_System();
    }

    @Override
    public void e_on_event(Event event) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void build_ui() {
        ClientEventManager.eventManager.subscribe(this);

        //System.out.println("building main menu UI");

        NE_GUI_FrameModern frame = new NE_GUI_FrameModern();
        ui.root.add(frame);

        frame.set_tw(12);
        frame.set_th(6);
        frame.center();

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
