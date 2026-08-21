package com.nuclearunicorn.libroguelike.game.ai;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What {@link AI#debugImpulses()} is allowed to be: a recording of the walk that actually
 * happened, never a re-run of it.
 *
 * <p>The distinction is the whole point. Triggers are stateful — the flee trigger starts its
 * own stopwatch the first turn it says yes and logs when it lets go — so a debug view that
 * asked "are you relevant?" to build its table would be starting that stopwatch from the
 * render thread, sixty times a second, on an NPC nobody had attacked. {@code timesAsked}
 * below is what pins that down.
 */
class ImpulseWalkRecordTest {

    /** Counts how often it was consulted, so the test can prove the overlay never asks. */
    private static class Urge implements Impulse {
        private final String name;
        boolean relevant;
        int timesAsked;

        Urge(String name, boolean relevant) {
            this.name = name;
            this.relevant = relevant;
        }

        public String name() { return name; }
        public String state() { return "state_" + name; }

        public boolean isRelevant() {
            timesAsked++;
            return relevant;
        }
    }

    private static class TestBrain extends AI {
        void add(int priority, Impulse impulse) { registerImpulse(priority, impulse); }
        Impulse choose() { return selectImpulse(); }
    }

    @Test
    void theWalkIsRecordedInPriorityOrderWithTheWinnerMarked() {
        TestBrain brain = new TestBrain();
        brain.add(100, new Urge("threat", false));
        brain.add(40, new Urge("plan", true));
        brain.add(10, new Urge("patrol", true));
        brain.choose();

        List<AI.ImpulseView> walk = brain.debugImpulses();
        assertEquals(3, walk.size());
        assertEquals("threat", walk.get(0).name);
        assertEquals(AI.ImpulseView.Verdict.NO, walk.get(0).verdict);
        assertFalse(walk.get(0).selected);

        assertEquals("plan", walk.get(1).name);
        assertEquals(AI.ImpulseView.Verdict.YES, walk.get(1).verdict);
        assertTrue(walk.get(1).selected, "the first relevant impulse is the one that won");
        assertEquals("state_plan", walk.get(1).state);
    }

    /**
     * Selection stops at the first yes, so everything under it was never consulted — and
     * "not asked" is not the same claim as "said no". Reporting it as a no would say the
     * patrol trigger declined, when in fact nothing ever put the question to it.
     */
    @Test
    void impulsesBelowTheWinnerAreRecordedAsNeverAsked() {
        TestBrain brain = new TestBrain();
        Urge threat = new Urge("threat", true);
        Urge patrol = new Urge("patrol", true);
        brain.add(100, threat);
        brain.add(10, patrol);
        brain.choose();

        assertEquals(AI.ImpulseView.Verdict.NOT_ASKED, brain.debugImpulses().get(1).verdict);
        assertEquals(0, patrol.timesAsked, "nothing below the winner may be evaluated");
        assertEquals(1, threat.timesAsked);
    }

    /** Reading the table is free: a render pass must never move a trigger's own state. */
    @Test
    void readingTheWalkDoesNotConsultAnyTrigger() {
        TestBrain brain = new TestBrain();
        Urge threat = new Urge("threat", false);
        brain.add(100, threat);
        brain.choose();
        assertEquals(1, threat.timesAsked);

        for (int frame = 0; frame < 60; frame++) {
            brain.debugImpulses();
        }
        assertEquals(1, threat.timesAsked, "the overlay reads the recording, it does not re-run it");
    }

    /** Before the first update there is nothing to show — and nothing to blow up on either. */
    @Test
    void anUndecidedBrainReportsAnEmptyWalk() {
        TestBrain brain = new TestBrain();
        brain.add(10, new Urge("patrol", true));
        assertTrue(brain.debugImpulses().isEmpty());
    }

    /** The recording is this turn's, not a growing log of every turn so far. */
    @Test
    void eachWalkReplacesTheLast() {
        TestBrain brain = new TestBrain();
        Urge threat = new Urge("threat", false);
        brain.add(100, threat);
        brain.add(10, new Urge("patrol", true));

        brain.choose();
        assertEquals(AI.ImpulseView.Verdict.NO, brain.debugImpulses().get(0).verdict);

        threat.relevant = true;
        brain.choose();
        assertEquals(2, brain.debugImpulses().size());
        assertEquals(AI.ImpulseView.Verdict.YES, brain.debugImpulses().get(0).verdict);
        assertEquals(AI.ImpulseView.Verdict.NOT_ASKED, brain.debugImpulses().get(1).verdict);
    }
}
