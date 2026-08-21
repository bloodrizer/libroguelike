/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.game.ai;

import com.nuclearunicorn.libroguelike.events.Event;
import com.nuclearunicorn.libroguelike.events.IEventListener;
import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.game.ent.controller.NpcController;
import com.nuclearunicorn.libroguelike.utils.Fov;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base brain: a set of {@link IAIAction} behaviours keyed by state name, and a prioritised
 * list of {@link Impulse} triggers that decides which of them owns the body this turn.
 *
 * <p>The two halves are deliberately separate (Isla, <i>Handling Complexity in the Halo 2
 * AI</i>, GDC 2005): impulses answer <i>when</i>, actions answer <i>what</i>. Selection is
 * a walk down the impulse list taking the first relevant one, which is the same
 * arbitration a subsumption architecture uses — a higher layer suppresses everything below
 * it, so fleeing does not have to know that patrolling exists.
 *
 * @author Administrator
 */
public class AI implements Serializable, IEventListener{

    protected String state;
    protected Map<String,IAIAction> stateMap = new HashMap<String, IAIAction>();

    /** Impulses in descending priority. First relevant one names the state (§ Halo 2). */
    private final List<RankedImpulse> impulses = new ArrayList<RankedImpulse>();

    /** The action currently holding the body, so entry/exit fire exactly once each. */
    private transient IAIAction active;

    /** The last walk down the impulse list, for the debug overlay. Rebuilt every update. */
    private transient List<ImpulseView> walk;

    /** Transient, so it comes back null from a save: build it on demand. */
    private List<ImpulseView> walk(){
        if (walk == null){
            walk = new ArrayList<ImpulseView>();
        }
        return walk;
    }

    protected void registerState(String name, IAIAction action){
        stateMap.put(name, action);
    }

    /**
     * Register a trigger. Higher {@code priority} wins; ties keep registration order. Explicit
     * numbers rather than list position because subclasses register <i>after</i> {@code super()}
     * and routinely need to outrank what the parent set up — a policeman's urge to give chase
     * has to beat a pedestrian's urge to go to bed.
     */
    protected void registerImpulse(int priority, Impulse impulse){
        RankedImpulse ranked = new RankedImpulse(priority, impulse);
        int at = 0;
        while (at < impulses.size() && impulses.get(at).priority >= priority){
            at++;
        }
        impulses.add(at, ranked);
    }

    /** Drop a trigger by name. How a subclass declines an inherited urge — see PoliceAI. */
    protected void removeImpulse(String name){
        impulses.removeIf(ranked -> ranked.impulse.name().equals(name));
    }

    /**
     * First relevant impulse, or null when nothing applies. The walk is recorded as it
     * happens — see {@link #debugImpulses()}.
     */
    protected Impulse selectImpulse(){
        List<ImpulseView> walk = walk();
        walk.clear();
        Impulse selected = null;
        for (RankedImpulse ranked : impulses){
            if (selected != null){
                // Never asked: selection stops at the first yes, and a debug view that
                // asked anyway would report a decision that was not taken.
                walk.add(new ImpulseView(ranked, ImpulseView.Verdict.NOT_ASKED, false));
                continue;
            }
            boolean relevant = ranked.impulse.isRelevant();
            walk.add(new ImpulseView(ranked,
                    relevant ? ImpulseView.Verdict.YES : ImpulseView.Verdict.NO, relevant));
            if (relevant){
                selected = ranked.impulse;
            }
        }
        return selected;
    }

    protected Entity owner;
    public void set_owner(Entity owner){
        this.owner = owner;
    }

    public void update(){

    }

    public void think(){

    }

    /** Run the action for the current state, firing entry/exit as the selection changes. */
    protected void act(NpcController npcController){
        IAIAction action = stateMap.get(state);
        if (action != active){
            if (active != null){
                active.onExit();
            }
            active = action;
            if (active != null){
                active.onEnter();
            }
        }
        if (action != null){
            action.act(npcController);
        }
    }

    /** The action holding the body right now, or null. */
    protected IAIAction activeAction(){
        return active;
    }

    /** Same, for the debug overlay: what is actually driving the body this turn. */
    public IAIAction debugActiveAction(){
        return active;
    }

    /**
     * The last impulse walk, highest priority first: every trigger, the answer it gave, and
     * the one that won. Deciding what to do <i>is</i> this walk, so this is the decision
     * itself — "why is he not chasing me" is answered by whichever row above the chase row
     * said yes.
     *
     * <p>Recorded during {@link #selectImpulse()} rather than re-run on demand, and that is
     * not an optimisation. Triggers are stateful: a flee trigger starts its own stopwatch
     * the first time it says yes and logs when it lets go. Asking one whether it is relevant
     * from a render pass is not a question, it is a change — sixty times a second.
     */
    public List<ImpulseView> debugImpulses(){
        return walk();
    }

    /** One row of {@link #debugImpulses}: a trigger, its answer, and whether it won. */
    public static class ImpulseView {

        /** {@code NOT_ASKED}: something above it already said yes, so it was never consulted. */
        public enum Verdict { YES, NO, NOT_ASKED }

        public final int priority;
        public final String name;
        public final String state;
        public final Verdict verdict;
        /** The first relevant one — the row that actually named this turn's state. */
        public final boolean selected;

        ImpulseView(RankedImpulse ranked, Verdict verdict, boolean selected){
            this.priority = ranked.priority;
            this.name = ranked.impulse.name();
            this.state = ranked.impulse.state();
            this.verdict = verdict;
            this.selected = selected;
        }
    }

    public boolean entity_in_fov(Entity ent){
        //todo: implement combat.get_fov();
        if (Fov.in_range(owner.origin, ent.origin, 5)){
            return true;
        }

        return false;
    }

    @Override
    public void e_on_event(Event event) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    //this event is triggered when Entity Controller hits obstacle
    //implement behavior logic there (e.g. attacking target, etc)
    public void e_on_obstacle(int x, int y) {
        //override me
    }


    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    private static class RankedImpulse implements Serializable {
        final int priority;
        final Impulse impulse;

        RankedImpulse(int priority, Impulse impulse){
            this.priority = priority;
            this.impulse = impulse;
        }
    }
}
