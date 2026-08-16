package com.nuclearunicorn.serialkiller.game.ai;

import com.nuclearunicorn.serialkiller.game.ai.behavior.SleepAction;
import com.nuclearunicorn.serialkiller.game.ai.llm.sense.Salience;
import com.nuclearunicorn.serialkiller.game.ai.llm.sense.Stimulus;
import com.nuclearunicorn.serialkiller.game.controllers.RLController;
import com.nuclearunicorn.serialkiller.game.world.entities.EntityRLHuman;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A sleeping NPC does not talk, listen or act (§D2 of INVARIANTS.md).
 *
 * <p>The bug this pins down was a priority inversion, and it is worth naming because it read
 * as a prompt problem for a long time. The planner impulse outranked bedtime, so every
 * completion that landed on a sleeper won the body for the single turn it took to say a
 * line, and the log then showed {@code SLEEPING} on the turns either side of it. Two people
 * asleep in the same room fed each other: the line woke the hearing sensor for everyone in
 * earshot, which submitted a reply, which was another line. A replay caught a dormitory
 * holding a "nightly catch-up" with all four of them in bed.
 *
 * <p>In the same package as {@link PedestrianAI} on purpose — the priorities it registers
 * are protected, because they are for subclasses and for this.
 */
class SleepIsQuietTest {

    private static final long TURN = 100;

    /** A pedestrian wired to a bare human, no world and no inference. */
    private static PedestrianAI sleeper() {
        EntityRLHuman human = new EntityRLHuman();
        human.set_controller(new RLController());
        PedestrianAI brain = new PedestrianAI();
        brain.set_owner(human);
        human.set_ai(brain);
        brain.setState(SleepAction.STATE);
        return brain;
    }

    private static Stimulus speech(String text) {
        return new Stimulus(TURN, Stimulus.Channel.SPEECH, Salience.DIRECTED, "speaker-uid", text);
    }

    /**
     * The inversion itself. Sleep has to beat the planner or a completion arriving at 3am is
     * enough to sit an NPC up and have it deliver the line.
     */
    @Test
    void bedOutranksThePlanner() {
        assertTrue(PedestrianAI.PRIORITY_SLEEP > PedestrianAI.PRIORITY_PLAN,
                "a plan that lands while its author is in bed must not win the body");
    }

    /** ...but not violence. Being attacked is what gets a person out of bed. */
    @Test
    void violenceStillOutranksBed() {
        assertTrue(PedestrianAI.PRIORITY_THREAT > PedestrianAI.PRIORITY_SLEEP,
                "you wake someone up by hurting them - the flee reflex has to win");
    }

    /** The commute stays below the planner: a town with inference on should not go to bed at dusk. */
    @Test
    void theWalkHomeIsStillInterruptible() {
        assertTrue(PedestrianAI.PRIORITY_PLAN > PedestrianAI.PRIORITY_NIGHT,
                "walking home is something you can be talked out of; being asleep is not");
    }

    @Test
    void aSleeperDoesNotTakeInSpeech() {
        PedestrianAI brain = sleeper();
        brain.sense(speech("the player is talking to you and just said: \"sup folks\""));
        assertTrue(brain.knowledge().stream().isEmpty(),
                "speech across a dark bedroom is not a stimulus to reply to");
    }

    @Test
    void aSleeperKeepsNoTranscript() {
        PedestrianAI brain = sleeper();
        brain.hear("the player", "sup folks", true);
        assertTrue(brain.voice().log().isEmpty(),
                "a conversation you slept through is not one you can quote in the morning");
    }

    /** The one channel that reaches a sleeper, and the reason the flee reflex still works. */
    @Test
    void painGetsThrough() {
        PedestrianAI brain = sleeper();
        brain.sense(new Stimulus(TURN, Stimulus.Channel.PAIN, Salience.URGENT, "attacker-uid",
                "the player just attacked you!"));
        assertFalse(brain.knowledge().stream().isEmpty(), "being stabbed wakes you");
        assertNotNull(brain.knowledge().threat(), "and the threat is what flee reads");
    }

    /** Awake, nothing is filtered — the gate is the state, not a new rule about speech. */
    @Test
    void awakeIsUnchanged() {
        PedestrianAI brain = sleeper();
        brain.setState(PedestrianAI.AI_STATE_IDLE);
        assertFalse(brain.isAsleep());

        brain.sense(speech("the player is talking to you"));
        brain.hear("the player", "sup folks", true);
        assertFalse(brain.knowledge().stream().isEmpty());
        assertFalse(brain.voice().log().isEmpty());
    }

    /** Police never register the impulse, so nothing above can ever mute one. */
    @Test
    void policemenAreNeverAsleep() {
        EntityRLHuman human = new EntityRLHuman();
        human.set_controller(new RLController());
        PoliceAI brain = new PoliceAI();
        brain.set_owner(human);
        human.set_ai(brain);

        brain.update();
        assertFalse(brain.isAsleep(), "an officer on duty has no bedtime impulse to select");
        assertNotEquals(SleepAction.STATE, brain.getState());
    }
}
