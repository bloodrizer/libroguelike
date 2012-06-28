/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nuclearunicorn.negame.server.core;

import org.jboss.netty.channel.Channel;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    public static List<User> activeUsers = new ArrayList<User>();
    
    public enum CHANNEL_TYPE {
        CHANNEL_CHARSERV,
        CHANNEL_GAMESERV
    }

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
    
    public static User getUser(Channel channel, CHANNEL_TYPE channelType){

         for (User activeUser: activeUsers){
            if (channelType == CHANNEL_TYPE.CHANNEL_CHARSERV){
                if (activeUser.charChannel == channel){
                    return activeUser;
                }
            }

            if (channelType == CHANNEL_TYPE.CHANNEL_GAMESERV){
                 if (activeUser.gameChannel == channel){
                     return activeUser;
                 }
            }
         }

         return attachChannel(channel, channelType);
    }

    /*
        This method resolves channel to the user ip and attach channel to it
     */
    private static User attachChannel(Channel channel, CHANNEL_TYPE channelType){
        InetSocketAddress isa = (InetSocketAddress) channel.getRemoteAddress();
        String ip = isa.getAddress().getHostAddress();
        
        User user = ip2user.get(ip);
        if (user != null){
            if (channelType == CHANNEL_TYPE.CHANNEL_CHARSERV){
                user.setCharChannel(channel);
            }
            if (channelType == CHANNEL_TYPE.CHANNEL_GAMESERV){
                user.setGameChannel(channel);
            }
            return user;
        }else{
            throw new RuntimeException("Trying to attach channel to unregistered user ip");
        }
    }

    public static List<User> getActiveUsers(){
        return activeUsers;
    }

}
