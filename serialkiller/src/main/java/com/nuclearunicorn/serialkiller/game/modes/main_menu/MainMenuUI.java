package com.nuclearunicorn.serialkiller.game.modes.main_menu;

import com.nuclearunicorn.libroguelike.core.client.ClientEventManager;
import com.nuclearunicorn.libroguelike.core.client.ClientGameEnvironment;
import com.nuclearunicorn.libroguelike.events.EMouseClick;
import com.nuclearunicorn.libroguelike.events.EKeyPress;
import com.nuclearunicorn.libroguelike.events.Event;
import com.nuclearunicorn.libroguelike.events.IEventListener;
import com.nuclearunicorn.libroguelike.game.ui.IUserInterface;
import com.nuclearunicorn.libroguelike.vgui.NE_GUI_Button;
import com.nuclearunicorn.libroguelike.vgui.NE_GUI_FrameModern;
import com.nuclearunicorn.libroguelike.vgui.NE_GUI_Label;
import com.nuclearunicorn.libroguelike.vgui.NE_GUI_System;
import com.nuclearunicorn.serialkiller.game.Main;
import com.nuclearunicorn.serialkiller.game.SkillerGame;
import com.nuclearunicorn.serialkiller.game.character.CharacterPreset;
import com.nuclearunicorn.serialkiller.game.character.CharacterSetup;
import com.nuclearunicorn.serialkiller.utils.pathfinder.adaptive.AdaptivePathfinder;
import com.nuclearunicorn.serialkiller.vgui.VGUINewGameWizard;
import org.newdawn.slick.Color;
import org.lwjgl.input.Keyboard;

/**

 */
public class MainMenuUI implements IUserInterface, IEventListener {

    public NE_GUI_System ui;
    private VGUINewGameWizard newGameWizard;
    final NE_GUI_FrameModern frame = new NE_GUI_FrameModern();
    private final MainMenuMode mode;

    public MainMenuUI(MainMenuMode mode){
        this.mode = mode;
        ui = new NE_GUI_System();
    }

    @Override
    public void e_on_event(Event event) {
        //build_ui() subscribes us once and there is no unsubscribe, so keep our hands off
        //input whenever the menu is not the mode on screen
        if (!mode.isCurrent()){
            return;
        }

        //allow esc to cycle game menu
        if (event instanceof EKeyPress){
            //the menu is reachable by keyboard as well as by mouse: replays carry keys and
            //not clicks, so a mouse-only new game is a new game no test can ever start
            if (((EKeyPress) event).key == Keyboard.KEY_N
                    && (newGameWizard == null || !newGameWizard.visible)){
                event.dispatch();
                openNewGameWizard();
                return;
            }

            if (((EKeyPress) event).key == Keyboard.KEY_ESCAPE){
                event.dispatch();

                //escape backs out one screen at a time: the wizard first, the menu second
                if (newGameWizard != null && newGameWizard.visible){
                    newGameWizard.cancel();
                    return;
                }

                SkillerGame game = Main.game;
                game.set_state("inGame");
                return;
            }
        }
    }

    @Override
    public void build_ui() {
        ClientEventManager.subscribe(this);

        final SkillerGame game = Main.game;


        NE_GUI_Label loadingLabel = new NE_GUI_Label();
        loadingLabel.set_text("Loading...");
        loadingLabel.center();
        loadingLabel.setColor(Color.lightGray);
        ui.root.add(loadingLabel);


        frame.set_tw(12);
        frame.set_th(8);
        frame.center();
        frame.title = "Main Menu";
        frame.dragable = false; //don't let show underlying 'loading' lable.

        ui.root.add(frame);
        
        

        NE_GUI_Button newGameButon = new NE_GUI_Button(){
            @Override
            public void e_on_mouse_click(EMouseClick e) {
                openNewGameWizard();
            }
        };
        newGameButon.set_tw(4);
        newGameButon.set_coord(130, 50);
        newGameButon.text = "New game (n)";
        newGameButon.color = Color.lightGray;

        frame.add(newGameButon);

        NE_GUI_Button continueButton = new NE_GUI_Button(){
            @Override
            public void e_on_mouse_click(EMouseClick e) {
                game.set_state("inGame");
            }
        };
        continueButton.set_tw(4);
        continueButton.set_coord(130, 110);
        continueButton.text = "Continue";
        continueButton.color = Color.lightGray;

        frame.add(continueButton);

        //-------------------------
        //the character screen the "custom game" button used to open was an empty frame; this
        //is what it was a placeholder for
        newGameWizard = new VGUINewGameWizard(new VGUINewGameWizard.Listener(){
            @Override
            public void onBegin(CharacterPreset preset) {
                startNewGame();
            }

            @Override
            public void onCancel() {
                frame.visible = true;
            }
        });
        newGameWizard.center();
        newGameWizard.visible = false;

        ui.root.add(newGameWizard);

    }

    /** A new game is a choice of life first; the wizard starts it once one is made. */
    private void openNewGameWizard() {
        if (newGameWizard == null){
            return;   //keys can arrive before build_ui() has run
        }
        frame.visible = false;
        newGameWizard.visible = true;
    }

    /** Throw away the world that was and build one for the preset just chosen. */
    private void startNewGame() {
        SkillerGame game = Main.game;

        frame.visible = false;
        newGameWizard.visible = false;

        ClientGameEnvironment.reset();  //reset env, or wierd shit will happen
        AdaptivePathfinder.reset();
        CharacterSetup.reset();         //re-roll "random" for the town about to be built

        game.resetState("inGame");
        game.set_state("inGame");
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

    @Override
    public void init() {
        frame.visible = true;
        if (newGameWizard != null){
            newGameWizard.visible = false;   //back from a game: the menu, not the wizard
        }
    }
}
