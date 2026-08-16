package com.nuclearunicorn.serialkiller.game.modes.loading;

import com.nuclearunicorn.libroguelike.game.ui.IUserInterface;
import com.nuclearunicorn.libroguelike.vgui.NE_GUI_System;

/** No widgets — the loading screen is drawn straight through the text overlay. */
public class LoadingUI implements IUserInterface {

    private final NE_GUI_System ui = new NE_GUI_System();

    @Override
    public void build_ui() {
    }

    @Override
    public NE_GUI_System get_nge_ui() {
        return ui;
    }

    @Override
    public void update() {
    }

    @Override
    public void render() {
    }

    @Override
    public void init() {
    }
}
