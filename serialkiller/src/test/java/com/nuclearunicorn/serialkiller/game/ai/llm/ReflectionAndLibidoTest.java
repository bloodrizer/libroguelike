package com.nuclearunicorn.serialkiller.game.ai.llm;

import com.nuclearunicorn.serialkiller.game.ai.PedestrianAI;
import com.nuclearunicorn.serialkiller.game.ai.llm.sense.DialogueLog;
import com.nuclearunicorn.serialkiller.game.ai.mind.Knowledge;
import com.nuclearunicorn.serialkiller.game.world.entities.EntityRLHuman;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The two newest prompt surfaces: the libido condition (a standing fact about the body) and
 * the reflection tier's durable beliefs. Both must reach the model, because the reactor's
 * reply is built from what the NPC <i>is</i> and has come to <i>believe</i>, not only from
 * what it has just sensed.
 */
class ReflectionAndLibidoTest {

    private static EntityRLHuman human(int age) {
        EntityRLHuman ent = new EntityRLHuman();
        ent.setName("MABEL FARR");
        ent.setSex(EntityRLHuman.Sex.FEMALE);
        ent.age = age;
        return ent;
    }

    private static Perception.Situation situation(EntityRLHuman owner) {
        PedestrianAI brain = new PedestrianAI();
        brain.set_owner(owner);
        owner.set_ai(brain);
        Perception.Situation s = new Perception.Situation();
        s.persona = brain;
        return s;
    }

    private static String prompt(EntityRLHuman owner, Knowledge knowledge) {
        return Perception.snapshot(owner, knowledge, (DialogueLog) null, situation(owner));
    }

    /** A body in the mood says so, in the same standing-fact block as being wounded. */
    @Test
    void libidoIsAConditionNotAnEvent() {
        EntityRLHuman owner = human(30);
        owner.getBodysim().setAttribute("libido", 80f);

        assertTrue(prompt(owner, new Knowledge()).contains("in the mood"),
                "an aroused NPC is a condition the model must be able to talk about:\n"
                        + prompt(owner, new Knowledge()));
    }

    /** A body not in the mood says nothing — the line must not fire for everyone. */
    @Test
    void lowLibidoIsSilent() {
        EntityRLHuman owner = human(30);   // libido defaults to 0
        assertTrue(!prompt(owner, new Knowledge()).contains("in the mood"),
                "a stranger at zero libido is not \"in the mood\"");
    }

    /** A reflection is durable, so it reaches the reactor prompt without any fresh stimulus. */
    @Test
    void reflectionsReachThePrompt() {
        Knowledge knowledge = new Knowledge();
        knowledge.addReflection("I suspect the player of something.");

        String text = prompt(human(30), knowledge);
        assertTrue(text.contains("What you have come to believe:"),
                "no belief block at all:\n" + text);
        assertTrue(text.contains("I suspect the player of something."),
                "the belief is held but never stated:\n" + text);
    }

    /** Beliefs neither repeat nor accumulate forever: dedupe, then cap. */
    @Test
    void reflectionsAreDedupedAndCapped() {
        Knowledge knowledge = new Knowledge();
        knowledge.addReflection("a");
        knowledge.addReflection("a");
        knowledge.addReflection("b");
        knowledge.addReflection("c");
        knowledge.addReflection("d");

        assertEquals(3, knowledge.reflections().size(), "reflections must be capped");
        assertEquals("b", knowledge.reflections().get(0), "the oldest belief is dropped first");
        assertEquals("d", knowledge.reflections().get(2), "the newest belief is kept");
    }
}
