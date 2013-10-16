package com.nuclearunicorn.negame.server;

import com.nuclearunicorn.negame.client.NEGame;
import com.nuclearunicorn.negame.client.NEGameClient;
import com.nuclearunicorn.negame.client.game.modes.in_game.InGameMode;
import com.nuclearunicorn.negame.client.game.modes.main_menu.MainMenuMode;
import com.nuclearunicorn.negame.server.core.NEServerCore;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;

/**
 * Main server class
 *
 * Server will launch additional client instance
 * and attach it's update loop to the client loop
 *
 */
public class Main {

    public static InGameMode inGameMode;
    public static NEServerCore serverCore;

    public static void main(String[] args) {

        PropertyConfigurator.configure("application.properties");

        inGameMode = new InGameMode();

        // Run NE Server
        serverCore = new NEServerCore();
        serverCore.run();

        //Run Client in the same session and connect it to the server
        NEGame game = NEGameClient.getNEGame();

        game.attachServerSession(serverCore);

        game.registerMode("mainMenu", new MainMenuMode());
        game.registerMode("inGame", inGameMode);

        game.set_state("inGame");

        try {
            game.run();
        }
        finally{
            game.running = false;
            serverCore.destroy();
        }
    }
}
