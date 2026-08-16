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

    /** First relevant impulse, or null when nothing applies. */
    protected Impulse selectImpulse(){
        for (RankedImpulse ranked : impulses){
            if (ranked.impulse.isRelevant()){
                return ranked.impulse;
            }
        }
        return null;
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
