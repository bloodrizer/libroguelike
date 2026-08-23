package com.nuclearunicorn.serialkiller.game.ai.mind;

import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.game.player.Player;
import com.nuclearunicorn.libroguelike.utils.Fov;
import com.nuclearunicorn.serialkiller.game.ai.llm.InferenceService;
import com.nuclearunicorn.serialkiller.game.ai.llm.LlamaHttpInferenceService;
import com.nuclearunicorn.serialkiller.game.ai.llm.LlmDebug;
import com.nuclearunicorn.serialkiller.game.ai.llm.LlmRuntime;
import com.nuclearunicorn.serialkiller.game.ai.llm.Perception;
import com.nuclearunicorn.serialkiller.game.ai.llm.command.AgentContext;
import com.nuclearunicorn.serialkiller.game.ai.llm.command.NpcCommand;
import com.nuclearunicorn.serialkiller.game.ai.llm.command.PlanInterpreter;
import com.nuclearunicorn.serialkiller.game.ai.llm.sense.GameTurn;
import com.nuclearunicorn.serialkiller.game.ai.llm.sense.Relations;
import com.nuclearunicorn.serialkiller.game.ai.llm.sense.Salience;
import com.nuclearunicorn.serialkiller.game.ai.llm.sense.Stimulus;
import com.nuclearunicorn.serialkiller.game.controllers.RLController;
import com.nuclearunicorn.serialkiller.game.world.entities.EntityRLHuman;

import java.util.Collections;
import java.util.List;

/**
 * The slow half of the brain: build a prompt, submit it, and run whatever plan comes back.
 *
 * <p>A component rather than a brain of its own. The layered arrangement it sits in is
 * Brooks' subsumption architecture — competences stacked so that a higher, faster layer
 * suppresses everything under it, each layer complete without the ones above — and the
 * modern hybrid-agent literature reaches the same shape for the same reason: a reactive
 * loop for anything time-critical, a deliberative one for goals, with an explicit escalation
 * rule between them. Here the escalation rule is the impulse list, and this is the bottom
 * layer: it only ever gets the body when no reflex wants it.
 *
 * <p>Which is precisely why it must not <i>be</i> the body. Making the LLM a whole AI class
 * meant that turning inference on replaced a policeman's reflexes with a civilian's, and an
 * officer who has to wait for a model round trip before reacting to a murder in front of him
 * is not policing. Reflexes belong to the brain; this belongs to the brain too, one rung down.
 *
 * <p>Inference stays off-thread: submit now, poll a later turn. The game thread never blocks,
 * and worker threads see an immutable String and nothing else.
 */
public class Deliberation {

    private final EntityRLHuman owner;
    private final RLController controller;
    private final Knowledge knowledge;
    private final Voice voice;
    private final PlanInterpreter interpreter = new PlanInterpreter();
    private final AgentContext context;

    /** Turn of the last submit; negative means never. Not a MIN_VALUE sentinel — subtracting
     *  that overflows, and a negative "elapsed" silently disables the cadence forever. */
    private long lastRequestTurn = -1;

    /** Turn of the last <i>director</i> (reflection) submit; negative means never. */
    private long lastDirectorTurn = -1;

    // Conversation focus (§8): who last addressed us, and until when. Not a dialogue
    // manager - just a bias on cadence and prompt framing, so exchanges hold together.
    private String attentionUid;
    private long attentionUntilTurn;

    // The last round trip, kept verbatim for the ALT overlay. This is the closest thing an
    // NPC has to a thought you can read: the exact question it was asked and the exact
    // answer that came back. Held on the game thread only, one round trip deep.
    private String lastPrompt;
    private String lastCompletion;
    private String lastReason;
    private long lastCompletionTurn = -1;

    public Deliberation(EntityRLHuman owner, RLController controller, Knowledge knowledge, Voice voice) {
        this.owner = owner;
        this.controller = controller;
        this.knowledge = knowledge;
        this.voice = voice;
        this.context = new AgentContext(owner, controller, voice);
    }

    /**
     * Collect any finished plan and decide whether to ask for a new one. Called every turn
     * from the brain's {@code update()}, whatever behaviour currently owns the body — an
     * officer mid-chase still gets to compose the next thing he shouts.
     *
     * <p>{@code asleep} is the one exception, and it is not an optimisation. The impulse list
     * already keeps a sleeper from <i>running</i> a plan, but a planner left pumping goes on
     * asking the model for lines all night and hands over a stack of them at dawn — a reply
     * to a conversation that ended eight hours ago. Asleep, the queue is drained and dropped.
     */
    public void pump(Perception.Situation situation, boolean asleep) {
        // The reactor is the conversational tier, gated to the near bucket; the director
        // (reflection) runs for every NPC and is what a far NPC still gets. Both are
        // optional - pump() degrades cleanly to whichever tier actually booted.
        InferenceService service = LlmRuntime.reactor();
        if (service != null) {
            pumpReactor(service, situation, asleep);
        }
        pumpDirector(asleep);
    }

    private void pumpReactor(InferenceService service, Perception.Situation situation, boolean asleep) {
        String uid = owner.get_uid();

        String completion = service.poll(uid);
        if (asleep) {
            if (completion != null) {
                LlmDebug.log("%s: completion dropped - asleep", uid);
            }
            // Nothing pending and no cadence held over, so waking re-plans on the first turn
            // the NPC is actually awake to have a thought.
            lastRequestTurn = -1;
            return;
        }
        boolean planJustLanded = false;
        if (completion != null) {
            lastCompletion = completion;
            lastCompletionTurn = GameTurn.current();
            LlmDebug.log("%s: completion received: %s", uid, completion.replace('\n', ' '));
            List<NpcCommand> plan = LlmRuntime.registry().parse(completion);
            if (!plan.isEmpty()) {
                LlmDebug.log("%s: plan parsed (%d cmds): %s", uid, plan.size(), verbList(plan));
                interpreter.setReactive(plan, GameTurn.current());
                planJustLanded = true;
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

        int top = knowledge.stream().topSalience();
        if (top >= Tuning.priority().interruptAt) {
            // Not in the same call that installed it. A completion is installed at the top of
            // pump() and the preempt below fires at the bottom of it, so a reply that has not
            // had one tick to run was destroyed before tick() ever saw it — and in a
            // conversation there is always a hot stimulus, so this was every reply. Traced in
            // a replay: the player asked his wife "who am I?", her model answered "Hello,
            // Giovanni.", and the line was binned unspoken. Twice, so she never said a word
            // to him — each thing he said killed the answer to the one before.
            //
            // The stimulus is not consumed here, so it is still hot next turn and preempts
            // then. The cost is one turn; what it buys is finishing your sentence.
            if (planJustLanded) {
                LlmDebug.log("%s: interrupt held one turn - a reply just landed", uid);
                return;
            }
            submit(service, situation, top, "interrupt");
            return;
        }

        long elapsed = lastRequestTurn < 0 ? Long.MAX_VALUE : GameTurn.current() - lastRequestTurn;
        if (interpreter.isIdle() && elapsed >= cadenceTurns()) {
            submit(service, situation, Math.max(top, Salience.AMBIENT),
                    sensedSinceRequest() ? "ambient re-plan" : "idle re-plan");
        }
    }

    /**
     * The long-term tier (§ reflection): ask the director model, at a very low cadence, to
     * consolidate recent observations into a durable belief. Free text, not a command array —
     * the result is stored on {@link Knowledge} and surfaced in every later reactor prompt.
     *
     * <p>Runs for every NPC, near or far, which is the whole point of a director: a distant
     * NPC that never gets the reactor still ends the day with an opinion about what happened.
     */
    private void pumpDirector(boolean asleep) {
        InferenceService service = LlmRuntime.director();
        if (service == null) {
            return;
        }
        String uid = owner.get_uid();

        String completion = service.poll(uid);
        if (completion != null && !asleep) {
            String belief = completion.trim().replace('\n', ' ');
            if (!belief.isEmpty()) {
                knowledge.addReflection(belief);
                LlmDebug.log("%s: reflected \"%s\"", uid, belief);
            }
        }
        if (asleep) {
            return;   // no reflecting in your sleep either; the clock just keeps counting
        }

        long elapsed = lastDirectorTurn < 0 ? Long.MAX_VALUE : GameTurn.current() - lastDirectorTurn;
        if (elapsed >= Tuning.director().cadenceTurns && !service.isBusy(uid)) {
            LlmDebug.log("%s: requesting reflection (turn %d)", uid, GameTurn.current());
            service.submit(uid, Perception.reflectPrompt(owner, knowledge), Salience.AMBIENT);
            lastDirectorTurn = GameTurn.current();
        }
    }

    /**
     * Run the plan. Called by the deliberate behaviour, so it happens only when no reflex
     * has taken the body — an in-flight "goto home" must not walk a victim calmly past the
     * person stabbing her.
     */
    public void tick() {
        // Reflex may have held the body for a while; whatever was queued before that is a
        // reply to a scene that has moved on. Re-plan instead of delivering it late.
        if (interpreter.dropStaleReactive(GameTurn.current(), Tuning.priority().planTtlTurns)) {
            lastRequestTurn = -1;
        }
        interpreter.tick(context);
    }

    /** Running for your life does not make you mute: let a queued plan contribute its line. */
    public void flushSpeech() {
        interpreter.flushSpeech(context);
    }

    /**
     * Drop the running plan and any conversation focus. What a reflex does on the way in:
     * being attacked ends a conversation, it does not start one, and wherever the plan was
     * walking to is moot.
     */
    public void interrupt() {
        interpreter.setReactive(Collections.<NpcCommand>emptyList());
        attentionUid = null;
    }

    public boolean isIdle() {
        return interpreter.isIdle();
    }

    /**
     * Build a snapshot and queue it. An interrupt also drops the running plan — an NPC that
     * kept walking away mid-sentence while its reply was in flight read as ignoring you.
     */
    private void submit(InferenceService service, Perception.Situation situation,
                        int priority, String why) {
        String uid = owner.get_uid();
        if (service.isBusy(uid) && priority <= pendingPriority(service, uid)) {
            return;   // already queued at this priority or better
        }
        Stimulus top = knowledge.stream().peekTop();
        if (priority >= Tuning.priority().interruptAt) {
            interpreter.setReactive(Collections.<NpcCommand>emptyList());
            focusOn(top);
        }
        situation.attending = hasAttention() ? displayName(attentionUid) : null;

        // Log the stimulus's own band, not the decayed number's: a DIRECTED line one turn
        // old scores 68 and would read as "NOTABLE", which is exactly the wrong thing to
        // see in a log you are using to work out whether a directed signal got through.
        LlmDebug.log("%s: %s -> submitting at %s (score %d, turn %d)",
                uid, why, Salience.label(top == null ? priority : top.salience),
                priority, GameTurn.current());
        String prompt = Perception.snapshot(owner, knowledge, voice.log(), situation);
        lastPrompt = prompt;
        lastReason = why;
        LlmDebug.prompt(uid, prompt);
        service.submit(uid, prompt, priority);

        // Consume at submit time, not at reply time: a round trip takes seconds, and a
        // stimulus left unconsumed re-fires the interrupt every turn until then.
        knowledge.stream().markConsumed();
        lastRequestTurn = GameTurn.current();
    }

    private int pendingPriority(InferenceService service, String uid) {
        if (service instanceof LlamaHttpInferenceService) {
            return ((LlamaHttpInferenceService) service).pendingPriority(uid);
        }
        return 0;
    }

    /**
     * Hold conversation focus on whoever just <i>spoke</i> to us (§8). Speech only: focusing
     * on any high-salience source made a stabbing register as the opening of a conversation,
     * and the prompt then told the victim to stay put and keep talking to her attacker.
     */
    private void focusOn(Stimulus stimulus) {
        if (stimulus == null || stimulus.sourceUid == null
                || stimulus.channel != Stimulus.Channel.SPEECH) {
            return;
        }
        attentionUid = stimulus.sourceUid;
        attentionUntilTurn = GameTurn.current() + Tuning.priority().attentionTurns;
    }

    public boolean hasAttention() {
        return attentionUid != null && GameTurn.current() < attentionUntilTurn;
    }

    public String attentionUid() {
        return attentionUid;
    }

    /**
     * Has anything actually happened since we last asked the model? Idle alone used to be
     * enough, so an NPC next to the player re-planned every other turn with nothing to react
     * to — and since the prompt forbids an empty reply, what came back was chatter.
     */
    private boolean sensedSinceRequest() {
        return lastRequestTurn < 0 || knowledge.stream().lastAddedTurn() > lastRequestTurn;
    }

    /** Turns between ambient re-plans — tighter in conversation, much slower with no input. */
    private int cadenceTurns() {
        if (!sensedSinceRequest()) {
            return Tuning.priority().idleCadenceTurns;
        }
        return hasAttention()
                ? Tuning.priority().attentionCadenceTurns
                : LlmRuntime.peekConfig().reactor.cadenceTurns;
    }

    /** Near bucket: only NPCs within throttle.nearRadius of the player run inference. */
    public boolean isNearPlayer() {
        if (Player.get_ent() == null) {
            return false;
        }
        return Fov.in_range(owner.origin, Player.get_origin(),
                LlmRuntime.peekConfig().throttle.nearRadius);
    }

    public String displayName(String uid) {
        if (uid == null || owner.getEnvironment() == null) {
            return null;
        }
        Entity ent = owner.getEnvironment().getEntityManager().get_entity(uid);
        return ent == null ? null : Relations.describe(owner, ent);
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

    // --------------------------------------------------------------- debug view

    /** The exact prompt last submitted, or null if this NPC has never asked the model. */
    public String lastPrompt() {
        return lastPrompt;
    }

    /** The model's last reply, verbatim and unparsed. */
    public String lastCompletion() {
        return lastCompletion;
    }

    /** Why the last prompt was sent: "interrupt", "ambient re-plan", "idle re-plan". */
    public String lastReason() {
        return lastReason;
    }

    public long lastCompletionTurn() {
        return lastCompletionTurn;
    }

    /** Turns since the last submit, or -1 if there has never been one. */
    public long turnsSinceRequest() {
        return lastRequestTurn < 0 ? -1 : GameTurn.current() - lastRequestTurn;
    }

    /** Turns that have to pass before the next ambient re-plan. */
    public int debugCadence() {
        return cadenceTurns();
    }

    /** A request is in flight for this NPC — the model, not the body, is the holdup. */
    public boolean isBusy() {
        InferenceService service = LlmRuntime.reactor();
        return service != null && service.isBusy(owner.get_uid());
    }

    /** The plan as it stands, one line per step, head marked. */
    public List<String> plan() {
        return interpreter.debugPlan();
    }

    /** Turn the running plan was composed on, or -1 when there is no plan. */
    public long planTurn() {
        return interpreter.debugReactiveTurn();
    }

    public boolean planStarted() {
        return interpreter.debugReactiveStarted();
    }

    /** Queue state for the replay log — whether the model, not the body, is the holdup. */
    public String debugState() {
        InferenceService service = LlmRuntime.reactor();
        return " idle=" + interpreter.isIdle()
                + " busy=" + (service != null && service.isBusy(owner.get_uid()))
                + " sinceRequest=" + (lastRequestTurn < 0
                        ? "never" : String.valueOf(GameTurn.current() - lastRequestTurn))
                + " dialogue=" + voice.log().size()
                + " attention=" + (hasAttention() ? attentionUid : "none")
                + " near=" + isNearPlayer();
    }
}
