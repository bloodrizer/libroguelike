/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.vgui;

import com.nuclearunicorn.libroguelike.core.Console;
import com.nuclearunicorn.libroguelike.core.client.ClientGameEnvironment;
import com.nuclearunicorn.libroguelike.events.EMouseClick;
import com.nuclearunicorn.libroguelike.events.Event;
import com.nuclearunicorn.libroguelike.events.network.EChatMessage;
import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.game.player.Player;
import com.nuclearunicorn.libroguelike.game.player.PlayerSpeech;

/**
 *
 * @author Administrator
 */
public class NE_GUI_Chat extends NE_GUI_FrameModern {

    NE_GUI_Input input;

    NE_GUI_Text chat_history;

    public NE_GUI_Chat(){
        super(true);
        set_title("Chatlog");


        final NE_GUI_Chat __chat_control = this;

        chat_history = new NE_GUI_Text();

        chat_history.add_line("Wellcome to the Nameless Game. There are currently 19876 players online");
        chat_history.add_line("Controls: I - inventory, E - equip, Q - craft, L - chatlog, G - toggle grid");


        add(chat_history);
        chat_history.x = 40;
        chat_history.y = 15;
        chat_history.dragable = false;

        input = new NE_GUI_Input(){
            @Override
            public void e_on_submit(){
                submit();
            }

        };
        input.x = 40;
        input.y = 95;

        input.w = 380;
        input.dragable = false;

        add(input);

        NE_GUI_Button submit = new NE_GUI_Button(){
            @Override
            public void e_on_mouse_click(EMouseClick e){
                submit();
            }
        };
        add(submit);
        submit.text = "Send";
        submit.set_tw(2);
        submit.x = 430;
        submit.y = 88;
        submit.dragable = false;

    }

    public void submit(){
        String text = input.text;
        input.text = "";

        //chat_history.add_line(text);

        if (text.length()>0 && text.charAt(0) == '/' ){
            Console.execute_cmd(text);
            return;
        }

        EChatMessage message = new EChatMessage(Player.get_ent().get_uid(),text);
        message.post();
    }

    @Override
    public void notify_event(Event e){
        //((NE_GUI_Element)this).notify_event(e);
        super.notify_event(e);

        if (e instanceof EChatMessage){
            log_speech((EChatMessage)e);
        }
    }

    /**
     * A line in the log for anything the player made out the words of.
     *
     * <p>Chat events go to the whole layer, so this used to transcribe every conversation in
     * town — through walls, across the map — and attribute it to a raw entity uid. What you
     * could not hear does not belong in your log, and what you could is worth a name.
     */
    private void log_speech(EChatMessage chat){
        Entity speaker = ClientGameEnvironment.getEnvironment()
                .getEntityManager().get_entity(chat.uid);
        if (speaker == null || !PlayerSpeech.audible(chat, speaker)){
            return;
        }
        String who = speaker.getName() == null || speaker.getName().isEmpty()
                ? chat.uid : speaker.getName();
        chat_history.add_line(who + " says: " + chat.message);
    }
}
