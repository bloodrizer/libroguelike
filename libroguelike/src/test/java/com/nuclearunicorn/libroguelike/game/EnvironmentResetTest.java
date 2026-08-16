package com.nuclearunicorn.libroguelike.game;

import com.nuclearunicorn.libroguelike.events.Event;
import com.nuclearunicorn.libroguelike.events.EventManager;
import com.nuclearunicorn.libroguelike.events.IEventListener;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What {@link GameEnvironment#reset} leaves behind.
 *
 * <p>Written after a session where the second town of a run was completely deaf: every NPC
 * ignored the player and each other, with no exception, no warning and nothing in the log.
 * The cause was one line — {@code reset()} clears the event manager's listener list, and the
 * services subscribed at startup all guarded on "have I been created before?", so they
 * survived the reset as objects but not as subscribers.
 *
 * <p>The class of bug is worth a test even though the individual fixes are trivial: an
 * unsubscribed listener fails <i>silently and permanently</i>, and nothing about playing the
 * game points at the reset that caused it half an hour earlier.
 */
class EnvironmentResetTest {

    private static class Env extends GameEnvironment {
        private final EventManager events = new EventManager();

        Env() {
            super("test-env");
        }

        @Override
        public EventManager getEventManager() {
            return events;
        }
    }

    /** A listener that re-subscribes on every world, the way the sensors now do. */
    private static class Service implements IEventListener {
        int seen;

        @Override
        public void e_on_event(Event event) {
            seen++;
        }
    }

    @Test
    void resetUnsubscribesEverything() {
        Env env = new Env();
        Service service = new Service();
        env.getEventManager().subscribe(service);
        assertTrue(env.getEventManager().hasListener(service));

        env.reset();

        // Not a bug in itself - it is the contract. It is a bug in anything that assumed
        // subscribing once at startup was enough.
        assertFalse(env.getEventManager().hasListener(service),
                "reset() is expected to clear subscribers; services must re-subscribe");
    }

    @Test
    void resetKeepsTheEntityManagerListening() {
        Env env = new Env();
        // built lazily, and it subscribes itself exactly once when it is
        env.getEntityManager();
        assertTrue(env.getEventManager().hasListener(env.getEntityManager()));

        env.reset();

        assertTrue(env.getEventManager().hasListener(env.getEntityManager()),
                "the environment must re-attach the manager it owns, or spawns and moves "
                        + "stop reaching it for the rest of the process");
    }

    @Test
    void resubscribingIsIdempotent() {
        Env env = new Env();
        Service service = new Service();
        env.getEventManager().subscribe(service);
        env.getEventManager().subscribe(service);

        env.getEventManager().notify_event(new Event());

        // The sensors call init() on every entry to the game mode; a double subscription
        // would deliver every sound twice.
        assertTrue(service.seen == 1, "duplicate subscribe delivered " + service.seen);
    }
}
