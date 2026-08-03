package com.nuclearunicorn.serialkiller.game.ai.llm.command;

import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.game.ent.EntityActor;
import com.nuclearunicorn.serialkiller.game.ai.llm.LlmDebug;
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

    public void say(String text) {
        LlmDebug.log("  %s says: \"%s\"", owner.get_uid(), text);
        ((EntityActor) owner).say_message(text);
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
