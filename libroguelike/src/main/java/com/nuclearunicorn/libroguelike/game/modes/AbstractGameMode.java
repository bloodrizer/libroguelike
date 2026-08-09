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
        ClientEventManager.subscribe(this);
    }

    public boolean isActive() {
        return isActive;
    }

    /*
     * True only while this mode is the one on screen. isActive() says "has been entered at
     * least once" and never goes back to false, so every mode ever visited keeps handling
     * input - which is how ESC ended up being handled by the menu and the game at once.
     */
    public boolean isCurrent() {
        return Game.getActiveMode() == this;
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
