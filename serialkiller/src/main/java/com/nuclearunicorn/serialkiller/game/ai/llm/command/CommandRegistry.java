package com.nuclearunicorn.serialkiller.game.ai.llm.command;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.nuclearunicorn.serialkiller.game.ai.llm.command.commands.GotoCommand;
import com.nuclearunicorn.serialkiller.game.ai.llm.command.commands.SayCommand;
import com.nuclearunicorn.serialkiller.game.ai.llm.command.commands.WaitCommand;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * verb → CommandFactory (§4). Assembles the decoding grammar from the registered
 * fragments and parses a JSON command array into commands. Adding a verb here is the
 * only change needed to grow the vocabulary — interpreter, AI, and scheduler are
 * untouched.
 */
public class CommandRegistry {

    private final Map<String, CommandFactory> factories = new LinkedHashMap<>();
    private final Gson gson = new Gson();
    private final String grammar;

    public CommandRegistry() {
        register(new GotoCommand.Factory());
        register(new SayCommand.Factory());
        register(new WaitCommand.Factory());
        this.grammar = assembleGrammar();
    }

    private void register(CommandFactory factory) {
        factories.put(factory.verb(), factory);
    }

    /**
     * Longest plan the grammar will accept. An unbounded {@code *} let a small model fall
     * into a repetition loop — the same two commands emitted until maxTokens cut it off
     * mid-token, which then failed to parse and was dropped as an empty plan. That burned
     * the slowest inference in the queue to produce nothing. A hard ceiling ends the array
     * while it is still valid JSON, and NPCs re-plan every cadence anyway.
     */
    private static final int MAX_PLAN = 4;

    /**
     * GBNF that accepts a JSON array of command objects (§5.2). The model emits an
     * ordered program constrained to registered verbs with valid argument shapes.
     */
    private String assembleGrammar() {
        StringBuilder command = new StringBuilder("command ::= ");
        boolean first = true;
        for (CommandFactory factory : factories.values()) {
            if (!first) {
                command.append(" | ");
            }
            command.append("( ").append(factory.grammarFragment()).append(" )");
            first = false;
        }

        // Spelled out as optional tails rather than {0,n}: repetition-count syntax is a
        // newer GBNF addition, and this stays portable across llama.cpp builds.
        StringBuilder tail = new StringBuilder();
        for (int i = 1; i < MAX_PLAN; i++) {
            tail.append(" ( ws \",\" ws command )?");
        }

        // The first command is mandatory. "Never reply with an empty array" was only ever an
        // instruction, and a small model handed a hard prompt still answered "[]" — silence
        // exactly when there was most to react to. The decoder can simply forbid it.
        return "root ::= \"[\" ws command" + tail + " ws \"]\"\n" +
                command + "\n" +
                "string ::= \"\\\"\" ( [^\"\\\\] )* \"\\\"\"\n" +
                "number ::= [0-9]+\n" +
                "ws ::= [ \\t\\n]*\n";
    }

    public String getGrammar() {
        return grammar;
    }

    /**
     * Parse a model completion (JSON command array) into commands; unknown/malformed entries
     * are skipped, and speech is capped at {@code speech.maxSaysPerPlan}.
     *
     * <p>The cap is enforced here rather than in the grammar because it is a runtime config
     * value and the grammar is assembled once. It exists because a plan of four consecutive
     * {@code say}s is not a plan — it is the model offering four drafts of one reply, which
     * the interpreter then performs as a four-turn monologue.
     */
    public List<NpcCommand> parse(String json) {
        List<NpcCommand> commands = new ArrayList<>();
        if (json == null) {
            return commands;
        }
        int sayLimit = com.nuclearunicorn.serialkiller.game.ai.llm.LlmRuntime.config().speech.maxSaysPerPlan;
        int says = 0;
        int dropped = 0;
        try {
            JsonArray array = gson.fromJson(json, JsonArray.class);
            if (array == null) {
                return commands;
            }
            for (JsonElement element : array) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject obj = element.getAsJsonObject();
                if (!obj.has("verb")) {
                    continue;
                }
                String verb = obj.get("verb").getAsString();
                if ("say".equals(verb) && ++says > sayLimit) {
                    dropped++;
                    continue;
                }
                CommandFactory factory = factories.get(verb);
                if (factory != null) {
                    commands.add(factory.parse(obj));
                }
            }
        } catch (RuntimeException e) {
            // Residual parse failure (§12) — drop; the AI re-submits next cadence.
            System.err.println("command parse failed: " + e);
        }
        if (dropped > 0) {
            com.nuclearunicorn.serialkiller.game.ai.llm.LlmDebug.log(
                    "  dropped %d extra say(s), limit is %d per plan", dropped, sayLimit);
        }
        return commands;
    }
}
