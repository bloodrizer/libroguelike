package com.nuclearunicorn.serialkiller.game.modes.main_menu;

import com.nuclearunicorn.libroguelike.events.Event;
import com.nuclearunicorn.libroguelike.events.IEventListener;
import com.nuclearunicorn.libroguelike.game.modes.AbstractGameMode;
import com.nuclearunicorn.libroguelike.game.ui.*;
import com.nuclearunicorn.libroguelike.render.overlay.OverlaySystem;
import com.nuclearunicorn.libroguelike.render.overlay.VersionOverlay;

/**
 * Created by IntelliJ IDEA.
 * User: Administrator
 * Date: 03.03.12
 * Time: 12:27
 * To change this template use File | Settings | File Templates.
 */
public class MainMenuMode extends AbstractGameMode implements IEventListener {

    private OverlaySystem overlay;
    IUserInterface wgt = null;
    
    @Override
    public void run() {
        overlay = new OverlaySystem(){
            @Override
            public void render() {
                VersionOverlay.render();
            }
        };
    }

    @Override
    public void update() {
        super.update();

        get_ui().update();
        get_ui().render();

        overlay.render();
    }

    @Override
    public IUserInterface get_ui(){
        if (wgt!=null){
            return wgt;
        }
        wgt = new MainMenuUI();

        return wgt;
    }

    @Override
    public void e_on_event(Event event) {
    }
}
