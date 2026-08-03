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

        return "root ::= \"[\" ws ( command ( ws \",\" ws command )* )? ws \"]\"\n" +
                command + "\n" +
                "string ::= \"\\\"\" ( [^\"\\\\] )* \"\\\"\"\n" +
                "number ::= [0-9]+\n" +
                "ws ::= [ \\t\\n]*\n";
    }

    public String getGrammar() {
        return grammar;
    }

    /** Parse a model completion (JSON command array) into commands; unknown/malformed entries are skipped. */
    public List<NpcCommand> parse(String json) {
        List<NpcCommand> commands = new ArrayList<>();
        if (json == null) {
            return commands;
        }
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
                CommandFactory factory = factories.get(obj.get("verb").getAsString());
                if (factory != null) {
                    commands.add(factory.parse(obj));
                }
            }
        } catch (RuntimeException e) {
            // Residual parse failure (§12) — drop; the AI re-submits next cadence.
            System.err.println("command parse failed: " + e);
        }
        return commands;
    }
}
