/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.game.modes;

import com.nuclearunicorn.libroguelike.game.ui.IUserInterface;

/**
 *
 * @author Administrator
 */
public interface IGameMode {
    public void run();
    public void update();
    
    public IUserInterface get_ui();
}
