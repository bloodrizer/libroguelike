/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nuclearunicorn.negame.server.core;

import org.jboss.netty.channel.Channel;

/**
 *
 * @author bloodrizer
 */
public class NEDataPacket {
    String[] data;
    Channel channel;
    
    public NEDataPacket(String[] data, Channel channel){
        this.data = data;
        this.channel = channel;
    }

    public String[] getData() {
        return data;
    }

    public Channel getChannel() {
        return channel;
    }
}
