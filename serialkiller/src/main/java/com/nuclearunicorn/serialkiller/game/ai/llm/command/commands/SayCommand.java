package com.nuclearunicorn.serialkiller.game.ai.llm.command.commands;

import com.google.gson.JsonObject;
import com.nuclearunicorn.serialkiller.game.ai.llm.command.AgentContext;
import com.nuclearunicorn.serialkiller.game.ai.llm.command.CommandFactory;
import com.nuclearunicorn.serialkiller.game.ai.llm.command.NpcCommand;

/**
 * Speak a line into the local chat channel (§8). Instant — nearby NPCs perceive the
 * EChatMessage and may react on their next cadence; conversation emerges from timing.
 */
public class SayCommand implements NpcCommand {

    private final String text;

    public SayCommand(String text) {
        this.text = text;
    }

    @Override
    public String verb() {
        return "say";
    }

    @Override
    public Status step(AgentContext ctx) {
        if (text != null && !text.isEmpty()) {
            ctx.say(text);
        }
        return Status.SUCCESS;
    }

    public static class Factory implements CommandFactory {
        @Override
        public String verb() {
            return "say";
        }

        @Override
        public String grammarFragment() {
            return "\"{\" ws \"\\\"verb\\\"\" ws \":\" ws \"\\\"say\\\"\" ws \",\" ws " +
                   "\"\\\"text\\\"\" ws \":\" ws string ws \"}\"";
        }

        @Override
        public NpcCommand parse(JsonObject args) {
            String text = args.has("text") ? args.get("text").getAsString() : null;
            return new SayCommand(text);
        }
    }
}
