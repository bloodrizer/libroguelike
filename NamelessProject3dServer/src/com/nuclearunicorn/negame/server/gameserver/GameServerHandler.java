/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.negame.server.gameserver;

import com.nuclearunicorn.libroguelike.events.network.NetworkEvent;
import com.nuclearunicorn.negame.server.core.*;
import org.jboss.netty.channel.*;

import java.util.concurrent.atomic.AtomicLong;



/**
 *
 * @author Administrator
 */
public class GameServerHandler extends AServerHandler {

    private final AtomicLong transferredBytes = new AtomicLong();
    AServerIoLayer server;

    public GameServer getServer(){
        return (GameServer)server;
    }

    public GameServerHandler(AServerIoLayer server){
        this.server = server;
    }

    public long getTransferredBytes() {
        return transferredBytes.get();
    }

    @Override
    public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) {
        
        Channel channel = e.getChannel();
        User user = ServerUserPool.getUser(channel);
        
        //Remote user connected to the game server
        if(user == null){
            throw new RuntimeException("Game Server: remote connection from unregistered user");
        }
        
        ((GameServer)server).registerUser(user);
        server.allChannels.add(channel);


        //spawn player entity on server side
        try{
            getServer().spawnPlayerCharacter(user);
        }catch (Exception ex){
            ex.printStackTrace();
        }

        //get entity parameters and push them back to the client since we need to keep channel context
        int x = user.getEntity().x();
        int y = user.getEntity().y();

        String uid = user.getEntity().get_uid();
        sendMsg("EPlayerSpawn "+x+" "+y+" "+uid, channel);
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {

        String request = (String) e.getMessage();
        System.err.println("Netty Game server: recived ["+request+"]");

        String[] packet = null;
        if (request != null){
            packet = request.split(" ");
            server.registerPacket(new NEDataPacket(packet, ctx.getChannel()));
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
        // Close the connection when an exception is raised.
        e.getChannel().close();

        e.getCause().printStackTrace();

        //throw new RuntimeException("Unexpected exception from downstream", e.getCause());
    }

    private void sendNetworkEvent(NetworkEvent event){

        String[] tokens = event.serialize();
        StringBuilder sb = new StringBuilder();

        sb.append(event.classname().concat(" "));

        for (int i = 1; i<tokens.length; i++){
            sb.append(tokens[i].concat(" "));
        }
    }
}

