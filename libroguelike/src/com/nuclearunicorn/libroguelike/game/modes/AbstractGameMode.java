package com.nuclearunicorn.libroguelike.game.modes;

import com.nuclearunicorn.libroguelike.core.Game;
import com.nuclearunicorn.libroguelike.core.Input;
import com.nuclearunicorn.libroguelike.core.client.ClientEventManager;
import com.nuclearunicorn.libroguelike.events.IEventListener;

/**
 * Created by IntelliJ IDEA.
 * User: Administrator
 * Date: 03.03.12
 * Time: 12:36
 * To change this template use File | Settings | File Templates.
 */
public abstract class AbstractGameMode implements IGameMode, IEventListener {
    private boolean isActive = false;
    private Game gameManager;

    public AbstractGameMode(){
        ClientEventManager.eventManager.subscribe(this);
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public void update(){
            Input.update();
            ClientEventManager.update();

            get_ui().update();
            get_ui().render();
    }

    public Game getGameManager() {
        return gameManager;
    }

    public void setGameManager(Game gameManager) {
        this.gameManager = gameManager;
    }
}
