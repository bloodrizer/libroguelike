package com.nuclearunicorn.serialkiller.game.modes.main_menu;

import com.nuclearunicorn.libroguelike.core.client.ClientEventManager;
import com.nuclearunicorn.libroguelike.core.client.ClientGameEnvironment;
import com.nuclearunicorn.libroguelike.events.EMouseClick;
import com.nuclearunicorn.libroguelike.events.Event;
import com.nuclearunicorn.libroguelike.events.IEventListener;
import com.nuclearunicorn.libroguelike.game.ent.EntityManager;
import com.nuclearunicorn.libroguelike.game.ui.IUserInterface;
import com.nuclearunicorn.libroguelike.vgui.NE_GUI_Button;
import com.nuclearunicorn.libroguelike.vgui.NE_GUI_FrameModern;
import com.nuclearunicorn.libroguelike.vgui.NE_GUI_System;
import com.nuclearunicorn.serialkiller.game.Main;
import com.nuclearunicorn.serialkiller.game.modes.in_game.InGameMode;
import com.nuclearunicorn.serialkiller.vgui.VGUICreateCharacterScreeen;

/**

 */
public class MainMenuUI implements IUserInterface, IEventListener {

    public NE_GUI_System ui;
    private VGUICreateCharacterScreeen createCharScreen;

    public MainMenuUI(){
        ui = new NE_GUI_System();
    }

    @Override
    public void e_on_event(Event event) {
    }

    @Override
    public void build_ui() {
        ClientEventManager.eventManager.subscribe(this);

        //System.out.println("building main menu UI");

        NE_GUI_FrameModern frame = new NE_GUI_FrameModern();

        frame.set_tw(12);
        frame.set_th(6);
        frame.center();
        frame.title = "Main Menu";

        ui.root.add(frame);

        NE_GUI_Button newGameButon = new NE_GUI_Button(){
            @Override
            public void e_on_mouse_click(EMouseClick e) {

                ClientGameEnvironment.reset();  //reset env, or wierd shit will happen
                Main.game.resetState("inGame");

                //Main.inGameMode = new InGameMode();
                //Main.inGameMode.setActive(false);

                Main.game.set_state("inGame");
            }
        };
        newGameButon.set_tw(4);
        newGameButon.set_coord(130, 50);
        newGameButon.text = "New game";

        frame.add(newGameButon);

        NE_GUI_Button continueButton = new NE_GUI_Button(){
            @Override
            public void e_on_mouse_click(EMouseClick e) {
                Main.game.set_state("inGame");
            }
        };
        continueButton.set_tw(4);
        continueButton.set_coord(130, 110);
        continueButton.text = "Continue game";

        frame.add(continueButton);
        
        //-------------------------
        createCharScreen = new VGUICreateCharacterScreeen();
        createCharScreen.set_tw(14);
        createCharScreen.set_th(10);
        createCharScreen.center();
        createCharScreen.title = "Create new character";

        createCharScreen.visible = false;

        ui.root.add(createCharScreen);

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
