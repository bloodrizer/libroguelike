/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nuclearunicorn.negame.client.clientIo;


import com.nuclearunicorn.libroguelike.core.client.ClientEventManager;
import com.nuclearunicorn.negame.client.clientIo.charclient.CharClientPipelineFactory;
import com.nuclearunicorn.negame.client.clientIo.gameclient.GameClientPipelineFactory;
import com.nuclearunicorn.negame.common.EventConstants;
import com.nuclearunicorn.negame.common.IoCommon;

import java.util.logging.Level;
import java.util.logging.Logger;



/**
 *
 * @author bloodrizer
 */
public class NettyClient {
    
    static final String host = "localhost";
    static final int port = IoCommon.CHAR_SERVER_PORT;
        
    final static NettyClientLayer charServClient = new NettyClientLayer(host, port, "charserv-client-layer") {{
        packetFilter.add(EventConstants.E_SELECT_CHARACTER);
        //packetFilter.add(EventConstants.E_PLAYER_LOGON);
    }};
    static {
        charServClient.name = "charserv client";
    }


    public static void connect(){
        //return;
        
        System.out.println("NEClient:>> Connecting to the character server...");
        

        ClientEventManager.subscribe(charServClient);


        charServClient.setPipelineFactory(new CharClientPipelineFactory(charServClient.bootstrap));

        try {
            charServClient.connect(
            );
         System.out.println("NEClient:>> Connected successfully");
                            
         System.out.println("NEClient:>> Sending login command");
         charServClient.sendMsg("EPlayerLogin Red True");    
            
            
        } catch (Exception ex) {
            Logger.getLogger(NettyClient.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
    }

    public static void gameServConnect(String host, int port){
        
        System.out.println("NEClient:>> Starting gameServListening thread @"+host+":"+port+" (kinda)");


        Thread chrSrvThread = new Thread(new GameServConnectionThread(host, port));
        chrSrvThread.setDaemon(true);
        chrSrvThread.start();
        

    }

    static NettyClientLayer gameServClient;
    
    private static class GameServConnectionThread implements Runnable{
        
        String host;
        int port;
        
        public GameServConnectionThread(String host, int port){
            this.host = host;
            this.port = port;
        }

        public void run() {
            gameServClient = new NettyClientLayer(host,port, "gameserv-client-layer") {{
                packetFilter.add(EventConstants.E_ENTITY_SET_PATH);
                packetFilter.add("events.network.EBuildStructure");
            }};
            gameServClient.name = "gameserv client";
            ClientEventManager.subscribe(gameServClient);


            gameServClient.setPipelineFactory(new GameClientPipelineFactory(gameServClient.bootstrap));
            try {
                gameServClient.connect();
            } catch (Exception ex) {
                Logger.getLogger(NettyClient.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public static void destroy(){
        charServClient.destroy();
        if (gameServClient != null){
            gameServClient.destroy();
        }
    }
}
