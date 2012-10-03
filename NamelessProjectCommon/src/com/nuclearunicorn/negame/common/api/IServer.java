package com.nuclearunicorn.negame.common.api;

import com.nuclearunicorn.libroguelike.game.GameEnvironment;

/**
 * Created by IntelliJ IDEA.
 * User: bloodrizer
 * Date: 25.06.12
 * Time: 16:35
 * To change this template use File | Settings | File Templates.
 */

/*
Default server interface for cross-project dependency resolution
 */
public interface IServer {
    public void update();

    public GameEnvironment getWorldEnvironment();
}
