package com.nuclearunicorn.serialkiller.game.ai.llm.command;

import com.nuclearunicorn.serialkiller.game.ai.llm.LlmDebug;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Per-NPC command runner (§5.3). Two queues: an agenda (base plan from the director) and
 * a reactive queue (short-term reactor) that preempts it. Each tick, step the reactive
 * head if any, else the agenda head; advance on SUCCESS/FAILURE. Reflex (escape/chase)
 * preempts both above this level, in the AI.
 */
public class PlanInterpreter {

    private final Deque<NpcCommand> agenda = new ArrayDeque<>();
    private final Deque<NpcCommand> reactive = new ArrayDeque<>();

    private NpcCommand active;   // currently-stepping command (needs onEnter/onExit bookkeeping)

    public void setAgenda(List<NpcCommand> commands) {
        clearActiveFrom(agenda);
        agenda.clear();
        agenda.addAll(commands);
    }

    public void setReactive(List<NpcCommand> commands) {
        clearActiveFrom(reactive);
        reactive.clear();
        reactive.addAll(commands);
    }

    public boolean isIdle() {
        return agenda.isEmpty() && reactive.isEmpty();
    }

    public void tick(AgentContext ctx) {
        Deque<NpcCommand> queue = !reactive.isEmpty() ? reactive : agenda;
        if (queue.isEmpty()) {
            active = null;
            return;
        }

        NpcCommand head = queue.peek();
        if (head != active) {
            active = head;
            LlmDebug.log("  exec %s", head.verb());
            head.onEnter(ctx);
        }

        NpcCommand.Status status = head.step(ctx);
        if (status != NpcCommand.Status.RUNNING) {
            LlmDebug.log("  %s -> %s", head.verb(), status);
            head.onExit(ctx);
            queue.poll();
            active = null;
        }
    }

    /** If the command being replaced is currently active, let it clean up first. */
    private void clearActiveFrom(Deque<NpcCommand> queue) {
        if (active != null && !queue.isEmpty() && queue.peek() == active) {
            active = null;
        }
    }
}
