/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nuclearunicorn.negame.client.clientIo.charclient;

import com.nuclearunicorn.libroguelike.core.client.ClientEventManager;
import com.nuclearunicorn.libroguelike.events.EPlayerAuthorise;
import com.nuclearunicorn.libroguelike.game.player.Player;
import com.nuclearunicorn.negame.client.clientIo.NettyClient;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.*;

import java.net.InetSocketAddress;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 * @author bloodrizer
 */
class CharClientHandler extends SimpleChannelHandler {
        
    final ClientBootstrap bootstrap;

    public CharClientHandler(ClientBootstrap bootstrap) {
        this.bootstrap = bootstrap;
    }

    InetSocketAddress getRemoteAddress() {
        return (InetSocketAddress) bootstrap.getOption("remoteAddress");
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
        String request = (String) e.getMessage();
        System.err.println("char client: recieved msg: ["+request+"]");
        
        String[] packet = null;
        if (request != null){
            packet = request.split(" ");
            handlePacket(packet, ctx.getChannel());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
        e.getCause().printStackTrace();
        e.getChannel().close();
    }

     @Override
     public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) {
        System.out.println("Connected to: " + getRemoteAddress());
     }
     
     //-------------------------------------------------------------------------

     private static void sendMsg(String msg, Channel ioChannel){
        ioChannel.write(msg+"\r\n");
     }
     
     private void handlePacket(String[] packet, Channel ioChannel) {
        System.out.println("handling data packet '" + packet[0] + "'");

        if (packet.length == 0){
            return;
        }
        String eventType = packet[0];
        
        if (eventType.equals("EPlayerAuthorize")){
            //Initialization problem there
            System.out.println("Initiating EPlayerAuthorize");
            EPlayerAuthorise event = new EPlayerAuthorise();
            event.setManager(ClientEventManager.getEventManager());
            event.post();

            return;
        }
        
        if (eventType.equals("EPlayerAccepted")){
            try {
                /*
                 * Character server accepted our connection, so we could
                 * connect to game server
                 */
                //TODO: move to the NEClient
                
                String host = packet[1];
                int    port = Integer.parseInt(packet[2]);
                int  userId = Integer.parseInt(packet[3]);
                
                NettyClient.gameServConnect(host, port);
                
                //------------------------------------------------
                //Meanwhile...
                
                //Enter InGame mode
                //Main.game.set_state("inGame");

                //set correct player id
                Player.character_id = userId;
                //some vague shit there

                //((InGameMode)(Main.inGameMode)).spawnPlayer(new Point(5, 5));
                //waiting for spawn event
                //------------------------------------------------------------------
            } catch (Exception ex) {
                Logger.getLogger(CharClientHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
            return;
        }
        
     }
     

}
