/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nuclearunicorn.negame.server.core;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.SimpleChannelHandler;

/**
 *
 * @author bloodrizer
 */
public abstract class AServerHandler extends SimpleChannelHandler {
    
    public void sendMsg(String msg, Channel ioChannel){
        ioChannel.write(msg+"\r\n");
    }
}
