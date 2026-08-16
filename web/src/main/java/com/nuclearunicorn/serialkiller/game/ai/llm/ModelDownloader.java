package com.nuclearunicorn.serialkiller.game.ai.llm;

/**
 * Web build: models are multi-gigabyte files fetched to disk, which a browser
 * build neither wants nor can store. Reports itself immediately done so the
 * loading screen moves straight on to world generation.
 */
public class ModelDownloader implements Runnable {

    public ModelDownloader(LlmConfig.Tier... tiers) {
    }

    @Override
    public void run() {
    }

    public boolean isDone() {
        return true;
    }

    public String getError() {
        return null;
    }

    public String getStatus() {
        return "no model staging in the browser build";
    }

    public String getSizeLine() {
        return "";
    }

    public float getFraction() {
        return 1.0f;
    }
}
