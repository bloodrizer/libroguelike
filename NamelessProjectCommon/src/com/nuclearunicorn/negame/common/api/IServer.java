package com.nuclearunicorn.negame.common.api;

import com.nuclearunicorn.libroguelike.game.GameEnvironment;

/*
Default server interface for cross-project dependency resolution
 */
public interface IServer {
    public void update();

    public GameEnvironment getWorldEnvironment();
}
