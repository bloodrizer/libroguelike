/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nuclearunicorn.negame.server.core;

import org.jboss.netty.channel.Channel;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Set;

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

    public static Set<Channel> getActiveChannels() {
        return channel2user.keySet();
    }

    /*
        Returns io channel session assigned to current user, null otherwise
     */
    public static Channel getUserChannel(User observer) {
        for (Channel channel : channel2user.keySet()){
            if(channel2user.get(channel).equals(observer)){
                return channel;
            }
        }
        return null;
    }

    /*
        Check if given channel session is assigned to user
     */
    public static boolean isUserChannel(User observer, Channel channel) {
        return channel2user.get(channel).equals(observer);
    }
}
