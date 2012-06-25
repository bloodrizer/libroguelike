package com.nuclearunicorn.negame.server.cache;

import com.nuclearunicorn.libroguelike.game.world.WorldChunk;
import org.lwjgl.util.Point;

/**
 * Created by IntelliJ IDEA.
 * User: bloodrizer
 * Date: 25.06.12
 * Time: 12:26
 * To change this template use File | Settings | File Templates.
 */
public interface INECache<Key, Value> {
    public Value get(Key key);
    public void put(Key point, Value chunk);
}
