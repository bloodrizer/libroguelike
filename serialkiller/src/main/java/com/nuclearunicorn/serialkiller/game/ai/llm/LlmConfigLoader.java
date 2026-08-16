package com.nuclearunicorn.serialkiller.game.ai.llm;

import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Where an {@link LlmConfig} comes from on this platform.
 *
 * Split out of LlmConfig so the config schema stays platform-neutral: this is
 * the only part that touches the filesystem, and the browser build swaps it for
 * one that returns the disabled default.
 */
final class LlmConfigLoader {

    private LlmConfigLoader() {
    }

    /** Bundled default template, copied out by scripts/stage-llm-models.sh. */
    private static final String BUNDLED = "/resources/llm/config.json";
    /** External override next to the run script; edited by users, wins if present. */
    private static final String EXTERNAL = "llm-config.json";

    /**
     * Resolution order (§14.1): external file wins, else bundled template, else a
     * disabled default so the game runs unaffected.
     */
    static LlmConfig read() {
        Gson gson = new Gson();

        File external = new File(EXTERNAL);
        if (external.isFile()) {
            try {
                String json = new String(Files.readAllBytes(external.toPath()), StandardCharsets.UTF_8);
                return gson.fromJson(json, LlmConfig.class);
            } catch (IOException e) {
                System.err.println("LlmConfig: failed to read " + EXTERNAL + ", falling back to bundled: " + e);
            }
        }

        try (InputStream is = LlmConfig.class.getResourceAsStream(BUNDLED)) {
            if (is != null) {
                return gson.fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), LlmConfig.class);
            }
        } catch (IOException e) {
            System.err.println("LlmConfig: failed to read bundled template: " + e);
        }

        return new LlmConfig();   // disabled default
    }
}
