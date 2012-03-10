package com.nuclearunicorn.serialkiller.messages;

import com.nuclearunicorn.libroguelike.events.Event;
import org.newdawn.slick.Color;

/**
 * Created by IntelliJ IDEA.
 * User: Administrator
 * Date: 10.03.12
 * Time: 20:10
 * To change this template use File | Settings | File Templates.
 */
public class EConsoleMessage extends Event {
    public String message;
    public Color color;
    
    public EConsoleMessage(String message, Color color){
        this.message = message;
        this.color = color;
    }
    
}
