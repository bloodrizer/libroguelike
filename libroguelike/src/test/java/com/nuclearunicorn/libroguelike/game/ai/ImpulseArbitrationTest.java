package com.nuclearunicorn.libroguelike.game.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Arbitration (§1 of INVARIANTS.md). The impulse list is the whole of what it means to be
 * one kind of person rather than another here, so the order it resolves in is worth pinning
 * down: a policeman is a pedestrian who declines to flee and outranks bedtime with a chase.
 *
 * <p>In the same package as {@link AI} on purpose — registration and selection are protected,
 * because they are for subclasses and for this.
 */
class ImpulseArbitrationTest {

    /** An impulse whose relevance is a field, so a test can stage the situation directly. */
    private static class Urge implements Impulse {
        private final String name;
        boolean relevant;

        Urge(String name, boolean relevant) {
            this.name = name;
            this.relevant = relevant;
        }

        public String name() { return name; }
        public String state() { return "state_" + name; }
        public boolean isRelevant() { return relevant; }
    }

    /** Exposes the protected arbitration so a test can drive it. */
    private static class TestBrain extends AI {
        void add(int priority, Impulse impulse) { registerImpulse(priority, impulse); }
        void drop(String name) { removeImpulse(name); }
        Impulse choose() { return selectImpulse(); }
    }

    @Test
    void highestPriorityRelevantImpulseWins() {
        TestBrain brain = new TestBrain();
        brain.add(10, new Urge("patrol", true));
        brain.add(100, new Urge("threat", true));
        brain.add(40, new Urge("plan", true));
        assertEquals("threat", brain.choose().name());
    }

    /** Registration order is not priority order: subclasses register after super(). */
    @Test
    void loweringPriorityDoesNotDependOnRegistrationOrder() {
        TestBrain brain = new TestBrain();
        brain.add(100, new Urge("threat", false));
        brain.add(10, new Urge("patrol", true));
        brain.add(90, new Urge("suspect", true));
        assertEquals("suspect", brain.choose().name(),
                "an irrelevant higher impulse must be skipped, not block the list");
    }

    /** Equal priorities keep registration order — how the sleep/commute pair splits. */
    @Test
    void tiesKeepRegistrationOrder() {
        TestBrain brain = new TestBrain();
        brain.add(30, new Urge("asleep", true));
        brain.add(30, new Urge("night", true));
        assertEquals("asleep", brain.choose().name());
    }

    @Test
    void nothingRelevantMeansNoImpulse() {
        TestBrain brain = new TestBrain();
        brain.add(100, new Urge("threat", false));
        brain.add(10, new Urge("patrol", false));
        assertNull(brain.choose());
    }

    /** How a subclass declines an inherited urge — an officer does not flee or go to bed. */
    @Test
    void removedImpulseNoLongerFires() {
        TestBrain brain = new TestBrain();
        brain.add(100, new Urge("threat", true));
        brain.add(10, new Urge("patrol", true));
        brain.drop("threat");
        assertEquals("patrol", brain.choose().name());
    }

    @Test
    void removingAnUnknownNameIsHarmless() {
        TestBrain brain = new TestBrain();
        brain.add(10, new Urge("patrol", true));
        brain.drop("nonexistent");
        assertEquals("patrol", brain.choose().name());
    }

    /** Selection re-reads relevance every time; it is not latched at registration. */
    @Test
    void selectionFollowsChangingRelevance() {
        TestBrain brain = new TestBrain();
        Urge threat = new Urge("threat", false);
        brain.add(100, threat);
        brain.add(10, new Urge("patrol", true));
        assertEquals("patrol", brain.choose().name());

        threat.relevant = true;
        assertEquals("threat", brain.choose().name());
    }
}
