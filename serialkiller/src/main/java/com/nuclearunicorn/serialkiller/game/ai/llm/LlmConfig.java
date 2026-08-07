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
    /** System property that forces "enabled" either way, whatever the files say. */
    private static final String OVERRIDE = "llm.enabled";

    public static class Tier {
        public String model;
        public String url;      // where ModelDownloader fetches "model" from if it's missing
        public int port;
        public int cadenceMs;
        public int maxTokens;
        public boolean batch;
        /**
         * Turns between ambient re-plans. Cadence is counted in turns, not milliseconds:
         * the world only advances when the player acts, so a wall clock throttles a
         * standing-still player and outruns one holding shift.
         */
        public int cadenceTurns = 8;
        /** Pending requests the queue holds before it starts dropping the least salient. */
        public int queueCapacity = 12;
    }

    /** Hearing sensor ranges, in tiles (§8). Distance is what makes speech feel addressed. */
    public static class Speech {
        /** Spoken this close and the NPC treats it as addressed to them (DIRECTED). */
        public int directedRadius = 4;
        /** Heard out to here, but only overheard (NOTABLE). */
        public int earshotRadius = 10;
    }

    /** How signal priority resolves against a running plan (§9). */
    public static class Priority {
        /** Stimuli at or above this salience preempt the current plan and jump the queue. */
        public int interruptAt = 70;          // Salience.DIRECTED
        /** Salience lost per turn of age, so nothing stays urgent forever. */
        public int decayPerTurn = 2;
        /** Turns an NPC stays focused on whoever addressed it. */
        public int attentionTurns = 12;
        /** Ambient re-plan cadence while holding attention — conversation ticks faster. */
        public int attentionCadenceTurns = 2;
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
    public Speech speech = new Speech();
    public Priority priority = new Priority();

    /**
     * Resolution order (§14.1): external file wins, else bundled template, else a
     * disabled default so the game runs unaffected.
     */
    public static LlmConfig load() {
        LlmConfig config = read();

        // Escape hatch for tooling that wants the plain game: -Dllm.enabled=false skips
        // model staging and the servers entirely (see scripts/shot.sh).
        String override = System.getProperty(OVERRIDE);
        if (override != null) {
            config.enabled = Boolean.parseBoolean(override);
        }
        return config;
    }

    private static LlmConfig read() {
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
