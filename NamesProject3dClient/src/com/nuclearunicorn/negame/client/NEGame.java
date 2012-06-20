package com.nuclearunicorn.negame.client;

import com.nuclearunicorn.libroguelike.core.Game;

/**
 * Created by IntelliJ IDEA.
 * User: Administrator
 * Date: 03.03.12
 * Time: 12:24
 * To change this template use File | Settings | File Templates.
 */
public class NEGame extends Game {

    public void initStateUI() {
        activeMode.get_ui().init();
    }
}
