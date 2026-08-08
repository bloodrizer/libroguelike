package com.nuclearunicorn.serialkiller.game.ai.llm.sense;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Per-NPC ranked short-term memory (§7). Replaces the plain ring buffer, whose
 * drop-oldest-first rule let a burst of ambient events evict the one line that mattered —
 * measured on phi-4-mini, a diluted memory turned half of all completions into an empty
 * command array.
 *
 * <p>Two rules do the work: eviction drops the <b>least salient</b> entry rather than the
 * oldest, and salience <b>decays with age</b> so nothing stays urgent forever.
 */
public class StimulusMemory implements Serializable {

    private final List<Stimulus> entries = new ArrayList<>();
    private final int capacity;
    private final int decayPerTurn;
    /** Turn of the most recent arrival; -1 when nothing has ever been sensed. */
    private long lastAddedTurn = -1;

    public StimulusMemory(int capacity, int decayPerTurn) {
        this.capacity = Math.max(1, capacity);
        this.decayPerTurn = Math.max(0, decayPerTurn);
    }

    /**
     * When we last sensed anything at all. Eviction and consumption both erase the evidence
     * that something happened, so "is there any reason to re-plan?" needs its own clock.
     */
    public long lastAddedTurn() {
        return lastAddedTurn;
    }

    /** Record a stimulus, evicting the least salient entry if that puts us over capacity. */
    public void add(Stimulus stimulus) {
        entries.add(stimulus);
        lastAddedTurn = Math.max(lastAddedTurn, stimulus.turn);
        if (entries.size() <= capacity) {
            return;
        }
        long now = GameTurn.current();
        Stimulus weakest = entries.get(0);
        for (Stimulus s : entries) {
            if (rank(s, now) < rank(weakest, now)) {
                weakest = s;
            }
        }
        entries.remove(weakest);
    }

    /** Consumed entries rank below unconsumed ones at the same salience, then by age. */
    private long rank(Stimulus s, long now) {
        long score = s.effectiveSalience(now, decayPerTurn);
        return s.consumed ? score : score + 1000;
    }

    /**
     * The strongest unconsumed stimulus, or null if there is nothing new. This is what
     * decides whether to interrupt the running plan and at what priority to queue.
     */
    public Stimulus peekTop() {
        long now = GameTurn.current();
        Stimulus top = null;
        int best = -1;
        for (Stimulus s : entries) {
            if (s.consumed) {
                continue;
            }
            int score = s.effectiveSalience(now, decayPerTurn);
            if (score > best) {
                best = score;
                top = s;
            }
        }
        return top;
    }

    /** Effective salience of the strongest unconsumed stimulus; 0 when there is none. */
    public int topSalience() {
        Stimulus top = peekTop();
        return top == null ? 0 : top.effectiveSalience(GameTurn.current(), decayPerTurn);
    }

    /**
     * Everything still worth showing the model, strongest first. Consumed entries stay as
     * background context so the NPC remembers what it already answered.
     */
    public List<Stimulus> ranked() {
        long now = GameTurn.current();
        List<Stimulus> sorted = new ArrayList<>(entries);
        sorted.sort(Comparator.comparingLong((Stimulus s) -> rank(s, now)).reversed());
        return sorted;
    }

    /**
     * Mark everything currently unconsumed as folded into a prompt. Called at submit time,
     * not at reply time — otherwise a slow round-trip re-triggers on the same stimulus and
     * the NPC answers "hello" three times.
     */
    public void markConsumed() {
        for (Stimulus s : entries) {
            s.consumed = true;
        }
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }
}
