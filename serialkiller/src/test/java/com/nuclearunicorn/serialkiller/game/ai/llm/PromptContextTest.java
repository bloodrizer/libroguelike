package com.nuclearunicorn.serialkiller.game.ai.llm;

import com.nuclearunicorn.libroguelike.game.combat.BasicCombat;
import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.serialkiller.game.ai.PedestrianAI;
import com.nuclearunicorn.serialkiller.game.ai.llm.sense.DialogueLog;
import com.nuclearunicorn.serialkiller.game.ai.llm.sense.GameTurn;
import com.nuclearunicorn.serialkiller.game.ai.llm.sense.Relations;
import com.nuclearunicorn.serialkiller.game.ai.llm.sense.Salience;
import com.nuclearunicorn.serialkiller.game.ai.llm.sense.Stimulus;
import com.nuclearunicorn.serialkiller.game.ai.mind.Knowledge;
import com.nuclearunicorn.serialkiller.game.controllers.RLController;
import com.nuclearunicorn.serialkiller.game.world.entities.EntityRLHuman;
import com.nuclearunicorn.serialkiller.game.world.entities.EntityRLPlayer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The prompt has to contain the things an NPC is expected to know.
 *
 * <p>The bug these pin down produced a scene worth quoting in full. The player asked his own
 * wife whether she recognised him; she said "Of course, I recognize you." He asked "who am
 * I?" and she said "I'm sorry, I don't recognize you." That reads as a model failing at a
 * trivial question, and it is not — every route by which a person's name could reach the
 * model replaced the player's with the literal string {@code "the player"}, so the answer she
 * gave was the only honest one available to her. The first exchange worked for the same
 * reason the second failed: "your husband" was in the prompt, "ASHLEY ANDREWS" was not.
 *
 * <p>Asserted on the built prompt rather than on the helpers, because the contract is what
 * the model is handed. A prompt is an interface with a consumer that cannot file a bug.
 */
class PromptContextTest {

    /** A named human, wired enough to be described but with no world to stand in. */
    private static EntityRLHuman human(String name, EntityRLHuman.Sex sex, int age) {
        EntityRLHuman ent = new EntityRLHuman();
        ent.setName(name);
        ent.setSex(sex);
        ent.age = age;
        ent.set_controller(new RLController());
        return ent;
    }

    private static EntityRLPlayer player(String name) {
        EntityRLPlayer ent = new EntityRLPlayer();
        ent.setName(name);
        ent.setSex(EntityRLHuman.Sex.MALE);
        ent.age = 34;
        return ent;
    }

    /** JACINTA and the player, married, as the replay had them. */
    private static EntityRLHuman wife(EntityRLPlayer husband) {
        EntityRLHuman jacinta = human("JACINTA ANDREWS", EntityRLHuman.Sex.FEMALE, 32);
        jacinta.setMate(husband);
        return jacinta;
    }

    private static Perception.Situation situation(EntityRLHuman owner) {
        PedestrianAI brain = new PedestrianAI();
        brain.set_owner(owner);
        owner.set_ai(brain);
        Perception.Situation s = new Perception.Situation();
        s.persona = brain;
        return s;
    }

    private static String prompt(EntityRLHuman owner, Knowledge knowledge, DialogueLog dialogue) {
        return Perception.snapshot(owner, knowledge, dialogue, situation(owner));
    }

    /** The whole of the original bug: she is handed no name for the man in front of her. */
    @Test
    void thePlayerHasAName() {
        String text = prompt(wife(player("ASHLEY ANDREWS")), new Knowledge(), null);

        assertTrue(text.contains("Ashley Andrews"),
                "asked \"who am I?\", an NPC can only answer from a name she was given:\n" + text);
        assertFalse(text.contains("the player"),
                "\"the player\" is a game-engine word, not a person a 3.8B model can reason"
                        + " about:\n" + text);
    }

    /** ...and the tie, so "who am I?" and "do you know me?" are both answerable. */
    @Test
    void theRelationSurvivesAlongsideTheName() {
        String text = prompt(wife(player("ASHLEY ANDREWS")), new Knowledge(), null);
        assertTrue(text.contains("Ashley Andrews (your husband)"),
                "name and relation belong in the same breath:\n" + text);
    }

    /** Family is a standing fact, so it must not depend on anybody having just spoken. */
    @Test
    void theFamilyRosterDoesNotNeedAStimulusToExist() {
        EntityRLPlayer husband = player("ASHLEY ANDREWS");
        EntityRLHuman jacinta = wife(husband);
        EntityRLHuman son = human("BRET ANDREWS", EntityRLHuman.Sex.MALE, 9);
        jacinta.addChild(son);

        String text = prompt(jacinta, new Knowledge(), null);

        assertTrue(text.contains("Your family:"), "no household block at all:\n" + text);
        assertTrue(text.contains("Bret Andrews (your son)"),
                "a child who happens not to be talking is still your child:\n" + text);
    }

    /** In a game about a murderer, a widow who lists her husband as ordinary is a bug. */
    @Test
    void deadKinAreMarkedAsDead() {
        EntityRLPlayer husband = player("ASHLEY ANDREWS");
        EntityRLHuman jacinta = wife(husband);
        EntityRLHuman son = human("BRET ANDREWS", EntityRLHuman.Sex.MALE, 9);
        son.set_combat(new BasicCombat());
        son.get_combat().set_hp(0);
        jacinta.addChild(son);

        String text = prompt(jacinta, new Knowledge(), null);

        assertTrue(text.contains("Bret Andrews (your son) - dead"),
                "the roster reports a dead child as if he were at the table:\n" + text);
    }

    /**
     * The vocative bug: DANIAL MICHAEL, told "Sure, Danial. Come on in.", answered "What's
     * new with you, Danial?" — his own name was in his transcript only inside somebody
     * else's quoted line, so that is the one he reached for.
     */
    @Test
    void theTranscriptNamesItsOwnSpeaker() {
        EntityRLHuman danial = human("DANIAL MICHAEL", EntityRLHuman.Sex.MALE, 41);
        DialogueLog log = new DialogueLog(6);
        log.said(GameTurn.current(), "Mind if I join you for a drink?");
        log.heard(GameTurn.current(), "BARNEY FLYNN", "Sure, Danial. Come on in.", true);

        String text = prompt(danial, new Knowledge(), log);

        assertTrue(text.contains("Danial Michael (you): \"Mind if I join you for a drink?\""),
                "an own turn labelled only \"you\" leaves the speaker's name unbound:\n" + text);
    }

    /**
     * Beliefs reached the reflexes and never the model, so a witness could recognise a killer
     * well enough to run from him and had nothing to say about it. Fed the visible list
     * directly — seeing someone needs a world, believing something about them does not.
     */
    @Test
    void whatYouKnowAboutSomeoneInTheRoomReachesThePrompt() {
        EntityRLHuman witness = human("VERGIE NOLAN", EntityRLHuman.Sex.FEMALE, 28);
        EntityRLPlayer killer = player("ASHLEY ANDREWS");

        Knowledge knowledge = new Knowledge();
        knowledge.learnedOfCrime(killer.get_uid(), null, GameTurn.current(), true);

        StringBuilder sb = new StringBuilder();
        List<Entity> visible = new ArrayList<Entity>(Collections.<Entity>singletonList(killer));
        Perception.appendBeliefs(sb, witness, knowledge, visible);

        assertTrue(sb.toString().contains("Ashley Andrews"),
                "the one fact that should change how she speaks to him is missing:\n" + sb);
        assertTrue(sb.toString().contains("committed a crime"),
                "the belief is held but never stated:\n" + sb);
    }

    /**
     * Names are stored and drawn in caps; a prompt is prose. A model handed "DANIAL MICHAEL"
     * hands it straight back — an NPC introduced himself in a text bubble as "Hello Barney,
     * it's DANIAL." Display keeps the caps, the model's copy does not.
     */
    @Test
    void namesReachTheModelAsProseNotAsShouting() {
        String text = prompt(wife(player("ASHLEY ANDREWS")), new Knowledge(), null);
        assertFalse(text.contains("ASHLEY ANDREWS"), "the model was handed a name in caps:\n" + text);
    }

    /** A stranger is still a stranger: no invented ties, no roster line. */
    @Test
    void anUnrelatedPersonIsJustANamedPerson() {
        EntityRLHuman stranger = human("BARNEY FLYNN", EntityRLHuman.Sex.MALE, 50);
        EntityRLHuman jacinta = wife(player("ASHLEY ANDREWS"));

        assertTrue("Barney Flynn".equals(Relations.tagged(jacinta, stranger)),
                "a stranger acquired a relation: " + Relations.tagged(jacinta, stranger));
        assertTrue("Barney Flynn".equals(Relations.describe(jacinta, stranger)),
                "a stranger acquired a relation: " + Relations.describe(jacinta, stranger));
    }

    /** Nothing above may cost the urgent block its place at the top of the reply. */
    @Test
    void theDirectedStimulusStillLeadsTheReply() {
        EntityRLPlayer husband = player("ASHLEY ANDREWS");
        EntityRLHuman jacinta = wife(husband);

        Knowledge knowledge = new Knowledge();
        knowledge.record(new Stimulus(GameTurn.current(), Stimulus.Channel.SPEECH,
                Salience.DIRECTED, husband.get_uid(),
                "your husband Ashley Andrews is talking to you and just said: \"who am I?\""));

        String text = prompt(jacinta, knowledge, null);

        assertTrue(text.contains("RIGHT NOW:"), "the live signal lost its own block:\n" + text);
        assertTrue(text.indexOf("RIGHT NOW:") > text.indexOf("Your family:"),
                "standing facts belong above the thing being reacted to:\n" + text);
    }
}
