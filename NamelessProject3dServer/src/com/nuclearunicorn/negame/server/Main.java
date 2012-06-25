package com.nuclearunicorn.negame.server;

import com.nuclearunicorn.negame.client.NEGame;
import com.nuclearunicorn.negame.client.game.modes.in_game.InGameMode;
import com.nuclearunicorn.negame.client.game.modes.main_menu.MainMenuMode;
import com.nuclearunicorn.negame.server.core.NEServerCore;

/**
 * Created by IntelliJ IDEA.
 * User: bloodrizer
 * Date: 25.06.12
 * Time: 12:04
 * To change this template use File | Settings | File Templates.
 */
public class Main {

    public static NEGame game;
    public static InGameMode inGameMode = new InGameMode();

    public static void main(String[] args) {
        /* Run NE Server */
        NEServerCore serverCore = new NEServerCore();
        serverCore.run();

        /* Run Client in the same session and connect it to the server */

        game = new NEGame();

        game.registerMode("mainMenu", new MainMenuMode());
        game.registerMode("inGame", inGameMode);

        game.set_state("inGame");
        game.run();

        //when client is done, terminate server

        serverCore.destroy();
    }
}
