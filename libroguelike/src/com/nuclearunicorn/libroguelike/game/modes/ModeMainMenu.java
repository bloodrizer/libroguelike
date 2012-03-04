/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.game.modes;


import com.nuclearunicorn.libroguelike.core.Input;
import com.nuclearunicorn.libroguelike.core.client.ClientEventManager;
import com.nuclearunicorn.libroguelike.events.Event;
import com.nuclearunicorn.libroguelike.events.IEventListener;
import com.nuclearunicorn.libroguelike.game.ui.IUserInterface;
import com.nuclearunicorn.libroguelike.game.ui.MainMenuUI;
import com.nuclearunicorn.libroguelike.render.overlay.OverlaySystem;
import com.nuclearunicorn.libroguelike.render.overlay.VersionOverlay;


/**
 *
 * @author Administrator
 */
public class ModeMainMenu implements IGameMode, IEventListener {

    //public static final boolean FORCE_AUTOLOGIN = true;    //USE THIS ONLY FOR DEBUG

    //private NE_GUI_System gui;
    private OverlaySystem overlay;
    
    public ModeMainMenu(){
        ClientEventManager.eventManager.subscribe(this);
    }

    public void run(){
        //gui = new NE_GUI_System();
        overlay = new OverlaySystem(){
            @Override
            public void render() {
                VersionOverlay.render();
            }

        };

        //Render.set_cursor("/render/ico_default.png");
    }

    public void update(){
        Input.update();
        //EventManager.update();

        get_ui().update();
        get_ui().render();

        overlay.render();
    }

    IUserInterface wgt = null;
    public IUserInterface get_ui(){
        if (wgt!=null){
            return wgt;
        }
        wgt = new MainMenuUI();

        return wgt;
    }

    public void e_on_event(Event event) {
        /* (event instanceof EPlayerAuthorise && FORCE_AUTOLOGIN){
            event.dispatch();   //TODO: check if it would conflict with gui subsystem
            
            CharacterInfo chrInfo = new CharacterInfo();
            chrInfo.name = "PlayerName(debug)";
            
            ESelectCharacter selectChrEvent = new ESelectCharacter(chrInfo);
            selectChrEvent.post();
        }*/
    }

    public void e_on_event_rollback(Event event) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
