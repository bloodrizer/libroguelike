/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.vgui.effects;

import com.nuclearunicorn.libroguelike.core.client.ClientEventManager;
import com.nuclearunicorn.libroguelike.core.client.ClientGameEnvironment;
import com.nuclearunicorn.libroguelike.events.ENotificationMessage;
import com.nuclearunicorn.libroguelike.events.ETakeDamage;
import com.nuclearunicorn.libroguelike.events.Event;
import com.nuclearunicorn.libroguelike.events.IEventListener;
import com.nuclearunicorn.libroguelike.events.network.EChatMessage;
import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.game.player.PlayerSpeech;


/*
 * Effects are quite similar to gui components
 * but they are usualy tile-assigned and have a limited lifespawn
 */
public class EffectsSystem implements IEventListener {
    public final Effect_Element root = new Effect_Element();

    public EffectsSystem(){
        ClientEventManager.subscribe(this);
    }

    public void render(){
        root.render();
    }

    public void update(){
        root.update();
    }
    
    public FXTooltip show_tooltip(){
        return null;
    }

    public void e_on_event(Event event) {

        if (event instanceof EChatMessage){
            show_speech((EChatMessage)event);
        }

        if (event instanceof ETakeDamage){
            root.add(new FXDamage(
                (ETakeDamage)event
            ));
        }
        
        if (event instanceof ENotificationMessage){
            root.add(new FXMessage(
                (ENotificationMessage)event
            ));
        }
    }

    /**
     * Float a spoken line over the speaker, if the player can perceive it at all.
     *
     * <p>Chat is the one effect that is not simply "something happened, draw it": the event
     * is broadcast to the whole layer, so drawing every one of them put bubbles in the black
     * void outside the player's FOV and let you read a conversation through a wall.
     * {@link PlayerSpeech} decides; the engine only draws.
     */
    private void show_speech(EChatMessage chat) {
        Entity speaker = ClientGameEnvironment.getEnvironment()
                .getEntityManager().get_entity(chat.uid);
        if (speaker == null) {
            return;     //HearingSensor logs this case; a bubble over nobody is not drawable
        }
        String text = PlayerSpeech.bubble(chat, speaker);
        if (text != null) {
            root.add(new FXTextBubble(speaker, text));
        }
    }

    public void e_on_event_rollback(Event event) {
        //throw new UnsupportedOperationException("Not supported yet.");
    }
}
