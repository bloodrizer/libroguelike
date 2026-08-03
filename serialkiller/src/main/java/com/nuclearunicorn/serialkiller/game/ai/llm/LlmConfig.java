package com.nuclearunicorn.serialkiller.game.ai.llm;

import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * LLM-NPC configuration (see LLM_NPC_SPEC.md §10). Plain Gson POJO — field names
 * match the JSON keys. Missing keys keep their defaults below.
 */
public class LlmConfig {

    /** Bundled default template, copied out by scripts/stage-llm-models.sh. */
    private static final String BUNDLED = "/resources/llm/config.json";
    /** External override next to the run script; edited by users, wins if present. */
    private static final String EXTERNAL = "llm-config.json";

    public static class Tier {
        public String model;
        public int port;
        public int cadenceMs;
        public int maxTokens;
        public boolean batch;
    }

    public static class Throttle {
        public String mode = "buckets";   // buckets | uniform
        public int nearRadius = 24;
    }

    public static class Far {
        public boolean teleport = false;
    }

    public static class Memory {
        public int observations = 8;
    }

    public boolean enabled = false;
    public boolean debug = false;
    public String serverBinary = "llama-server";
    public Tier reactor = new Tier();
    public Tier director = new Tier();
    public Throttle throttle = new Throttle();
    public Far far = new Far();
    public Memory memory = new Memory();

    /**
     * Resolution order (§14.1): external file wins, else bundled template, else a
     * disabled default so the game runs unaffected.
     */
    public static LlmConfig load() {
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
