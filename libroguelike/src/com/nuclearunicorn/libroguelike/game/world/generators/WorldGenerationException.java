package com.nuclearunicorn.libroguelike.game.world.generators;

/**
 * Created by IntelliJ IDEA.
 * User: Administrator
 * Date: 03.03.12
 * Time: 19:16
 * To change this template use File | Settings | File Templates.
 */
public class WorldGenerationException extends RuntimeException {

    public WorldGenerationException(String cause){
        super(cause);
    }
}
