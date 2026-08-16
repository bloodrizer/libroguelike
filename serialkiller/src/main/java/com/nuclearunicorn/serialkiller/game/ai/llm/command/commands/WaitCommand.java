package com.nuclearunicorn.serialkiller.game.ai.llm.command.commands;

import com.google.gson.JsonObject;
import com.nuclearunicorn.serialkiller.game.ai.llm.command.AgentContext;
import com.nuclearunicorn.serialkiller.game.ai.llm.command.CommandFactory;
import com.nuclearunicorn.serialkiller.game.ai.llm.command.NpcCommand;

/**
 * Idle for a number of ticks (§5.4). Durative — RUNNING until the counter drains.
 */
public class WaitCommand implements NpcCommand {

    private int remaining;

    public WaitCommand(int ticks) {
        this.remaining = ticks;
    }

    @Override
    public String verb() {
        return "wait";
    }

    @Override
    public Status step(AgentContext ctx) {
        if (remaining <= 0) {
            return Status.SUCCESS;
        }
        remaining--;
        return Status.RUNNING;
    }

    public static class Factory implements CommandFactory {
        @Override
        public String verb() {
            return "wait";
        }

        @Override
        public String grammarFragment() {
            return "\"{\" ws \"\\\"verb\\\"\" ws \":\" ws \"\\\"wait\\\"\" ws \",\" ws " +
                   "\"\\\"ticks\\\"\" ws \":\" ws number ws \"}\"";
        }

        @Override
        public NpcCommand parse(JsonObject args) {
            int ticks = args.has("ticks") ? args.get("ticks").getAsInt() : 0;
            return new WaitCommand(ticks);
        }
    }
}
