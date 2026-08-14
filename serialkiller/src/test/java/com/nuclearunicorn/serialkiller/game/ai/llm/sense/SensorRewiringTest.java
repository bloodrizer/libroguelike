package com.nuclearunicorn.serialkiller.game.ai.llm.sense;

import com.nuclearunicorn.libroguelike.core.client.ClientGameEnvironment;
import com.nuclearunicorn.libroguelike.events.IEventListener;
import com.nuclearunicorn.serialkiller.game.social.SocialController;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every sensor has to survive "New game".
 *
 * <p>The bug this pins: {@code ClientGameEnvironment.reset()} empties the event manager's
 * listener list, and each sensor's {@code init()} used to return early when its static
 * instance already existed. So on the second town of a session the sensors were constructed,
 * logged "subscribed", and received nothing ever again — NPCs could not hear the player
 * speak, could not hear each other, and did not react to being hit. No exception, no warning,
 * and no way to tell it apart from "the AI is just ignoring me".
 *
 * <p>Asserted through the real {@code init()} entry points rather than by inspecting the
 * guard, because the guard is not the contract — being subscribed afterwards is.
 */
class SensorRewiringTest {

    @Test
    void sensorsResubscribeAfterAWorldReset() {
        initAll();
        assertSubscribed("before reset");

        ClientGameEnvironment.reset();      // what the "New game" button does

        initAll();                          // what entering the game mode does
        assertSubscribed("after reset");
    }

    @Test
    void repeatedInitDoesNotDoubleSubscribe() {
        initAll();
        int before = listenerCount();
        initAll();
        assertTrue(listenerCount() == before,
                "init() twice added " + (listenerCount() - before) + " listener(s); "
                        + "every sound would be delivered more than once");
    }

    private static void initAll() {
        HearingSensor.init();
        CrimeSensor.init();
        SocialController.init();
    }

    private static void assertSubscribed(String when) {
        assertTrue(hasListenerOfType(HearingSensor.class), "hearing sensor deaf " + when);
        assertTrue(hasListenerOfType(CrimeSensor.class), "crime sensor blind " + when);
        assertTrue(hasListenerOfType(SocialController.class), "no police radio " + when);
    }

    /** The managers expose membership, not the list, so ask by type. */
    private static boolean hasListenerOfType(Class<?> type) {
        for (IEventListener listener : listeners()) {
            if (type.isInstance(listener)) {
                return true;
            }
        }
        return false;
    }

    private static int listenerCount() {
        int n = 0;
        for (IEventListener listener : listeners()) {
            if (listener instanceof HearingSensor || listener instanceof CrimeSensor
                    || listener instanceof SocialController) {
                n++;
            }
        }
        return n;
    }

    private static java.util.List<IEventListener> listeners() {
        try {
            java.lang.reflect.Field f =
                    com.nuclearunicorn.libroguelike.events.EventManager.class
                            .getDeclaredField("listeners");
            f.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.List<IEventListener> list = (java.util.List<IEventListener>)
                    f.get(ClientGameEnvironment.getEnvironment().getEventManager());
            return list;
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("cannot read the listener list", e);
        }
    }
}
