package com.nuclearunicorn.negame.client;

import com.nuclearunicorn.negame.client.game.modes.in_game.InGameMode;
import com.nuclearunicorn.negame.client.game.modes.main_menu.MainMenuMode;

public class Main {


    private static NEGame game;
    public static InGameMode inGameMode;

    public static void main(String[] args) {
        inGameMode = new InGameMode();

        System.out.println("------- starting game client ---------");

        game = NEGameClient.getNEGame();

        game.registerMode("mainMenu", new MainMenuMode());
        game.registerMode("inGame", inGameMode);

        game.set_state("inGame");
        game.run();

    }


}
