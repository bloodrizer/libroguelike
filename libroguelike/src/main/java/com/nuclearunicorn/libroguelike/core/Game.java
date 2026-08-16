/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.core;


import com.nuclearunicorn.libroguelike.game.modes.AbstractGameMode;
import com.nuclearunicorn.libroguelike.game.modes.IGameMode;
import com.nuclearunicorn.libroguelike.core.replay.Replay;
import com.nuclearunicorn.libroguelike.game.ui.IUserInterface;
import com.nuclearunicorn.libroguelike.render.ScreenCapture;
import com.nuclearunicorn.libroguelike.render.WindowRender;
import com.nuclearunicorn.libroguelike.utils.Timer;
import com.nuclearunicorn.libroguelike.vgui.NE_GUI_Element;
import com.nuclearunicorn.libroguelike.vgui.NE_GUI_System;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Administrator
 */
public class Game {

    protected Map<String, AbstractGameMode> gameStates = new HashMap<String,AbstractGameMode>();
    protected static AbstractGameMode activeMode = null;
    
    public void set_state(String state){
        AbstractGameMode mode = gameStates.get(state);
        this.activeMode = mode;

    }

    /*
     * The mode currently on screen. Unlike get_game_mode() this has no lazy-activation side
     * effects, so it is safe to ask from an event handler.
     */
    public static AbstractGameMode getActiveMode(){
        return activeMode;
    }

    public void resetState(String state) {
        AbstractGameMode mode = gameStates.get(state);
        try {

            AbstractGameMode _mode = mode.getClass().newInstance();
            gameStates.put(state, _mode );
            //_mode.setActive(false);

        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public void registerMode(String name, AbstractGameMode mode) {
        gameStates.put(name, mode);
        mode.setGameManager(this);
    }

    public static AbstractGameMode get_game_mode(){

         if (!activeMode.isActive()){

             activeMode.setActive(true);
             activeMode.run();

             IUserInterface gameUI = activeMode.get_ui();
             gameUI.build_ui();

         }
         return activeMode;
    }

    /* Typed as Object, not java.awt.Canvas, so core has no AWT dependency — the
     * browser build has no java.desktop at all. WindowRender casts it back. */
    public static Object display_parent = null;
    public static void set_canvas(Object display_parent){
        Game.display_parent = display_parent;
    }

    //TODO: refact me
    public static boolean running = true;

    /*
     * One iteration of the main loop. Split out of run() so a caller that does not
     * own the loop can drive frames itself — the browser build ticks this from
     * requestAnimationFrame, since a blocking while() would freeze the page.
     */
    public void runFrame(){
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        IGameMode mode = get_game_mode();

        Timer.tick();
        Replay.nextFrame();
        mode.update();

        ScreenCapture.tick();

        Display.sync(60);
        Display.update();

        if (Display.isCloseRequested()){
            onCloseDisplay();
            running = false;
        }
    }

    /* Boot the render context and replay recorder. Separate from the loop so the
     * browser build can start up without entering one. */
    public void init() throws Exception {
        WindowRender.create();
        Replay.init();
    }

    public void run(){
        try {
            init();

            while(running) {
                runFrame();
            }

            System.out.println("Game stopped, destroying lwjgl render...");
            Replay.shutdown();
            WindowRender.destroy();

        }
        catch(Exception e){
            e.printStackTrace();
        }

    }

    /*
        Override it to do some shit
     */
    protected void onCloseDisplay() {
    }

    public static void stop() {
        running = false;
    }

    /*
     * Helper function, returning root gui element of current game mode
     * Obviously, as you can see below, it is very useful;
     */
    public static NE_GUI_Element get_ui_root(){
        return get_ui().root;
    }

    public static NE_GUI_System get_ui() {
        return get_game_mode().get_ui().get_nge_ui();
    }
}
