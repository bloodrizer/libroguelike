package com.nuclearunicorn.serialkiller.game.ai.llm.command;

/**
 * One executable step in an NPC's plan (LLM_NPC_SPEC.md §5.1). Durative commands
 * (goto) return RUNNING across many ticks; instant ones (say, wait 0) return SUCCESS
 * at once. The interpreter steps the head command each tick and advances on
 * SUCCESS/FAILURE.
 */
public interface NpcCommand {

    enum Status { RUNNING, SUCCESS, FAILURE }

    String verb();

    default void onEnter(AgentContext ctx) {}

    /** Called each tick while this command is active. */
    Status step(AgentContext ctx);

    default void onExit(AgentContext ctx) {}
}
