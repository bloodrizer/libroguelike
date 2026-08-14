package com.nuclearunicorn.serialkiller.game.ai.llm.sense;

import com.nuclearunicorn.libroguelike.core.client.ClientGameEnvironment;
import com.nuclearunicorn.libroguelike.events.ETakeDamage;
import com.nuclearunicorn.libroguelike.events.Event;
import com.nuclearunicorn.libroguelike.events.IEventListener;
import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.utils.Fov;
import com.nuclearunicorn.serialkiller.game.ai.TownAI;
import com.nuclearunicorn.serialkiller.game.ai.llm.LlmDebug;
import com.nuclearunicorn.serialkiller.game.ai.llm.LlmRuntime;

/**
 * The pain sensor (§8): turns {@code ETakeDamage} into a {@link Salience#URGENT} stimulus
 * for the victim, and a {@link Salience#NOTABLE} one for anyone who watched.
 *
 * <p>{@code ETakeDamage} existed only as a client-side FX cue — the damage number floating
 * over a head. Nothing routed it to the victim's brain, so {@link Stimulus.Channel#PAIN}
 * had no producer at all and being punched in the face was, to an LLM NPC, silence. The
 * only thing that did fire was a suspicious-noise event arriving as <i>"something happened
 * nearby"</i> at AMBIENT, which is not a reason to stop walking.
 *
 * <p>Being attacked is the one signal that must outrank an in-flight plan unconditionally,
 * so it enters above {@code priority.interruptAt} and carries the attacker's uid — the
 * victim then holds attention on whoever hit them rather than replying into the air.
 */
public class PainSensor implements IEventListener {

    private static PainSensor instance;

    public static void init() {
        if (instance != null) {
            return;
        }
        instance = new PainSensor();
        ClientGameEnvironment.getEnvironment().getEventManager().subscribe(instance);
        LlmDebug.log("pain sensor subscribed (witness radius %d)",
                LlmRuntime.config().speech.earshotRadius);
    }

    @Override
    public void e_on_event(Event event) {
        // Not gated on inference: the flee reflex is driven by what this writes into
        // Knowledge, and a pedestrian runs from her attacker with the model off too.
        if (!(event instanceof ETakeDamage)) {
            return;
        }
        ETakeDamage hit = (ETakeDamage) event;
        Entity victim = hit.ent;
        if (victim == null || hit.dmg == null) {
            return;
        }

        Entity attacker = hit.dmg.inflictor;
        String attackerUid = attacker == null ? null : attacker.get_uid();

        if (victim.getAI() instanceof TownAI) {
            ((TownAI) victim.getAI()).sense(new Stimulus(
                    GameTurn.current(), Stimulus.Channel.PAIN, Salience.URGENT, attackerUid,
                    Relations.describe(victim, attacker) + " just attacked you! You are hurt ("
                            + hit.dmg.amt + " damage) and in immediate danger."));
        }

        // Witnesses: a beating in the street is the strongest social signal the game has,
        // and it used to reach NPCs as an anonymous crime report broadcast town-wide. Only
        // for violence against *people* though - every swing at a crate or a door also
        // raises ETakeDamage, and "you saw your husband attack crate" is not a social event.
        if (!(victim instanceof com.nuclearunicorn.serialkiller.game.world.entities.EntityRLHuman)) {
            return;
        }

        int radius = LlmRuntime.config().speech.earshotRadius;
        Entity[] nearby = Fov.get_entity_in_radius(
                ClientGameEnvironment.getEnvironment().getEntityManager(),
                victim.origin, radius, victim.getLayerId());

        for (Entity witness : nearby) {
            if (witness == victim || witness == attacker || !(witness.getAI() instanceof TownAI)) {
                continue;
            }
            // Watching a stranger get hit and watching your own child get hit should not
            // arrive as the same sentence, so both parties are named from the witness's side.
            // How hard it lands is the brain's own answer: family and duty both make it an
            // emergency, and only the brain knows which of those it has.
            TownAI brain = (TownAI) witness.getAI();
            boolean kin = Relations.relation(witness, victim) != null;
            brain.sense(new Stimulus(
                    GameTurn.current(), Stimulus.Channel.SIGHT,
                    kin ? Salience.URGENT : brain.witnessSalience(), attackerUid,
                    "you just saw " + Relations.describe(witness, attacker) + " attack "
                            + Relations.describe(witness, victim) + " right in front of you"));
        }
    }
}
