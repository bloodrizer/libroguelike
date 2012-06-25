/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nuclearunicorn.negame.server.core;

import java.net.InetSocketAddress;
import java.util.HashMap;
import org.jboss.netty.channel.Channel;

/**
 *
 * @author bloodrizer
 * 
 * 
 * User pool allows different servers to map their connections to the same user data
 * 
 */
public class ServerUserPool {
    public static HashMap<String, User> ip2user         = new HashMap<String, User>(32);
    public static HashMap<Channel, User> channel2user   = new HashMap<Channel, User>(32);

    public static void registerUser(Channel channel, String username) {

        InetSocketAddress isa = (InetSocketAddress) channel.getRemoteAddress();
        String ip = isa.getAddress().getHostAddress();
        
        User userMdl = getUserMdl(username);
        ip2user.put(ip, userMdl);
    }
    
    private static User getUserMdl(String username){
        User user = new User();
        return user;
    }
    
    /**
     * Get user, associated with this IO channel
     * 
     */
    
    public static User getUser(Channel channel){
         User user = channel2user.get(channel);
         if (user != null){
             return user;
         }else{
             return attachChannel(channel);
         }
    }
    
    private static User attachChannel(Channel channel){
        InetSocketAddress isa = (InetSocketAddress) channel.getRemoteAddress();
        String ip = isa.getAddress().getHostAddress();
        
        User user = ip2user.get(ip);
        if (user != null){
            channel2user.put(channel, user);
            return user;
        }else{
            throw new RuntimeException("Trying to attach channel to unregistered user ip");
        }
    }
}
