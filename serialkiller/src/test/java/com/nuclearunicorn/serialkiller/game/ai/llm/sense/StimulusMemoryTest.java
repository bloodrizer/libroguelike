package com.nuclearunicorn.serialkiller.game.ai.llm.sense;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Working memory (§6 of INVARIANTS.md). Two rules carry the whole design, and both are the
 * kind of thing that silently stops holding: eviction drops the <b>least salient</b> entry
 * rather than the oldest, and salience <b>decays with age</b>.
 *
 * <p>The first rule exists because a ring buffer let a burst of small talk push out the one
 * line that mattered — measured on phi-4-mini, a diluted memory turned half of all
 * completions into an empty command array.
 */
class StimulusMemoryTest {

    /** GameTurn is a process-wide counter with no reset, so tests work relative to now. */
    private static long now() {
        return GameTurn.current();
    }

    private static Stimulus at(long turn, int salience, String text) {
        return new Stimulus(turn, Stimulus.Channel.SPEECH, salience, null, text);
    }

    @Test
    void emptyMemoryHasNothingToSay() {
        StimulusMemory memory = new StimulusMemory(8, 2);
        assertTrue(memory.isEmpty());
        assertNull(memory.peekTop());
        assertNull(memory.peekStrongest());
        assertEquals(0, memory.topSalience());
    }

    @Test
    void strongestUnconsumedStimulusIsOnTop() {
        StimulusMemory memory = new StimulusMemory(8, 0);
        memory.add(at(now(), Salience.AMBIENT, "someone coughs"));
        memory.add(at(now(), Salience.URGENT, "you have been stabbed"));
        memory.add(at(now(), Salience.NOTABLE, "a door slams"));
        assertEquals("you have been stabbed", memory.peekTop().text());
    }

    /** Eviction is by salience, not by age: the urgent line survives a flood of chatter. */
    @Test
    void theLeastSalientEntryIsEvicted() {
        StimulusMemory memory = new StimulusMemory(4, 0);
        memory.add(at(now(), Salience.URGENT, "you have been stabbed"));
        for (int i = 0; i < 10; i++) {
            memory.add(at(now(), Salience.AMBIENT, "small talk " + i));
        }
        assertNotNull(memory.peekTop());
        assertEquals("you have been stabbed", memory.peekTop().text(),
                "a burst of ambient noise evicted the one line that mattered");
    }

    @Test
    void salienceDecaysWithAge() {
        Stimulus shout = at(100, Salience.URGENT, "a shout");
        assertEquals(Salience.URGENT, shout.effectiveSalience(100, 2));
        assertEquals(Salience.URGENT - 20, shout.effectiveSalience(110, 2));
    }

    @Test
    void decayNeverGoesBelowZeroOrRunsBackwards() {
        Stimulus shout = at(100, Salience.NOTABLE, "a shout");
        assertEquals(0, shout.effectiveSalience(1000, 2), "salience floors at zero");
        assertEquals(Salience.NOTABLE, shout.effectiveSalience(50, 2),
                "a stimulus from the future is not more salient than a fresh one");
    }

    /**
     * Being prompted about something once does not make it stop being true. peekTop is for
     * deciding whether to re-plan; peekStrongest is for framing the prompt, and a victim who
     * has already been asked about the stabbing must not be led with the weather instead.
     */
    @Test
    void consumedStimuliLeaveTopButNotStrongest() {
        StimulusMemory memory = new StimulusMemory(8, 0);
        memory.add(at(now(), Salience.URGENT, "you have been stabbed"));
        memory.markConsumed();
        memory.add(at(now(), Salience.AMBIENT, "nice weather"));

        assertEquals("nice weather", memory.peekTop().text(), "only the new line can trigger");
        assertEquals("you have been stabbed", memory.peekStrongest().text(),
                "but the stabbing is still what matters most");
    }

    @Test
    void topSalienceReportsTheDecayedValue() {
        StimulusMemory memory = new StimulusMemory(8, 2);
        memory.add(at(now() - 5, Salience.URGENT, "a shout"));
        assertEquals(Salience.URGENT - 10, memory.topSalience());
    }

    /** Eviction and consumption both erase the evidence, so "did anything happen" is its own clock. */
    @Test
    void lastAddedTurnSurvivesEviction() {
        StimulusMemory memory = new StimulusMemory(2, 0);
        long turn = now();
        memory.add(at(turn, Salience.URGENT, "first"));
        memory.add(at(turn + 3, Salience.AMBIENT, "second"));
        memory.add(at(turn + 7, Salience.AMBIENT, "third"));
        assertEquals(turn + 7, memory.lastAddedTurn());
    }

    @Test
    void rankedListIsStrongestFirst() {
        StimulusMemory memory = new StimulusMemory(8, 0);
        memory.add(at(now(), Salience.AMBIENT, "quiet"));
        memory.add(at(now(), Salience.URGENT, "loud"));
        assertEquals("loud", memory.ranked().get(0).text());
    }
}
