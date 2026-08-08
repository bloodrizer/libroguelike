package com.nuclearunicorn.serialkiller.game.ai.llm.command;

import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.game.ent.EntityActor;
import com.nuclearunicorn.serialkiller.game.ai.LLMAgentAI;
import com.nuclearunicorn.serialkiller.game.ai.llm.LlmDebug;
import com.nuclearunicorn.serialkiller.game.ai.llm.LlmRuntime;
import com.nuclearunicorn.serialkiller.game.controllers.RLController;
import com.nuclearunicorn.serialkiller.game.world.RLWorldChunk;
import com.nuclearunicorn.serialkiller.game.world.entities.EntityRLHuman;
import com.nuclearunicorn.serialkiller.generators.Apartment;
import org.lwjgl.util.Point;

import java.util.List;
import java.util.Random;

/**
 * The command "tool surface" (§4): the game-thread-only handle a command uses to act —
 * movement via the controller, speech, and symbol→Point resolution over the milestone
 * nav mesh. Commands never touch worker threads or raw coordinates from the model.
 */
public class AgentContext {

    private final EntityRLHuman owner;
    private final RLController controller;
    private final Random rng = new Random();

    public AgentContext(EntityRLHuman owner, RLController controller) {
        this.owner = owner;
        this.controller = controller;
    }

    public EntityRLHuman owner() {
        return owner;
    }

    public RLController controller() {
        return controller;
    }

    /**
     * Speak, and remember having spoken (§8). Every line goes through here, so this is where
     * the two things a model gets wrong about dialogue are caught: length, and repetition.
     */
    public void say(String text) {
        String line = clip(text, LlmRuntime.config().speech.maxSayChars);
        if (line.isEmpty()) {
            return;
        }
        LLMAgentAI brain = owner.getAI() instanceof LLMAgentAI ? (LLMAgentAI) owner.getAI() : null;
        if (brain != null && brain.alreadySaid(line)) {
            LlmDebug.log("  %s: suppressed repeat \"%s\"", owner.get_uid(), line);
            return;
        }
        LlmDebug.log("  %s says: \"%s\"", owner.get_uid(), line);
        ((EntityActor) owner).say_message(line);
        if (brain != null) {
            brain.spoke(line);   // without this, the next re-plan opens cold on the same scene
        }
    }

    /**
     * Cut a line back to {@code limit} characters, preferring a sentence boundary and then a
     * word boundary. NPCs speak in a floating bubble over their head, and a model asked for
     * "short sentences" still returns paragraphs — the instruction is advice, this is the rule.
     */
    static String clip(String text, int limit) {
        if (text == null) {
            return "";
        }
        String line = text.trim();
        if (limit <= 0 || line.length() <= limit) {
            return line;
        }
        String head = line.substring(0, limit);
        int sentence = Math.max(head.lastIndexOf('.'), Math.max(head.lastIndexOf('!'), head.lastIndexOf('?')));
        if (sentence >= limit / 2) {
            return head.substring(0, sentence + 1).trim();
        }
        // Ellipsis has to fit inside the budget, not be added on top of it.
        String body = head.substring(0, Math.max(1, limit - 3));
        int word = body.lastIndexOf(' ');
        return (word >= limit / 2 ? body.substring(0, word) : body).trim() + "...";
    }

    /**
     * Resolve a symbolic target to a world Point (§6). Keywords: {@code home} (own
     * apartment), {@code random} (a random milestone). Otherwise treat the symbol as an
     * entity uid or name and use its origin. Returns null if unresolvable — the goto
     * command then reports FAILURE.
     */
    public Point resolve(String symbol) {
        if (symbol == null) {
            return null;
        }

        RLWorldChunk chunk = (RLWorldChunk) owner.get_chunk();

        Point resolved;
        switch (symbol) {
            case "home": {
                Apartment apt = owner.getApartment();
                resolved = apt != null ? new Point(chunk.getNearestMilestone(apt)) : null;
                break;
            }
            case "random": {
                List<Point> milestones = chunk.getMilestones();
                resolved = milestones.isEmpty()
                        ? null : new Point(milestones.get(rng.nextInt(milestones.size())));
                break;
            }
            default: {
                Entity ent = owner.getEnvironment().getEntityManager()
                        .get_entity(symbol, owner.getLayerId());
                resolved = ent != null ? new Point(ent.origin) : null;
            }
        }
        LlmDebug.log("  resolve('%s') -> %s", symbol,
                resolved != null ? resolved.getX() + "," + resolved.getY() : "UNRESOLVED");
        return resolved;
    }
}
