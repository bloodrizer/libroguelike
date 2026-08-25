package com.nuclearunicorn.serialkiller.game.ai.llm;



/**
 * LLM-NPC configuration (see LLM_NPC_SPEC.md §10). Plain Gson POJO — field names
 * match the JSON keys. Missing keys keep their defaults below.
 */
public class LlmConfig {

    /** System property that forces "enabled" either way, whatever the files say. */
    private static final String OVERRIDE = "llm.enabled";
    /** Same, for the prompt dump — so a debugging run needs no edit to a checked-in file. */
    private static final String PROMPT_OVERRIDE = "llm.debugPrompts";

    public static class Tier {
        public String model;
        public String url;      // where ModelDownloader fetches "model" from if it's missing
        public int port;
        public int cadenceMs;
        public int maxTokens;
        public boolean batch;
        /**
         * CPU threads handed to llama-server as {@code -t}. 0 lets llama-server pick its own
         * (all cores). Split when two tiers share a box, so the two models don't thrash each
         * other: e.g. reactor 12 + director 4 on a 16-core machine.
         */
        public int threads = 0;
        /**
         * Context window handed to llama-server as {@code -c}. 0 keeps its default, which
         * recent builds take from the model card — 32k on Qwen2.5, whose KV cache costs
         * gigabytes and seconds the prompts here never use. A couple of thousand is plenty.
         */
        public int contextSize = 0;
        /** Layers offloaded to the GPU ({@code -ngl}). 0 is CPU-only. */
        public int gpuLayers = 0;
        /**
         * How long to wait for {@code /health} before giving up on this tier. A 9GB model
         * off a cold page cache does not load in the 60s this used to be fixed at; a server
         * that dies is noticed at once regardless, so a generous ceiling costs nothing.
         */
        public int startupTimeoutSeconds = 180;
        /**
         * Turns between ambient re-plans. Cadence is counted in turns, not milliseconds:
         * the world only advances when the player acts, so a wall clock throttles a
         * standing-still player and outruns one holding shift.
         */
        public int cadenceTurns = 8;
        /** Pending requests the queue holds before it starts dropping the least salient. */
        public int queueCapacity = 12;
    }

    /** Hearing sensor ranges, in tiles (§8), and what an NPC is allowed to say. */
    public static class Speech {
        /** Spoken this close and the NPC treats it as addressed to them (DIRECTED). */
        public int directedRadius = 4;
        /** Heard out to here, but only overheard (NOTABLE). */
        public int earshotRadius = 10;
        /**
         * Most {@code say} commands one plan may contain. Asked for a plan, a small model
         * answers with several alternative <i>drafts</i> of the same line; the interpreter
         * then delivers them one per turn, so an NPC greets you four times in four turns,
         * contradicting itself. One line per plan — it re-plans when there is more to say.
         */
        public int maxSaysPerPlan = 1;
        /** Longest line an NPC may speak; longer is cut back to a sentence boundary. */
        public int maxSayChars = 110;
    }

    /** How signal priority resolves against a running plan (§9). */
    public static class Priority {
        /**
         * Stimuli at or above this salience preempt the current plan and jump the queue.
         * Deliberately *below* {@code Salience.DIRECTED} (70): salience is compared after
         * decay, so a threshold set exactly at DIRECTED gave a directed stimulus a window
         * of zero turns — one turn of ageing put it at 68 and it never interrupted at all.
         * At decay 2/turn this leaves being spoken to ~5 turns to preempt a plan, and being
         * attacked (URGENT 95) ~17, after which they are memory rather than emergency.
         */
        public int interruptAt = 60;
        /** Salience lost per turn of age, so nothing stays urgent forever. */
        public int decayPerTurn = 2;
        /** Turns an NPC stays focused on whoever addressed it. */
        public int attentionTurns = 12;
        /** Ambient re-plan cadence while holding attention — conversation ticks faster. */
        public int attentionCadenceTurns = 2;
        /**
         * Ambient re-plan cadence when <i>nothing has been sensed</i> since the last one.
         * The re-plan used to fire on idle alone, and the prompt ends with "never reply with
         * an empty array" — so an NPC standing near the player was asked every other turn to
         * produce something out of nothing, and what it produced was almost always talking.
         */
        public int idleCadenceTurns = 30;
        /**
         * Turns a parsed plan may sit unstarted before it is thrown away. The reflex owns
         * the body while fleeing, so a line composed mid-attack was delivered seven turns
         * later to a street the attacker had left — still answering the old scene.
         */
        public int planTtlTurns = 4;
        /**
         * Grace turns the flee reflex runs on <i>after the threat stops pressing</i>. This
         * used to be a flat budget from the last blow, which made panic a stopwatch: the
         * victim ran for ten turns and then strolled off with her attacker still standing
         * one tile away. The clock only runs while you are already clear.
         */
        public int fleeTurns = 10;
        /** Distance at which a fleeing NPC considers itself clear and stops running. */
        public int fleeDistance = 12;
        /**
         * Hard ceiling on one panic, however hard the attacker presses. Without it a victim
         * cornered in a room flees for the rest of the session and never plans anything
         * else; the next blow re-engages the reflex anyway.
         */
        public int fleeMaxTurns = 60;
        /** Grace turns a policeman keeps chasing after losing sight of the suspect. */
        public int pursueTurns = 15;
        /** Distance past which a policeman gives up the chase. */
        public int pursueDistance = 20;
        /** Hard ceiling on one chase, so a cop that cannot close eventually returns to duty. */
        public int pursueMaxTurns = 120;
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
        /**
         * Dialogue lines kept in the order they happened, heard and spoken. Conversation is
         * a sequence; ranked stimulus memory is not, which is why speech needs its own log.
         */
        public int dialogueLines = 6;
    }

    public boolean enabled = false;
    public boolean debug = false;
    /**
     * Mirror every submitted prompt into the trace, in full. Separate from {@code debug}
     * because it is a different order of volume — one multi-line block per submit against
     * one line per event — and you want it on only while you are reading prompts.
     */
    public boolean debugPrompts = false;
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
        LlmConfig config = LlmConfigLoader.read();

        // Escape hatch for tooling that wants the plain game: -Dllm.enabled=false skips
        // model staging and the servers entirely (see scripts/shot.sh).
        String override = System.getProperty(OVERRIDE);
        if (override != null) {
            config.enabled = Boolean.parseBoolean(override);
        }
        String prompts = System.getProperty(PROMPT_OVERRIDE);
        if (prompts != null) {
            config.debugPrompts = Boolean.parseBoolean(prompts);
        }
        return config;
    }

}
