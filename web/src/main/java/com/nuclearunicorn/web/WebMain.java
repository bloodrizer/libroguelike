package com.nuclearunicorn.web;

import com.nuclearunicorn.serialkiller.game.Main;
import com.nuclearunicorn.serialkiller.game.SkillerGame;
import com.nuclearunicorn.serialkiller.game.modes.in_game.InGameMode;
import com.nuclearunicorn.serialkiller.game.modes.loading.LoadingMode;
import com.nuclearunicorn.serialkiller.game.modes.main_menu.MainMenuMode;
import org.lwjgl.opengl.Display;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;

/**
 * Browser entry point.
 *
 * Mirrors {@link Main#main} — same modes, same order — but hands the frame loop
 * to requestAnimationFrame instead of running Game.run()'s blocking while(),
 * which would freeze the page and never yield to the event loop.
 */
public class WebMain {

    private static SkillerGame game;
    private static HTMLElement status;
    private static int frames;
    private static boolean dead;

    public static void main(String[] args) {
        HTMLDocument doc = Window.current().getDocument();
        status = doc.getElementById("status");

        HTMLCanvasElement canvas = (HTMLCanvasElement) doc.getElementById("gl");
        Display.bind(canvas);

        try {
            Main.inGameMode = new InGameMode();
            game = new SkillerGame();
            Main.game = game;

            game.registerMode("loading", new LoadingMode());
            game.registerMode("mainMenu", new MainMenuMode());
            game.registerMode("inGame", Main.inGameMode);

            game.set_state("loading");
            game.init();
        } catch (Throwable t) {
            fail("init", t);
            return;
        }

        setStatus("running — click the canvas, then WASD to move");
        Window.requestAnimationFrame(WebMain::frame);
    }

    private static void frame(double timestamp) {
        if (dead) {
            return;
        }
        try {
            game.runFrame();
            frames++;
        } catch (Throwable t) {
            fail("frame " + frames, t);
            return;
        }
        Window.requestAnimationFrame(WebMain::frame);
    }

    private static void fail(String where, Throwable t) {
        dead = true;
        String msg = "FAILED at " + where + ": " + t.getClass().getName() + ": " + t.getMessage();
        setStatus(msg);
        if (status != null) {
            status.setClassName("err");
        }
        Assets.log(msg);
        t.printStackTrace();
    }

    private static void setStatus(String s) {
        if (status != null) {
            status.setInnerHTML(s);
        }
    }
}
