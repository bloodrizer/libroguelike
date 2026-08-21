package com.nuclearunicorn.serialkiller.game.ai.llm.command;

import com.nuclearunicorn.serialkiller.game.ai.llm.LlmDebug;

import java.util.ArrayDeque;
import java.util.ArrayList;
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

    // When the reactive plan was installed, and whether it has ever had the body. A plan
    // that never started is still a reply to the world as it was at that turn.
    private long reactiveTurn = -1;
    private boolean reactiveStarted;

    public void setAgenda(List<NpcCommand> commands) {
        clearActiveFrom(agenda);
        agenda.clear();
        agenda.addAll(commands);
    }

    public void setReactive(List<NpcCommand> commands) {
        setReactive(commands, -1);
    }

    public void setReactive(List<NpcCommand> commands, long turn) {
        clearActiveFrom(reactive);
        reactive.clear();
        reactive.addAll(commands);
        reactiveTurn = turn;
        reactiveStarted = false;
    }

    /**
     * While the reflex owns the body, let a plan contribute its line and nothing else.
     * Fleeing and shouting for help are not in conflict, but the interpreter is
     * all-or-nothing — so a victim whose model produced "Help! Someone just attacked me!"
     * ran away in silence, and the line was later dropped unspoken. Movement decided before
     * the attack is moot by the time the reflex releases, so the rest of the plan goes.
     */
    public boolean flushSpeech(AgentContext ctx) {
        active = null;
        while (!reactive.isEmpty() && !"say".equals(reactive.peek().verb())) {
            reactive.poll();
        }
        if (reactive.isEmpty()) {
            return false;
        }
        NpcCommand say = reactive.poll();
        say.onEnter(ctx);
        say.step(ctx);
        say.onExit(ctx);
        reactive.clear();
        return true;
    }

    /**
     * Drop a reactive plan that was composed {@code ttl} turns ago and never got to run.
     * The reflex owns the body while fleeing, so a reply written mid-stabbing sat in the
     * queue for seven turns and was then delivered to an empty street, still in the past
     * tense. A plan that has already started is left alone — a long walk is not stale.
     */
    public boolean dropStaleReactive(long now, int ttl) {
        if (reactive.isEmpty() || reactiveStarted || reactiveTurn < 0 || now - reactiveTurn <= ttl) {
            return false;
        }
        LlmDebug.log("  dropped stale plan (%d turns old, never ran)", now - reactiveTurn);
        setReactive(java.util.Collections.<NpcCommand>emptyList());
        return true;
    }

    public boolean isIdle() {
        return agenda.isEmpty() && reactive.isEmpty();
    }

    /**
     * Both queues as text, reactive first because that is the one that runs, with the step
     * currently holding the body marked. The debug overlay's answer to "the model replied,
     * so why is nothing happening" — a plan can be present and still never be stepped.
     */
    public List<String> debugPlan() {
        List<String> out = new ArrayList<String>(reactive.size() + agenda.size());
        for (NpcCommand cmd : reactive) {
            out.add((cmd == active ? "> " : "  ") + cmd.describe());
        }
        for (NpcCommand cmd : agenda) {
            out.add((cmd == active ? "> " : "  ") + cmd.describe() + "  [agenda]");
        }
        return out;
    }

    /** Turn the running reactive plan was composed on, or -1. Age is what makes it stale. */
    public long debugReactiveTurn() {
        return reactive.isEmpty() ? -1 : reactiveTurn;
    }

    /** Whether the reactive plan has ever had the body. A plan that never ran is a symptom. */
    public boolean debugReactiveStarted() {
        return reactiveStarted;
    }

    public void tick(AgentContext ctx) {
        Deque<NpcCommand> queue = !reactive.isEmpty() ? reactive : agenda;
        if (queue.isEmpty()) {
            active = null;
            return;
        }
        if (queue == reactive) {
            reactiveStarted = true;
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
