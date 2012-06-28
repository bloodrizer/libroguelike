package com.nuclearunicorn.negame.client;

import com.nuclearunicorn.libroguelike.core.Game;
import com.nuclearunicorn.negame.client.clientIo.NettyClient;
import com.nuclearunicorn.negame.common.IServer;

/**
 * Created by IntelliJ IDEA.
 * User: Administrator
 * Date: 03.03.12
 * Time: 12:24
 * To change this template use File | Settings | File Templates.
 */
public class NEGame extends Game {
    
    protected IServer server;

    /**
     * Attach active server session. Client will call server.update() in the local update thread of the game
     * to reduce CPU usage footprint
     */
    public void attachServerSession(IServer server){
        this.server = server;
    }

    public void updateServerSession(){
        if (server != null){
            server.update();
        }
    }

    public void initStateUI() {
        activeMode.get_ui().init();
    }

    @Override
    public void run() {
        super.run();
        NettyClient.destroy();
    }
}
