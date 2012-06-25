/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.game.ui;

import com.nuclearunicorn.libroguelike.core.client.ClientEventManager;
import com.nuclearunicorn.libroguelike.events.EMouseClick;
import com.nuclearunicorn.libroguelike.events.EPlayerAuthorise;
import com.nuclearunicorn.libroguelike.events.Event;
import com.nuclearunicorn.libroguelike.events.IEventListener;
import com.nuclearunicorn.libroguelike.events.network.ESelectCharacter;
import com.nuclearunicorn.libroguelike.game.player.CharacterInfo;
import com.nuclearunicorn.libroguelike.render.WindowRender;
import com.nuclearunicorn.libroguelike.vgui.*;


/**
 *
 * @author Administrator
 */


public class MainMenuUI implements IUserInterface, IEventListener {
    
    public static final boolean FORCE_AUTOLOGIN = true;    //USE THIS ONLY FOR DEBUG
    
    NE_GUI_Input login_input;
    NE_GUI_Input pass_input;
    MainMenuLogo logo;

    public NE_GUI_System ui;
    
    public MainMenuUI(){
         ui = new NE_GUI_System();
    }

    public void show_message(String message){
        NE_GUI_Frame frame = new NE_GUI_Frame(true);
        ui.root.add(frame);
        frame.set_tw(
                (message.length()*10 / NE_GUI_Frame.WIN_TILE_SIZE)  + 2
                );
        frame.set_th(3);

        frame.center();

        NE_GUI_Label label1 = new NE_GUI_Label();
        frame.add(label1);
        label1.text = message;
        label1.x = frame.w/2 - (message.length()/2*10) + 24;
        label1.y = frame.h/2-7;
        
    }
    

    public void build_ui() {

        ClientEventManager.eventManager.subscribe(this);

        NE_GUI_Sprite background = new NE_GUI_Sprite(){{
            sprite_name = "/ui/background.jpeg";
            x = 0;
            y = 0;
            w = WindowRender.get_window_w();
            h = WindowRender.get_window_h();
        }};
        ui.root.add(background);

        
        //logo
        
        logo = new MainMenuLogo();
        logo.x = WindowRender.get_window_w()/2 - logo.get_w()/2 + 80;
        logo.y = 100;
       

        NE_GUI_FrameModern frame = new NE_GUI_FrameModern();
        ui.root.add(frame);
        frame.set_tw(12);
        frame.set_th(6);

        //frame.x = 190;
        //frame.y = 190;
        frame.center();

        login_input = new NE_GUI_Input();
        frame.add(login_input);
        login_input.x = 180;
        login_input.y = 50;
        login_input.w = 120;
        login_input.dragable = false;

        login_input.text = "Red";

        pass_input = new NE_GUI_Input();
        frame.add(pass_input);
        pass_input.x = 180;
        pass_input.y = 80;
        pass_input.w = 120;
        pass_input.dragable = false;

        pass_input.text = "true";

        NE_GUI_Label label1 = new NE_GUI_Label();
        frame.add(label1);
        label1.text = "Login";
        label1.x = 100;
        label1.y = 50;

        NE_GUI_Label label2 = new NE_GUI_Label();
        frame.add(label2);
        label2.text = "Password";
        label2.x = 100;
        label2.y = 80;


        NE_GUI_Button button = new NE_GUI_Button() {
            @Override
            public void e_on_mouse_click(EMouseClick e){
                try{
                    //NettyClient.connect();
                    //uncomment me, I'm old core code
                    /*
                    Io.reset();
                    Io.connect();
                    Io.login(login_input.text, pass_input.text);
                    */
                }

                /*catch (java.net.SocketTimeoutException ex){
                    show_message("Connection timed out");
                }*/
                catch(Exception ex){

                    ex.printStackTrace();
                    //System.out.println(ex);
                    show_message(ex.toString());
                }
            }
        };

        frame.add(button);

        button.dragable = false;

        button.x = 140;
        button.y = 120;
        button.set_tw(3);
        
        
        //-------------------------
        //AUTOCONNECT SHIT THERE
        if (FORCE_AUTOLOGIN){
            //NettyClient.connect();
        }
    }


    public NE_GUI_System get_nge_ui() {
        return ui;
    }

    public void e_on_event(Event event) {
        //throw new UnsupportedOperationException("Not supported yet.");
        if (event instanceof EPlayerAuthorise){
            
            if (!FORCE_AUTOLOGIN){
                show_char_select();
                return;
            }
            
            event.dispatch();   //TODO: check if it would conflict with gui subsystem
            
            CharacterInfo chrInfo = new CharacterInfo();
            chrInfo.name = "PlayerName(debug)";
            
            ESelectCharacter selectChrEvent = new ESelectCharacter(chrInfo);
            selectChrEvent.post();
        }
    }


    public void e_on_event_rollback(Event event) {
        //throw new UnsupportedOperationException("Not supported yet.");
    }

    private void show_char_select() {
        NE_GUI_FrameModern char_select = new NE_GUI_FrameModern();
        ui.root.add(char_select);
        char_select.set_tw(10);
        char_select.set_th(8);
        /*char_select.x = 220;
        char_select.y = 160;*/
        char_select.center();

        NE_GUI_Button char_button;
        NE_GUI_SpriteArea char_sprite;
        for(int i = 0; i< 3; i++){
            char_button = new NE_GUI_Button() {
                @Override
                public void e_on_mouse_click(EMouseClick e){
                    
                    //TODO: actually populate character infos list from server
                    
                    CharacterInfo chrInfo = new CharacterInfo();
                    chrInfo.name = login_input.text;
                    
                    ESelectCharacter event = new ESelectCharacter(chrInfo);
                    event.post();
                }
            };
            char_select.add(char_button);
            char_button.set_tw(3);
            char_button.x = 160;
            char_button.y = 55 + i * 60;
            char_button.text = "Char #"+i;


            char_sprite = new NE_GUI_SpriteArea();
            char_select.add(char_sprite);
            char_sprite.set_rect(48, 96, 48, 48);

            /*char_sprite.set_size(64, 64, 40,
                    55 + i * 60);*/

            char_sprite.w = 60;
            char_sprite.h = 60;
            char_sprite.x = 60;
            char_sprite.y = 37 + i * 64;

        }
    }

    public void update() {
        //throw new UnsupportedOperationException("Not supported yet.");
        logo.update();
    }

    public void render() {
        //throw new UnsupportedOperationException("Not supported yet.");
        
        get_nge_ui().render();
        logo.render();
    }

    @Override
    public void init() {
        //To change body of implemented methods use File | Settings | File Templates.
    }


}
