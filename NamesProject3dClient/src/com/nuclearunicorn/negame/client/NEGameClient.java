package com.nuclearunicorn.negame.client;

/**
 *  Class to contain client-side game instance
 */
public class NEGameClient {
    private static NEGame game = null;

    public static NEGame getNEGame(){
        if (game == null){
            game = new NEGame();
        }
        return game;
    }
}
