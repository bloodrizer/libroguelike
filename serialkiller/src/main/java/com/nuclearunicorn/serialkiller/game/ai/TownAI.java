package com.nuclearunicorn.serialkiller.game.ai;

import com.nuclearunicorn.libroguelike.events.Event;
import com.nuclearunicorn.libroguelike.game.ai.BasicMobAI;
import com.nuclearunicorn.libroguelike.game.ai.IAIAction;
import com.nuclearunicorn.libroguelike.game.ai.Impulse;
import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.game.ent.controller.NpcController;
import com.nuclearunicorn.libroguelike.game.world.WorldTile;
import com.nuclearunicorn.serialkiller.game.ai.behavior.SleepAction;
import com.nuclearunicorn.serialkiller.game.ai.llm.LlmDebug;
import com.nuclearunicorn.serialkiller.game.ai.llm.LlmRuntime;
import com.nuclearunicorn.serialkiller.game.ai.llm.Perception;
import com.nuclearunicorn.serialkiller.game.ai.llm.sense.GameTurn;
import com.nuclearunicorn.serialkiller.game.ai.llm.sense.Relations;
import com.nuclearunicorn.serialkiller.game.ai.llm.sense.Salience;
import com.nuclearunicorn.serialkiller.game.ai.llm.sense.Stimulus;
import com.nuclearunicorn.serialkiller.game.ai.mind.Deliberation;
import com.nuclearunicorn.serialkiller.game.ai.mind.Knowledge;
import com.nuclearunicorn.serialkiller.game.ai.mind.Narrating;
import com.nuclearunicorn.serialkiller.game.ai.mind.Percept;
import com.nuclearunicorn.serialkiller.game.ai.mind.Persona;
import com.nuclearunicorn.serialkiller.game.ai.mind.Voice;
import com.nuclearunicorn.serialkiller.game.controllers.RLController;
import com.nuclearunicorn.serialkiller.game.events.NPCReportCrimeEvent;
import com.nuclearunicorn.serialkiller.game.events.NPCWitnessCrimeEvent;
import com.nuclearunicorn.serialkiller.game.world.entities.EntityDoor;
import com.nuclearunicorn.serialkiller.game.world.entities.EntityFurniture;
import com.nuclearunicorn.serialkiller.game.world.entities.EntityRLHuman;
import org.lwjgl.util.Point;

/**
 * Base brain for anyone who lives in the town, and the place its parts are assembled.
 *
 * <p>It owns three components and almost no behaviour of its own:
 *
 * <ul>
 *   <li>{@link Knowledge} — what this NPC believes: a ranked stream of what happened, and a
 *       {@link Percept} per person it has an opinion about.</li>
 *   <li>{@link Voice} — everything it says, and the record of having said it.</li>
 *   <li>{@link Deliberation} — the LLM planner, present only when inference is on. It is a
 *       component precisely so that switching it on does not replace the NPC's reflexes.</li>
 * </ul>
 *
 * <p>Deciding what to do is a walk down the {@link Impulse} list registered by the subclass,
 * and doing it is one {@link IAIAction}. Subclasses differ by which impulses they register:
 * a pedestrian runs from whoever hit her, an officer chases him instead — same machinery,
 * different priorities, no flag anywhere asking which kind of person this is.
 */
public abstract class TownAI extends BasicMobAI implements Persona {

    /** Nothing better to do. Every subclass registers something for this. */
    public static final String AI_STATE_IDLE = "ai_state_IDLE";
    /** The model has the body: run whatever plan came back. */
    public static final String AI_STATE_DELIBERATE = "ai_state_DELIBERATE";

    private Knowledge knowledge;
    private Voice voice;
    private transient Deliberation deliberation;
    private transient boolean wired;

    /** Rebuild transient wiring (also after deserialization). Components need a live controller. */
    protected boolean ensureWired() {
        if (wired) {
            return true;
        }
        if (!(owner instanceof EntityRLHuman) || !(owner.controller instanceof RLController)) {
            return false;
        }
        if (knowledge == null) {
            knowledge = new Knowledge();
        }
        if (voice == null) {
            voice = new Voice((EntityRLHuman) owner);
        }
        if (deliberation == null && LlmRuntime.isEnabled()) {
            deliberation = new Deliberation((EntityRLHuman) owner, (RLController) owner.controller,
                    knowledge, voice);
            LlmDebug.log("%s (%s): %s wired and live", owner.get_uid(), owner.getName(),
                    getClass().getSimpleName());
        }
        wired = true;
        return true;
    }

    public Knowledge knowledge() {
        return knowledge;
    }

    public Voice voice() {
        return voice;
    }

    /** The LLM planner, or null when inference is off. Behaviours must tolerate null. */
    public Deliberation deliberation() {
        return deliberation;
    }

    public EntityRLHuman human() {
        return (EntityRLHuman) owner;
    }

    public RLController ctrl() {
        return (RLController) owner.controller;
    }

    /** Resolve a uid we hold a belief about to the live entity, or null if it is gone. */
    public Entity entity(String uid) {
        if (uid == null || owner == null || owner.getEnvironment() == null) {
            return null;
        }
        return owner.getEnvironment().getEntityManager().get_entity(uid);
    }

    public static boolean isAlive(Entity ent) {
        return ent != null && (ent.get_combat() == null || ent.get_combat().is_alive());
    }

    @Override
    public void update() {
        if (!ensureWired()) {
            return;
        }
        if (deliberation != null) {
            deliberation.pump(situation(), isAsleep());
        }
        Impulse impulse = selectImpulse();
        setState(impulse == null ? AI_STATE_IDLE : impulse.state());
    }

    /**
     * In bed for the night. Asleep is not a mood — it gates the senses, the planner and the
     * impulse list together, and every one of them has to agree or the NPC talks in its
     * sleep. See {@link SleepAction}.
     */
    public boolean isAsleep() {
        return SleepAction.STATE.equals(state);
    }

    @Override
    public void think() {
        if (!ensureWired()) {
            return;
        }
        super.think();
    }

    /**
     * What to tell the model about who we are and what we are already doing. The persona is
     * this class; the situation line comes from the behaviour that currently has the body,
     * because that is the only thing that actually knows.
     */
    protected Perception.Situation situation() {
        Perception.Situation s = new Perception.Situation();
        s.persona = this;
        IAIAction action = activeAction();
        if (action instanceof Narrating) {
            s.doing = ((Narrating) action).narrate();
            s.reflexive = true;   // a reflex has the body; do not advise it to stand and chat
        }
        return s;
    }

    /** The ordinary case. Subclasses append what the NPC is <i>for</i>. */
    @Override
    public void describeSelf(StringBuilder sb, EntityRLHuman self) {
        Perception.appendIdentity(sb, self);
        sb.append(" living in this town.\n");
    }

    /**
     * Sensor entry point (§7). Sensors call this; salience decides everything downstream —
     * whether it survives memory eviction, where it sits in the inference queue, and whether
     * it preempts the running plan. Anything that also changes what we <i>believe</i> is
     * folded into {@link Knowledge} here, because a stimulus decays and a belief should not.
     */
    public void sense(Stimulus stimulus) {
        if (!ensureWired()) {
            return;
        }
        // Asleep is deaf, and it is the second half of a rule the ears started: Acoustics
        // already raised this listener's threshold by HEAR_ASLEEP, and this says that what
        // still gets through is not something to answer with your eyes shut. Only PAIN
        // reaches a sleeper — which is the whole of "you wake someone up by hurting them",
        // and it works because the flee reflex outranks bedtime and fires on the same turn
        // as the blow.
        if (isAsleep() && stimulus.channel != Stimulus.Channel.PAIN) {
            LlmDebug.log("%s slept through %s", owner.get_uid(), stimulus);
            return;
        }
        knowledge.record(stimulus);
        LlmDebug.log("%s sensed %s", owner.get_uid(), stimulus);

        if (stimulus.channel == Stimulus.Channel.PAIN) {
            knowledge.wasAttackedBy(stimulus.sourceUid, stimulus.turn);
        }
    }

    /** A line this NPC heard, recorded verbatim and in order (§8). Not while asleep. */
    public void hear(String speaker, String text, boolean directed) {
        if (!ensureWired() || isAsleep()) {
            return;   // a conversation you slept through is not one you can quote in the morning
        }
        voice.heard(speaker, text, directed);
    }

    /**
     * Crime, arriving by one of two routes: {@link NPCWitnessCrimeEvent} when this NPC was
     * present, {@link NPCReportCrimeEvent} when someone called it in. Both name a place, only
     * the first names a person, and the difference is recorded — an eyewitness account and a
     * radio call should not produce the same certainty.
     */
    @Override
    public void e_on_event(Event event) {
        if (!ensureWired()) {
            return;
        }
        if (event instanceof NPCWitnessCrimeEvent) {
            NPCWitnessCrimeEvent crime = (NPCWitnessCrimeEvent) event;
            Entity criminal = crime.criminal;
            if (criminal == null || criminal.get_uid() == null
                    || criminal.get_uid().equals(owner.get_uid())) {
                return;
            }
            knowledge.learnedOfCrime(criminal.get_uid(), crime.origin, GameTurn.current(), true);
            sense(new Stimulus(GameTurn.current(), Stimulus.Channel.SIGHT, witnessSalience(),
                    criminal.get_uid(), "you saw " + Relations.describe(owner, criminal)
                            + " commit a crime right in front of you"));
            return;
        }
        if (event instanceof NPCReportCrimeEvent) {
            onCrimeReport((NPCReportCrimeEvent) event);
        }
    }

    /**
     * How hard witnessing a crime lands. A bystander files it away; whether anyone should
     * drop what they are doing over it is a question only the subclass can answer.
     */
    public int witnessSalience() {
        return Salience.NOTABLE;
    }

    /** A crime called in from elsewhere in town. Ignored by anyone with no duty to attend. */
    protected void onCrimeReport(NPCReportCrimeEvent report) {
    }

    /**
     * Contact. The controller reports every blocked step here, and walking into an occupied
     * tile is how every melee in this game happens — so contact is offered to the behaviour
     * that asked for the step first, and only then falls back to the things anybody does.
     *
     * <p>Which is now almost nothing. This used to break any furniture it walked into, and
     * since the pathfinder was routing people through windows on purpose, the two together
     * had the town smashing its own glass on the way to bed. Walking into something solid is
     * a stale route, not an intention: drop the step and let the behaviour ask for another.
     * Breaking things is left to the behaviour that means it — see {@link
     * com.nuclearunicorn.serialkiller.game.ai.behavior.FleeAction#onObstacle}.
     */
    @Override
    public void e_on_obstacle(int x, int y) {
        if (owner == null || owner.getLayer() == null) {
            return;
        }
        WorldTile tile = owner.getLayer().get_tile(x, y);
        Entity blocker = tile == null ? null : tile.get_actor();
        if (blocker == null) {
            return;
        }
        IAIAction action = activeAction();
        if (action != null && owner.controller instanceof NpcController
                && action.onObstacle((NpcController) owner.controller, blocker)) {
            return;
        }
        if (blocker instanceof EntityDoor) {
            ((EntityDoor) blocker).unlock();   // an agent brain must not be worse at doors
        }
    }

    /**
     * Brain state for the replay log. This is the view that tells you <i>why</i> an NPC
     * ignored you: whether the stimulus arrived at all, what salience it carries now, and
     * whether the trigger or the queue is the thing holding the reaction back.
     */
    public String debugState() {
        if (knowledge == null) {
            return "unwired";
        }
        Stimulus top = knowledge.stream().peekTop();
        Stimulus focus = knowledge.stream().peekStrongest();
        Percept threat = knowledge.threat();
        Percept suspect = knowledge.suspect();
        Point scene = knowledge.openCrimeScene();
        return "top=" + (top == null ? "none" : Salience.label(top.salience) + ":" + top.text())
                + " topSalience=" + knowledge.stream().topSalience()
                // What the prompt actually leads with, which is not always the trigger.
                + " focus=" + (focus == null || focus == top
                        ? "same" : Salience.label(focus.salience) + ":" + focus.text())
                + (deliberation == null ? " llm=off" : deliberation.debugState())
                + " brain=" + getClass().getSimpleName()
                + " state=" + (state == null ? "none" : state)
                + " threat=" + (threat == null ? "no" : threat.uid)
                + " suspect=" + (suspect == null ? "no" : suspect.uid)
                + " scene=" + (scene == null ? "no" : scene.getX() + "," + scene.getY());
    }
}
