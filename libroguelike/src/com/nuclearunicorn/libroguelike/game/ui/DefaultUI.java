/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.game.ui;


import com.nuclearunicorn.libroguelike.vgui.NE_GUI_System;

/**
 *
 * @author Administrator
 */
public class DefaultUI implements IUserInterface {

    public void build_ui(){

    }
    
    NE_GUI_System ui;
    public NE_GUI_System get_nge_ui() {
        return ui;
    }

    public void update() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void render() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void init() {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
