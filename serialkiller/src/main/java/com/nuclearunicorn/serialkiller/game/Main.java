package com.nuclearunicorn.serialkiller.game;

import com.nuclearunicorn.serialkiller.game.modes.in_game.InGameMode;
import com.nuclearunicorn.serialkiller.game.modes.loading.LoadingMode;
import com.nuclearunicorn.serialkiller.game.modes.main_menu.MainMenuMode;

/**
 * Created by IntelliJ IDEA.
 * User: Administrator
 * Date: 03.03.12
 * Time: 12:23
 * To change this template use File | Settings | File Templates.
 */
public class Main {


    public static SkillerGame game;

    public static InGameMode inGameMode;

    public static void main(String[] args) {
        //diagnose the LLM stack and exit - no window, no world, see LlmDoctor
        for (String arg : args) {
            if ("--llm-check".equals(arg)) {
                System.exit(com.nuclearunicorn.serialkiller.game.ai.llm.LlmDoctor.run());
            }
        }

        // -Dlrl.seed=N fixes the town, so two builds can be screenshotted side
        // by side; without it the run picks its own and reports it as before.
        String seed = System.getProperty("lrl.seed");
        if (seed != null) {
            com.nuclearunicorn.libroguelike.utils.Rng.seed(Long.parseLong(seed.trim()));
            System.out.println("[rng] seeded " + seed.trim());
        }

        inGameMode = new InGameMode();

        game = new SkillerGame();

        game.registerMode("loading", new LoadingMode());
        game.registerMode("mainMenu", new MainMenuMode());
        game.registerMode("inGame", inGameMode);

        //loading stages the LLM models, then hands off to inGame
        game.set_state("loading");
        game.run();

    }


}
