/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nuclearunicorn.negame.client.clientIo.gameclient;

import com.google.gson.Gson;
import com.nuclearunicorn.libroguelike.core.client.ClientEventManager;
import com.nuclearunicorn.libroguelike.core.client.ClientGameEnvironment;
import com.nuclearunicorn.libroguelike.events.network.EPlayerSpawn;
import com.nuclearunicorn.libroguelike.game.combat.BasicCombat;
import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.game.ent.EntityNPC;
import com.nuclearunicorn.libroguelike.game.ent.buildings.BuildManager;
import com.nuclearunicorn.libroguelike.game.ent.buildings.EntBuilding;
import com.nuclearunicorn.libroguelike.game.player.Player;
import com.nuclearunicorn.negame.client.render.ASCIISpriteEntityRenderer;
import com.nuclearunicorn.negame.client.render.VoxelEntityRenderer;
import com.nuclearunicorn.negame.common.EntityConstants;
import com.nuclearunicorn.negame.common.EventConstants;
import com.nuclearunicorn.negame.server.game.world.entities.EntityCommon;
import com.nuclearunicorn.negame.server.game.world.entities.actors.EntityCommonNPC;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.*;
import org.lwjgl.util.Point;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author bloodrizer
 */
public class GameServClientHandler extends SimpleChannelHandler {
    
    final ClientBootstrap bootstrap;
    

    public GameServClientHandler(ClientBootstrap bootstrap) {
        this.bootstrap = bootstrap;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
        // Close the connection when an exception is raised.
        e.getChannel().close();

        throw new RuntimeException("Game server connection: unexpected exception", e.getCause());
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
        String request = (String) e.getMessage();
        System.err.println("game client: recieved msg: ["+request+"]");

        String[] packet = null;
        if (request != null){
            packet = request.split(" ");
            handlePacket(packet, ctx.getChannel());
        }
    }

     private void handlePacket(String[] packet, Channel ioChannel) {
        //throw new UnsupportedOperationException("Not yet implemented");
        if (packet.length == 0){
            return;
        }
        String eventType = packet[0];

        if (eventType.equals("EPlayerSpawn")){
            int x = Integer.parseInt(packet[1]);
            int y = Integer.parseInt(packet[2]);
            String uid = packet[3];

            System.out.println("NEClient: recieved player's spawn packet, spawning player's entity");

            EPlayerSpawn event = new EPlayerSpawn(new Point(x,y), uid);
            ClientEventManager.addEvent(event);
            //event.post();
        }

        if (eventType.equals(EventConstants.E_ENTITY_SPAWN_NETWORK)){

            //TODO: implement deserialize
            String uid = packet[1];
            int x = Integer.parseInt(packet[2]);
            int y = Integer.parseInt(packet[3]);
            
            System.out.println("spawning entity #"+uid+" @"+x+","+y);

            Gson gson = new Gson();
            HashMap<String, String> entityData = gson.fromJson(packet[4], HashMap.class);
            
            String entClassname = entityData.get("classname");
            EntityCommon ent = null;
            /*if (entClassname.equals(EntityConstants.ENT_NPC)){
                ent = new EntityNPC();
            }*/
            try{
                if (entClassname.equals(EntityConstants.ENT_NPC)){
                    ent = new EntityCommonNPC();
                    //remote player NPC is a regular NPC from the client point of view
                    //no need to create concurent PlayerNPC instances
                } else {
                    ent = (EntityCommon) Class.forName(entClassname).newInstance();
                }
                //Some tricky cornercases go there
            } catch (Exception e) {
                throw new RuntimeException("failed to initialize entity of classname '"+entClassname+"'");
            }

            if (ent == null){
                throw new RuntimeException("failed to initialize entity of classname '"+entClassname+"'");
            }
            
            //ent.initClient();   //required for CommonEntities
            
            ent.setRenderer(new ASCIISpriteEntityRenderer(ent.getCharacter(), ent.getColor()));


            ent.setEnvironment(ClientGameEnvironment.getEnvironment());
            //ent.setRenderer(new VoxelEntityRenderer());

            ent.setLayerId(Player.get_zindex());
            ent.spawn(uid, new Point(x, y));


            //EPlayerSpawn event = new EPlayerSpawn(new Point(x,y), uid);
            //ClientEventManager.addEvent(event);
            //TODO: spawn entity placeholder
        }
    }
}