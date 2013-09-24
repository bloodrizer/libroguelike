/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.negame.server.gameserver;

import com.nuclearunicorn.libroguelike.events.EEntitySpawn;
import com.nuclearunicorn.libroguelike.events.Event;
import com.nuclearunicorn.libroguelike.events.EventManager;
import com.nuclearunicorn.libroguelike.events.IEventListener;
import com.nuclearunicorn.libroguelike.events.network.EEntityMove;
import com.nuclearunicorn.libroguelike.game.player.Player;
import com.nuclearunicorn.negame.common.events.EEntitySpawnNetwork;
import com.nuclearunicorn.libroguelike.events.network.NetworkEvent;
import com.nuclearunicorn.libroguelike.game.GameEnvironment;
import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.game.ent.controller.NpcController;
import com.nuclearunicorn.libroguelike.game.world.WorldChunk;
import com.nuclearunicorn.libroguelike.game.world.WorldModel;
import com.nuclearunicorn.libroguelike.game.world.layers.WorldLayer;
import com.nuclearunicorn.negame.common.EventConstants;
import com.nuclearunicorn.negame.common.IoCommon;
import com.nuclearunicorn.negame.server.core.AServerIoLayer;
import com.nuclearunicorn.negame.server.core.NEDataPacket;
import com.nuclearunicorn.negame.server.core.ServerUserPool;
import com.nuclearunicorn.negame.server.core.User;
import com.nuclearunicorn.negame.server.game.world.ServerWorldModel;
import com.nuclearunicorn.negame.server.game.world.entities.EntityPlayerNPC;
import com.nuclearunicorn.negame.server.generators.NEServerGroundChunkGenerator;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.frame.DelimiterBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.Delimiters;
import org.jboss.netty.handler.codec.string.StringDecoder;
import org.jboss.netty.handler.codec.string.StringEncoder;
import org.lwjgl.util.Point;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;


/**
 *
 * @author Administrator
 */
public class GameServer extends AServerIoLayer implements IEventListener {
    NioServerSocketChannelFactory nio_factory;


    GameEnvironment gameEnv;
    EventManager eventManager;

    public GameServer(){
        super("game-server");

        System.out.println("Loading server environment...");

        eventManager = new EventManager();

        gameEnv = new GameEnvironment("ne-server-game-environment") {
            {
                clientWorld = new ServerWorldModel();
                clientWorld.setEnvironment(this);
            }

            @Override
            public EventManager getEventManager() {
                return eventManager;
            }

            @Override
            public WorldModel getWorld() {
                return clientWorld;
            }
        };

        WorldModel model = gameEnv.getWorld();
        model.setName("server-world");

        ArrayList<WorldLayer> layers = new ArrayList<WorldLayer>(model.getLayers());
        layers.get(0).registerGenerator(new NEServerGroundChunkGenerator());

        gameEnv.getEventManager().subscribe(this);
    }


    public void run(){
        System.out.println("Starting local game server on "+ IoCommon.GAME_SERVER_PORT);
        
        handler = new GameServerHandler(this);

        nio_factory = new NioServerSocketChannelFactory(
            Executors.newCachedThreadPool(),
            Executors.newCachedThreadPool());

        bootstrap = new ServerBootstrap(
            nio_factory
        );

        // Set up the pipeline factory.

        //this shit not works yet
        bootstrap.setPipelineFactory(new ChannelPipelineFactory() {

            public ChannelPipeline getPipeline() throws Exception {
                ChannelPipeline pipeline = Channels.pipeline();

                pipeline.addLast("framer", new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
                pipeline.addLast("decoder", new StringDecoder());
                pipeline.addLast("encoder", new StringEncoder());

                pipeline.addLast("handler", handler);

                return pipeline;
            }
        });

        // Bind and start to accept incoming connections.
        Channel srvChannel = bootstrap.bind(new InetSocketAddress(IoCommon.GAME_SERVER_PORT));
        allChannels.add(srvChannel);
    }



    public GameEnvironment getEnv() {
        return gameEnv;
    }

    @Override
    public void destroy(){
        super.destroy();
        
        /*Cache cache = cacheManager.getCache("chunkCache");
        System.out.println("Saving chunk data");
        for (Point key: gameEnv.getWorldLayer(0).chunk_data.keySet()){
            WorldChunk chunk = gameEnv.getWorldLayer(0).chunk_data.get(key);
            Element element = new Element(key, chunk);

            cache.put(element);
        }*/


        System.out.println("Flushing down cache");
        //cache.flush();

        System.out.println("Shutting down cache manager");
        //cacheManager.shutdown();
    }

    @Override
    public void update() {
        synchronized (this){
            super.update();

            gameEnv.getWorld().update();
            gameEnv.getEntityManager().update();
        }
    }

    @Override
    /**
     * This method is called whenever DATA PACKED is recieved from client
     * The server's duty is to handle this packet and rise appropriate event
     */
    protected void handlePacket(NEDataPacket packet) {
        String[] data = packet.getData();
        Channel ioChannel = packet.getChannel();

        if (data.length == 0){
            return;
        }
        String eventType = data[0];

        System.err.println("handling event '"+eventType+"'");

        if (eventType.equals(EventConstants.E_ENTITY_SET_PATH)){
            int x = Integer.parseInt(data[1]);
            int y = Integer.parseInt(data[2]);

            User user = ServerUserPool.getUser(ioChannel, ServerUserPool.CHANNEL_TYPE.CHANNEL_GAMESERV);
            moveUser(user, x, y);
            
        }
    }

    private void moveUser(User user, int x, int y) {
        Entity ent = user.getEntity();

        if(ent == null){
            throw new RuntimeException("trying to move NULL user entity");
        }

        Point destCoord = new Point(x,y);

        ((NpcController) ent.controller).set_destination(destCoord);

        ent.move_to(new Point(x, y));
        //Point chunkCoord = WorldChunk.get_chunk_coord(destCoord);
        //worldUpdateLazyLoad(chunkCoord.getX(),chunkCoord.getY());
    }

    @Override
    public void e_on_event(Event event) {
        if (event instanceof EEntitySpawn){
            if (((EEntitySpawn) event).ent instanceof EntityPlayerNPC){
                EntityPlayerNPC entPlayerNpc = (EntityPlayerNPC)((EEntitySpawn) event).ent;

                //if player is spawned on server, notify every other player of this event
                User npcUser = entPlayerNpc.getUser();
                broadcostUserEvent(event, npcUser);
            }
        }
        if (event instanceof EEntityMove){
            EEntityMove moveEvent = (EEntityMove)event;

            Point chunkCoord = WorldChunk.get_chunk_coord(moveEvent.getTo());
            worldUpdateLazyLoad(chunkCoord.getX(),chunkCoord.getY());
            broadcostEvent(event);
        }
    }

     /**
     * Spawns player character binded to the connection channel
     */
    public void spawnPlayerCharacter(User user) {
        GameEnvironment env = getEnv();

        Entity mplayer_ent = new EntityPlayerNPC(user);
        mplayer_ent.setEnvironment(env);
        mplayer_ent.set_controller(new NpcController(env));

        //TODO: load layer
        mplayer_ent.setLayerId(0);
        mplayer_ent.setEnvironment(env);

        //EntityManager.add(mplayer_ent);       ?
        //TODO: get player locat
        mplayer_ent.spawn(getUserLocation(user));

        user.setEntity(mplayer_ent);
        //worldUpdateLazyLoad(0,0);

        //stream 3x3 chunk data
        WorldChunk chunk = mplayer_ent.get_chunk();
        //notifyChunkData(user);

        WorldLayer serverGroundLayer = getEnv().getWorldLayer(WorldLayer.GROUND_LAYER);
        for (int i = chunk.origin.getX()-1; i<=chunk.origin.getX(); i++){
            for (int j = chunk.origin.getY()-1; j<=chunk.origin.getY(); j++){
                //if chunk doest not exist, it will be generated and populated with game objects
                WorldChunk cachedChunk = serverGroundLayer.get_cached_chunk(i, j);
                notifyChunkData(user, cachedChunk);
            }
        }
        //once player has spawned, notify all active players of generic npc spawn

        EEntitySpawnNetwork spawnEvent = new EEntitySpawnNetwork(mplayer_ent, mplayer_ent.origin);
        broadcostUserEvent(spawnEvent, user);
    }


    public void removePlayerCharacter(User user) {
        GameEnvironment env = getEnv();

        Entity playerEnt = Player.get_ent();
        //we may have a special case here where player entity failed to initialize
        if (playerEnt != null){
            playerEnt.trash();
        }
    }

    /**
       Return player position based on saved user data
     */
    private Point getUserLocation(User user) {

        int rndX = (int)(Math.random()*32.0f);
        int rndY = (int)(Math.random()*32.0f);
        Point playerOrigin = new Point(rndX, rndY);
        return playerOrigin;
    }


    /**
     * Preloads 3x3 chunk cluster so pathfinding could work correctly near the chunk border
     */
    private void worldUpdateLazyLoad(int x, int y){
        WorldLayer serverGroundLayer = getEnv().getWorldLayer(WorldLayer.GROUND_LAYER);
        for (int i = x-1; i<= x+1; i++){
            for (int j = y-1; j<= y+1; j++){
                serverGroundLayer.get_cached_chunk(i, j);
            }
        }
    }


    /*
        This method takes all entities in the chunk and stream them to the player
        TODO: introduce some compact bundle format a-la json
        Note that this method passes simple data only, i.e. coords, uid and simple ent type
        Client responsibility is to request additional information about given entity
     */
    public void notifyChunkData(User observer, WorldChunk chunk){
        Entity userEnt = observer.getEntity();
        Channel userChannel = observer.getGameChannel();

        List<Entity> entityList = chunk.getEntList();
        if (entityList.isEmpty()){
            return; //do not even bother to send chunk data
        }

        System.err.println("Sending chunk data to user #"+observer.getId() + "(" + entityList.size() + " entities total)");
        for (Entity chunkEnt: entityList){
            if (!chunkEnt.equals(userEnt)){
                System.out.println("forcing client to spawn entity #"+chunkEnt.get_uid()+" @"+ chunkEnt.origin.getX() + "," + chunkEnt.origin.getY());
                EEntitySpawnNetwork spawnEvent = new EEntitySpawnNetwork(chunkEnt, chunkEnt.origin);
                sendEvent(spawnEvent, userChannel);
            }
        }
    }

    /**
        Broadcost user-triggered event to every user except observer
        Typical situation is player spawn, when everyone should be aware of this event except user itself
     */
    private void broadcostUserEvent(Event event, User observer){
        if (event.is_local()){
            return;
        }

        List<User> users = ServerUserPool.getActiveUsers();
        for(User user : users){
            if (! user.getGameChannel().equals(observer.getGameChannel()) ){
                sendEvent(event, user.getGameChannel());
            }
        }
    }

    private void broadcostEvent(Event event){
        if (event.is_local()){
            return;
        }
        
        List<User> users = ServerUserPool.getActiveUsers();
        for(User user : users){
            sendEvent(event, user.getGameChannel());
        }
    }
    /*
    Todo: this method looks similar to the client logic, probably need to extract as EventDispatcher class
     */
    private void sendEvent(Event event, Channel channel) {
        NetworkEvent networkEvent = (NetworkEvent)event;

        String[] tokens = networkEvent.serialize();
        StringBuilder sb = new StringBuilder();

        sb.append(event.classname().concat(" "));

        for (int i = 0; i<tokens.length; i++){
            sb.append(tokens[i].concat(" "));
        }
        sendMsg(sb.toString(), channel);
    }

}
