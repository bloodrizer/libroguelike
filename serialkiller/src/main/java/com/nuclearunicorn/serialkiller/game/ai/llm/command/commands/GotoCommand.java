package com.nuclearunicorn.serialkiller.game.ai.llm.command.commands;

import com.google.gson.JsonObject;
import com.nuclearunicorn.serialkiller.game.ai.llm.command.AgentContext;
import com.nuclearunicorn.serialkiller.game.ai.llm.command.CommandFactory;
import com.nuclearunicorn.serialkiller.game.ai.llm.command.NpcCommand;
import org.lwjgl.util.Point;

/**
 * Move to a symbolic target (§6). A* / milestone routing owns the route; this command
 * only kicks off pathing and follows it. Durative — RUNNING until arrival.
 */
public class GotoCommand implements NpcCommand {

    private final String targetSymbol;
    private Point target;

    public GotoCommand(String targetSymbol) {
        this.targetSymbol = targetSymbol;
    }

    @Override
    public String verb() {
        return "goto";
    }

    @Override
    public void onEnter(AgentContext ctx) {
        target = ctx.resolve(targetSymbol);
        if (target != null) {
            ctx.controller().calculateAdaptivePath(ctx.owner().origin, target);
        }
    }

    @Override
    public Status step(AgentContext ctx) {
        if (target == null) {
            return Status.FAILURE;
        }
        if (ctx.owner().origin.equals(target)) {
            return Status.SUCCESS;
        }
        // follow_path clears the path once the destination is reached; no path left
        // means either arrival at the nearest routable tile or an unroutable target.
        if (!ctx.controller().hasPath()) {
            return Status.SUCCESS;
        }
        ctx.controller().follow_path();
        return Status.RUNNING;
    }

    public static class Factory implements CommandFactory {
        @Override
        public String verb() {
            return "goto";
        }

        @Override
        public String grammarFragment() {
            return "\"{\" ws \"\\\"verb\\\"\" ws \":\" ws \"\\\"goto\\\"\" ws \",\" ws " +
                   "\"\\\"target\\\"\" ws \":\" ws string ws \"}\"";
        }

        @Override
        public NpcCommand parse(JsonObject args) {
            String target = args.has("target") ? args.get("target").getAsString() : null;
            return new GotoCommand(target);
        }
    }
}
