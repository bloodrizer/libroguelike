package com.nuclearunicorn.negame.server;

import com.nuclearunicorn.libroguelike.core.Game;
import com.nuclearunicorn.negame.client.NEGame;
import com.nuclearunicorn.negame.client.game.modes.in_game.InGameMode;
import com.nuclearunicorn.negame.client.game.modes.main_menu.MainMenuMode;
import com.nuclearunicorn.negame.server.core.NEServerCore;
import org.lwjgl.opengl.Display;

import java.applet.Applet;
import java.awt.*;

public class MainApplet extends Applet {
    Canvas display_parent;

    /** Thread which runs the main game loop */
    Thread gameThread;

    /** is the game loop running */
    boolean running = false;

    public static NEGame game;
    public static InGameMode inGameMode;
    public static NEServerCore serverCore;

    public void startLWJGL() {
        gameThread = new Thread() {
            @Override
            public void run() {
                NEGame game;
                running = true;

                inGameMode = new InGameMode();

                /* Run NE Server */
                serverCore = new NEServerCore();
                serverCore.run();

                /* Run Client in the same session and connect it to the server */

                game = new NEGame();
                game.attachServerSession(serverCore);

                game.registerMode("mainMenu", new MainMenuMode());
                game.registerMode("inGame", inGameMode);

                game.set_state("inGame");
                game.set_canvas(display_parent);
                //game.set_state(Game.GameModes.InGame);

                game.run();
            }
        };
        gameThread.start();
    }


    /**
     * Tell game loop to stop running, after which the LWJGL Display will
     * be destoryed. The main thread will wait for the Display.destroy().
     */
    private void stopLWJGL() {
        running = false;
        Game.running = false;

        try {
            gameThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    /**
     * Applet Destroy method will remove the canvas,
     * before canvas is destroyed it will notify stopLWJGL()
     * to stop the main game loop and to destroy the Display
     */
    @Override
    public void destroy() {
        remove(display_parent);
        super.destroy();
    }

    @Override
    public void init() {
        setLayout(new BorderLayout());
        try {
            display_parent = new Canvas() {
                @Override
                public final void addNotify() {
                    super.addNotify();
                    startLWJGL();
                }
                @Override
                public final void removeNotify() {
                    stopLWJGL();
                    super.removeNotify();
                }
            };
            display_parent.setSize(getWidth(),getHeight());
            add(display_parent);
            display_parent.setFocusable(true);
            display_parent.requestFocus();
            display_parent.setIgnoreRepaint(true);
            setVisible(true);
        } catch (Exception e) {
            System.err.println(e);
            throw new RuntimeException("Unable to create display");
        }
    }

    protected void initGL() {

    }

    public void gameLoop() {
        while(running) {
            Display.sync(60);
            Display.update();
        }

        Display.destroy();
    }
}
