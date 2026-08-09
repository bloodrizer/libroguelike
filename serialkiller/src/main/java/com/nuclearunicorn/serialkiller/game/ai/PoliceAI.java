package com.nuclearunicorn.serialkiller.game.ai;

import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.utils.Fov;
import com.nuclearunicorn.serialkiller.game.ai.behavior.InvestigateAction;
import com.nuclearunicorn.serialkiller.game.ai.behavior.PursueAction;
import com.nuclearunicorn.serialkiller.game.ai.llm.Perception;
import com.nuclearunicorn.serialkiller.game.ai.llm.sense.GameTurn;
import com.nuclearunicorn.serialkiller.game.ai.llm.sense.Salience;
import com.nuclearunicorn.serialkiller.game.ai.llm.sense.Stimulus;
import com.nuclearunicorn.serialkiller.game.ai.mind.Percept;
import com.nuclearunicorn.serialkiller.game.combat.RLCombat;
import com.nuclearunicorn.serialkiller.game.events.NPCReportCrimeEvent;
import com.nuclearunicorn.serialkiller.game.social.SocialController;
import com.nuclearunicorn.serialkiller.game.world.entities.EntityRLHuman;

/**
 * A police officer: the same person as a {@link PedestrianAI} with three differences, each
 * of them a line in the constructor rather than a branch somewhere downstream.
 *
 * <ol>
 *   <li>He does not run from a criminal — the flee impulse is removed outright.</li>
 *   <li>He chases and arrests one, at the priority a civilian gives to fleeing.</li>
 *   <li>He attends crimes he only heard about, which is nearly all of them.</li>
 * </ol>
 *
 * <p>This is what the {@code Role} enum was standing in for, and why it did not work.
 * Deciding "is this NPC a policeman" at four separate call sites — the prompt, the pain
 * sensor, the crime handler, the reflex — meant four chances to disagree, and they did: with
 * inference on, the spawn site handed policemen the civilian brain, so the uniform, the vest
 * and the stunstick were all still there while the behaviour behind them was a pedestrian's.
 * A witness to a murder said "I'm taking him into custody" and then fled from the murderer.
 */
public class PoliceAI extends PedestrianAI {

    public PoliceAI() {
        super();

        registerState(PursueAction.STATE, new PursueAction(this));
        registerState(InvestigateAction.STATE, new InvestigateAction(this));

        // An officer does not flee, and does not go home at dusk. Removing the inherited
        // impulses says that once, here, instead of guarding every behaviour with a role test.
        removeImpulse("threat");
        removeImpulse("night");

        registerImpulse(PRIORITY_SUSPECT, new PursueAction.Trigger(this));
        registerImpulse(PRIORITY_CRIME_SCENE, new InvestigateAction.Trigger(this));
    }

    @Override
    public void update() {
        if (ensureWired()) {
            spotWantedPeople();
        }
        super.update();
    }

    /**
     * Look around for anyone we know is wanted. This is what upgrades a radio call into an
     * arrest: a report names a suspect but only as hearsay, and {@link PursueAction} refuses
     * to chase hearsay — otherwise an officer across town sprints unerringly at a player he
     * has no way of having seen. Getting eyes on them is what makes the chase legitimate.
     *
     * <p>Deliberately the officer's own field of view, not {@code RLTile.isVisible()}. That
     * flag is the <i>player's</i> lighting-and-FOV mask, so the old code's idea of a witness
     * was "an entity standing somewhere the player can see" — which is how a crate ended up
     * witnessing crimes, and how an officer in an unlit street witnessed nothing at all.
     */
    private void spotWantedPeople() {
        int radius = human().get_combat() instanceof RLCombat
                ? ((RLCombat) human().get_combat()).getFovRadius() : 8;
        long now = GameTurn.current();
        for (Percept percept : knowledge().beliefs().values()) {
            if (percept.crimeTurn < 0 || percept.source == Percept.Source.SEEN) {
                continue;
            }
            Entity wanted = entity(percept.uid);
            if (wanted == null || wanted.origin == null || !isAlive(wanted)) {
                continue;
            }
            if (Fov.in_range(human().origin, wanted.origin, radius)) {
                knowledge().saw(wanted, now);
                sense(new Stimulus(now, Stimulus.Channel.SIGHT, Salience.URGENT, percept.uid,
                        "you have spotted the person wanted for the crime you were sent to"));
            }
        }
    }

    /**
     * Being assaulted on duty is a crime with a named suspect and an eyewitness, so it goes
     * in as one. The civilian handling — record a threat, run — is deliberately not reached:
     * an officer who flees the person hitting him has stopped being an officer.
     */
    @Override
    public void sense(Stimulus stimulus) {
        super.sense(stimulus);
        if (stimulus.channel == Stimulus.Channel.PAIN && stimulus.sourceUid != null) {
            knowledge().learnedOfCrime(stimulus.sourceUid, human().origin, stimulus.turn, true);
        }
    }

    /** A crime in front of an officer is an emergency, not something to mention later. */
    @Override
    public int witnessSalience() {
        return Salience.URGENT;
    }

    /**
     * Dispatch. Everything an officer knows about a crime he did not see arrives here, and
     * before {@link SocialController} started routing these he learned nothing at all — the
     * report was a {@code PointBasedEvent}, and the LLM brain dropped point-based events
     * that landed outside its ten-tile earshot. A police radio with a ten-tile range is a
     * shout.
     */
    @Override
    protected void onCrimeReport(NPCReportCrimeEvent report) {
        String criminal = report.criminal == null ? null : report.criminal.get_uid();
        if (criminal != null && criminal.equals(human().get_uid())) {
            return;
        }
        knowledge().learnedOfCrime(criminal, report.getOrigin(), GameTurn.current(), false);
        sense(new Stimulus(GameTurn.current(), Stimulus.Channel.EVENT, Salience.NOTABLE, criminal,
                criminal == null
                        ? "a crime has been reported nearby and you are on your way to look"
                        : "a crime has been reported and you know who is wanted for it"));
    }

    /**
     * Who he is, told to the model in the same file as what he does about it. He used to be
     * introduced as "a 34-year-old human male living in this town" — no uniform, no duty, no
     * authority — so the model quite reasonably played him as a bystander.
     */
    @Override
    public void describeSelf(StringBuilder sb, EntityRLHuman self) {
        Perception.appendIdentity(sb, self);
        sb.append(" and a police officer on duty in this town.\n")
          .append("You are armed and trained. Your job is to protect people, to stop")
          .append(" crimes you witness, and to arrest whoever commits them. You do not")
          .append(" run from a criminal.\n");
    }
}
