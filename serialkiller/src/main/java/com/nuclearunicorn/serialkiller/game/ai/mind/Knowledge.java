package com.nuclearunicorn.serialkiller.game.ai.mind;

import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.serialkiller.game.ai.llm.sense.GameTurn;
import com.nuclearunicorn.serialkiller.game.ai.llm.sense.Stimulus;
import com.nuclearunicorn.serialkiller.game.ai.llm.sense.StimulusMemory;
import org.lwjgl.util.Point;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * What an NPC knows: a ranked stream of things that happened ({@link StimulusMemory}) and a
 * set of beliefs about particular people ({@link Percept}). F.E.A.R.'s working memory, in
 * the two halves it actually has — episodic and per-object.
 *
 * <p>Keeping them together is the point. Before this, the stream lived on the LLM brain and
 * the per-person facts lived as a {@code List<EntityActor> knowCriminals} on the FSM one, so
 * a policeman who <i>heard</i> about a murder had no way to connect that to the person
 * standing in front of him, and a policeman running the LLM brain had no criminal list at
 * all. One component, both halves, and every behaviour asks it the same questions.
 *
 * <p>Nothing here decides anything. It answers "who hurt me", "who is wanted", "where was
 * the last crime"; whether that is worth fleeing or arresting is a behaviour's business.
 */
public class Knowledge implements Serializable {

    private final StimulusMemory stream;
    private final Map<String, Percept> beliefs = new HashMap<String, Percept>();

    /** Crime scene nobody has closed yet — what a patrolling officer walks towards. */
    private Point openCrimeScene;
    private long openCrimeTurn = -1;

    public Knowledge() {
        this.stream = new StimulusMemory(Tuning.memory().observations, Tuning.priority().decayPerTurn);
    }

    public StimulusMemory stream() {
        return stream;
    }

    /** Everything we believe, so a debug dump can show why an NPC is behaving as it is. */
    public Map<String, Percept> beliefs() {
        return beliefs;
    }

    public Percept about(String uid) {
        if (uid == null) {
            return null;
        }
        Percept percept = beliefs.get(uid);
        if (percept == null) {
            percept = new Percept(uid);
            beliefs.put(uid, percept);
        }
        return percept;
    }

    /** Record a stimulus and, if it names someone, that we now have them in mind. */
    public void record(Stimulus stimulus) {
        stream.add(stimulus);
        if (stimulus.sourceUid != null) {
            about(stimulus.sourceUid).saw(stimulus.turn, null, Percept.Source.SEEN);
        }
    }

    public void saw(Entity ent, long turn) {
        if (ent == null || ent.get_uid() == null) {
            return;
        }
        about(ent.get_uid()).saw(turn, ent.origin, Percept.Source.SEEN);
    }

    /** They hurt us. The one fact that outranks everything a civilian was doing. */
    public void wasAttackedBy(String uid, long turn) {
        if (uid == null) {
            return;
        }
        about(uid).noteAttack(turn);
    }

    /**
     * They committed a crime. {@code firsthand} separates seeing it from being told, which
     * is what stops a rumour from being treated as an eyewitness account — Thief calls the
     * same control "constraining knowledge cascades".
     */
    public void learnedOfCrime(String uid, Point where, long turn, boolean firsthand) {
        if (uid != null) {
            Percept percept = about(uid);
            percept.noteCrime(turn, where);
            percept.saw(turn, where, firsthand ? Percept.Source.SEEN : Percept.Source.REPORTED);
        }
        if (where != null && turn >= openCrimeTurn) {
            openCrimeScene = new Point(where);
            openCrimeTurn = turn;
        }
    }

    /** Whoever hurt us most recently and recently enough to still be running from. */
    public Percept threat() {
        return mostRecent(true);
    }

    /** Whoever we most recently learned is a criminal, while that knowledge is still live. */
    public Percept suspect() {
        return mostRecent(false);
    }

    private Percept mostRecent(boolean threat) {
        long now = GameTurn.current();
        int ttl = threat ? Tuning.priority().fleeMaxTurns : Tuning.priority().pursueMaxTurns;
        Percept best = null;
        long bestTurn = -1;
        for (Percept percept : beliefs.values()) {
            long turn = threat ? percept.attackedUsTurn : percept.crimeTurn;
            if (turn < 0 || now - turn > ttl || turn <= bestTurn) {
                continue;
            }
            bestTurn = turn;
            best = percept;
        }
        return best;
    }

    public Point openCrimeScene() {
        return openCrimeScene;
    }

    /** An officer reached the scene, or the case went cold. Stops him walking there forever. */
    public void closeCrimeScene() {
        openCrimeScene = null;
        openCrimeTurn = -1;
    }
}
