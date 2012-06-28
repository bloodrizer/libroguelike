/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nuclearunicorn.negame.server.core;

import com.nuclearunicorn.libroguelike.game.GameEnvironment;
import com.nuclearunicorn.negame.common.IServer;
import com.nuclearunicorn.negame.server.charserv.CharServer;
import com.nuclearunicorn.negame.server.gameserver.GameServer;

/**
 *
 * @author bloodrizer
 */
public class NEServerCore implements IServer {

    CharServer charServer;
    GameServer gameServer;
    
    public NEServerCore(){
        charServer = new CharServer();
        gameServer = new GameServer();
    }
    
    public void run(){
        charServer.run();
        gameServer.run();
    }

    public void destroy(){
        System.out.println("stopping NE Server wrapper");
        charServer.destroy();
        gameServer.destroy();
    }

    public void update() {
        charServer.update();
        gameServer.update();
    }

    public GameEnvironment getEnv() {
        return gameServer.getEnv();
    }

    public CharServer getCharServer() {
        return charServer;
    }
}
