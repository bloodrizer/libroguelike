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
import com.nuclearunicorn.serialkiller.game.ai.llm.sense.GameTurn;
import com.nuclearunicorn.serialkiller.game.ai.llm.sense.Salience;
import com.nuclearunicorn.serialkiller.game.ai.llm.sense.Stimulus;
import com.nuclearunicorn.serialkiller.game.ai.llm.sense.StimulusMemory;
import com.nuclearunicorn.serialkiller.game.controllers.RLController;
import com.nuclearunicorn.serialkiller.game.world.entities.EntityRLHuman;
import com.nuclearunicorn.libroguelike.game.player.Player;
import com.nuclearunicorn.libroguelike.utils.Fov;

import java.util.List;

/**
 * LLM-driven NPC brain (LLM_NPC_SPEC.md §3). Wires the async loop into the existing tick
 * contract, and resolves competing signals by {@link Salience}:
 *
 * <ol>
 *   <li><b>Interrupt</b> — a stimulus at or above {@code priority.interruptAt} (being
 *       spoken to, being attacked) drops the running plan and submits immediately at that
 *       priority, ignoring both the idle check and the cadence.</li>
 *   <li><b>Ambient</b> — otherwise re-plan only when idle and the cadence has elapsed.</li>
 * </ol>
 *
 * <p>The ambient rung is the behaviour this class used to have <i>unconditionally</i>, which
 * is why NPCs ignored the player: a durative {@code goto} kept the interpreter non-idle, so
 * a message could sit in memory indefinitely without ever reaching a prompt.
 *
 * <p>Inference stays off-thread: submit now, poll a later turn. The game thread never blocks.
 */
public class LLMAgentAI extends BasicMobAI {

    // Serializable memory (§7). Everything else is rebuilt from the runtime.
    private StimulusMemory memory;
    /** Turn of the last submit; negative means never. Not a MIN_VALUE sentinel — subtracting
     *  that overflows, and a negative "elapsed" silently disables the cadence forever. */
    private long lastRequestTurn = -1;

    // Conversation focus (§8): who last addressed us, and until when. Not a dialogue
    // manager - just a bias on cadence and prompt framing, so exchanges hold together.
    private String attentionUid;
    private long attentionUntilTurn;

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
        if (memory == null) {
            memory = new StimulusMemory(LlmRuntime.config().memory.observations,
                    LlmRuntime.config().priority.decayPerTurn);
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
                // Empty array is a real failure mode on small models, not a no-op. Re-plan
                // on the next cadence rather than letting the NPC look unresponsive.
                LlmDebug.log("%s: completion parsed to EMPTY plan (dropped)", uid);
                lastRequestTurn = -1;
            }
        }

        if (!isNearPlayer()) {
            return;   // far bucket: no reactor inference (§9)
        }

        int top = memory.topSalience();
        int interruptAt = LlmRuntime.config().priority.interruptAt;

        if (top >= interruptAt) {
            submit(service, top, "interrupt (" + Salience.label(top) + ")");
            return;
        }

        long elapsed = lastRequestTurn < 0 ? Long.MAX_VALUE : GameTurn.current() - lastRequestTurn;
        if (interpreter.isIdle() && elapsed >= cadenceTurns()) {
            submit(service, Math.max(top, Salience.AMBIENT), "ambient re-plan");
        }
    }

    /**
     * Build a snapshot and queue it. An interrupt also drops the running plan — an NPC that
     * kept walking away mid-sentence while its reply was in flight read as ignoring you.
     */
    private void submit(InferenceService service, int priority, String why) {
        String uid = owner.get_uid();
        if (service.isBusy(uid) && priority <= pendingPriority(service, uid)) {
            return;   // already queued at this priority or better
        }
        if (priority >= LlmRuntime.config().priority.interruptAt) {
            interpreter.setReactive(java.util.Collections.<NpcCommand>emptyList());
            focusOn(memory.peekTop());
        }

        LlmDebug.log("%s: %s -> submitting at %s (turn %d)",
                uid, why, Salience.label(priority), GameTurn.current());
        service.submit(uid, Perception.snapshot((EntityRLHuman) owner, memory, attentionName()), priority);

        // Consume at submit time, not at reply time: a round trip takes seconds, and a
        // stimulus left unconsumed re-fires the interrupt every turn until then.
        memory.markConsumed();
        lastRequestTurn = GameTurn.current();
    }

    private int pendingPriority(InferenceService service, String uid) {
        if (service instanceof com.nuclearunicorn.serialkiller.game.ai.llm.LlamaHttpInferenceService) {
            return ((com.nuclearunicorn.serialkiller.game.ai.llm.LlamaHttpInferenceService) service)
                    .pendingPriority(uid);
        }
        return 0;
    }

    /** Hold conversation focus on whoever just addressed us (§8). */
    private void focusOn(Stimulus stimulus) {
        if (stimulus == null || stimulus.sourceUid == null) {
            return;
        }
        attentionUid = stimulus.sourceUid;
        attentionUntilTurn = GameTurn.current() + LlmRuntime.config().priority.attentionTurns;
    }

    private boolean hasAttention() {
        return attentionUid != null && GameTurn.current() < attentionUntilTurn;
    }

    /** Display name of who we are attending to, or null when not in a conversation. */
    private String attentionName() {
        if (!hasAttention()) {
            return null;
        }
        com.nuclearunicorn.libroguelike.game.ent.Entity ent =
                owner.getEnvironment().getEntityManager().get_entity(attentionUid);
        if (ent == null) {
            return null;
        }
        return ent.isPlayerEnt() ? "the player" : ent.getName();
    }

    /** Turns between ambient re-plans — tighter while holding a conversation. */
    private int cadenceTurns() {
        return hasAttention()
                ? LlmRuntime.config().priority.attentionCadenceTurns
                : LlmRuntime.config().reactor.cadenceTurns;
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
        // World events are ambient by default; the ones that mean something get promoted
        // by their own sensors. Bare class names used to be written straight into memory,
        // where they crowded out real signal and pushed the model toward empty plans.
        sense(new Stimulus(GameTurn.current(), Stimulus.Channel.EVENT, Salience.AMBIENT,
                null, describe(event)));
    }

    /** Turn an event class into something a model can actually reason about. */
    private static String describe(Event event) {
        String name = event.getClass().getSimpleName();
        if (name.startsWith("E")) {
            name = name.substring(1);
        }
        return "something happened nearby (" + name + ")";
    }

    /**
     * Sensor entry point (§7). Sensors call this; salience decides everything downstream —
     * whether it survives memory eviction, where it sits in the inference queue, and whether
     * it preempts the running plan.
     */
    public void sense(Stimulus stimulus) {
        if (!LlmRuntime.isEnabled() || !ensureWired()) {
            return;
        }
        memory.add(stimulus);
        LlmDebug.log("%s sensed %s", owner.get_uid(), stimulus);
    }

    /**
     * Brain state for the replay log. This is the view that tells you <i>why</i> an NPC
     * ignored you: whether the stimulus arrived at all, what salience it carries now, and
     * whether the trigger or the queue is the thing holding the reaction back.
     */
    public String debugState() {
        if (memory == null || interpreter == null) {
            return "unwired";
        }
        Stimulus top = memory.peekTop();
        return "top=" + (top == null ? "none" : Salience.label(top.salience) + ":" + top.text())
                + " topSalience=" + memory.topSalience()
                + " idle=" + interpreter.isIdle()
                + " busy=" + (LlmRuntime.reactor() != null && LlmRuntime.reactor().isBusy(owner.get_uid()))
                + " sinceRequest=" + (lastRequestTurn < 0
                        ? "never" : String.valueOf(GameTurn.current() - lastRequestTurn))
                + " attention=" + (hasAttention() ? attentionUid : "none")
                + " near=" + isNearPlayer();
    }
}
