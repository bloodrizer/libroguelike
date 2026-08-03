package com.nuclearunicorn.serialkiller.game.ai;

import com.nuclearunicorn.libroguelike.events.Event;
import com.nuclearunicorn.libroguelike.game.ai.BasicMobAI;
import com.nuclearunicorn.serialkiller.game.ai.llm.InferenceService;
import com.nuclearunicorn.serialkiller.game.ai.llm.LlmDebug;
import com.nuclearunicorn.serialkiller.game.ai.llm.LlmRuntime;
import com.nuclearunicorn.serialkiller.game.ai.llm.Perception;
import com.nuclearunicorn.serialkiller.game.ai.llm.command.AgentContext;
import com.nuclearunicorn.serialkiller.game.ai.llm.command.CommandRegistry;
import com.nuclearunicorn.serialkiller.game.ai.llm.command.NpcCommand;
import com.nuclearunicorn.serialkiller.game.ai.llm.command.PlanInterpreter;
import com.nuclearunicorn.serialkiller.game.controllers.RLController;
import com.nuclearunicorn.serialkiller.game.world.entities.EntityRLHuman;
import com.nuclearunicorn.libroguelike.game.player.Player;
import com.nuclearunicorn.libroguelike.utils.Fov;
import com.nuclearunicorn.libroguelike.utils.Timer;

import java.util.ArrayList;
import java.util.List;

/**
 * LLM-driven NPC brain (LLM_NPC_SPEC.md §3, M1). Wires the async loop into the existing
 * tick contract:
 * <ul>
 *   <li>{@code update()} — on cadence, if the interpreter is idle and no request is in
 *       flight, build a perception snapshot and submit it. Poll any ready result and
 *       load it as the reactive plan.</li>
 *   <li>{@code think()} — step the interpreter once (executes the active command via the
 *       controller / speech).</li>
 * </ul>
 * Inference is off-thread: submit now, poll a later turn. The game thread never blocks.
 */
public class LLMAgentAI extends BasicMobAI {

    // Serializable memory (§7). Everything else is rebuilt from the runtime.
    private final List<String> observations = new ArrayList<>();
    private long lastRequestMs = 0;

    private transient PlanInterpreter interpreter;
    private transient AgentContext context;

    public LLMAgentAI() {
        super();
    }

    /** Rebuild transient wiring (also after deserialization). Context needs the live controller. */
    private boolean ensureWired() {
        if (!(owner instanceof EntityRLHuman) || !(owner.controller instanceof RLController)) {
            return false;
        }
        if (interpreter == null) {
            interpreter = new PlanInterpreter();
        }
        if (context == null) {
            context = new AgentContext((EntityRLHuman) owner, (RLController) owner.controller);
            LlmDebug.log("%s (%s): LLM agent wired and live", owner.get_uid(), owner.getName());
        }
        return true;
    }

    @Override
    public void update() {
        if (!LlmRuntime.isEnabled() || !ensureWired()) {
            return;
        }

        InferenceService service = LlmRuntime.reactor();
        CommandRegistry registry = LlmRuntime.registry();
        String uid = owner.get_uid();

        // Load a ready plan (submitted on an earlier turn) as the reactive queue.
        String completion = service.poll(uid);
        if (completion != null) {
            LlmDebug.log("%s: completion received: %s", uid, completion.replace('\n', ' '));
            List<NpcCommand> plan = registry.parse(completion);
            if (!plan.isEmpty()) {
                LlmDebug.log("%s: plan parsed (%d cmds): %s", uid, plan.size(), verbList(plan));
                interpreter.setReactive(plan);
            } else {
                LlmDebug.log("%s: completion parsed to EMPTY plan (dropped)", uid);
            }
        }

        // Request a fresh plan when idle and the cadence has elapsed. Only NPCs in the near
        // bucket submit — one CPU worker can't serve the whole town, so far NPCs stay dormant
        // (they still finish any plan they already hold, which is free). (§11)
        long now = Timer.get_time();
        int cadence = LlmRuntime.config().reactor.cadenceMs;
        if (interpreter.isIdle() && !service.isBusy(uid) && (now - lastRequestMs) >= cadence && isNearPlayer()) {
            LlmDebug.log("%s: idle + cadence elapsed (%dms) — submitting perception", uid, now - lastRequestMs);
            service.submit(uid, Perception.snapshot((EntityRLHuman) owner, observations));
            lastRequestMs = now;
        }
    }

    /** Near bucket: only NPCs within throttle.nearRadius of the player run inference. */
    private boolean isNearPlayer() {
        if (Player.get_ent() == null) {
            return false;
        }
        int radius = LlmRuntime.config().throttle.nearRadius;
        return Fov.in_range(owner.origin, Player.get_origin(), radius);
    }

    private static String verbList(List<NpcCommand> plan) {
        StringBuilder sb = new StringBuilder();
        for (NpcCommand c : plan) {
            if (sb.length() > 0) {
                sb.append(" -> ");
            }
            sb.append(c.verb());
        }
        return sb.toString();
    }

    @Override
    public void think() {
        if (!LlmRuntime.isEnabled() || !ensureWired()) {
            return;
        }
        interpreter.tick(context);
    }

    @Override
    public void e_on_event(Event event) {
        // Feed events into memory as short observations (§7). Kept lean for M1.
        addObservation(event.getClass().getSimpleName());
    }

    private void addObservation(String text) {
        int cap = LlmRuntime.config() != null ? LlmRuntime.config().memory.observations : 8;
        observations.add(text);
        while (observations.size() > cap) {
            observations.remove(0);
        }
    }
}
