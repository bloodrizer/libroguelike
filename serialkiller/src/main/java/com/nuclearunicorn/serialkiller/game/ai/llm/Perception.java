package com.nuclearunicorn.serialkiller.game.ai.llm;

import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.game.player.Player;
import com.nuclearunicorn.libroguelike.game.world.WorldTimer;
import com.nuclearunicorn.libroguelike.utils.Fov;
import com.nuclearunicorn.serialkiller.game.world.entities.EntityRLHuman;

import java.util.List;

/**
 * Builds the reactor prompt on the game thread (§3): a compact, human-readable snapshot
 * of who the NPC is, the time of day, who is nearby, recent memory, and the instruction
 * to emit a JSON command program. The result is an immutable String — the worker thread
 * sees only this, never live game objects.
 */
public final class Perception {

    private static final int NEARBY_RADIUS = 8;

    private Perception() {}

    public static String snapshot(EntityRLHuman owner, List<String> observations) {
        StringBuilder sb = new StringBuilder(512);

        sb.append("You are ").append(owner.getName())
          .append(", a ").append(owner.age).append("-year-old ")
          .append(owner.race.diplayName()).append(" ")
          .append(owner.getSex().toString().toLowerCase())
          .append(" living in this town.\n");

        sb.append("Time: ").append(WorldTimer.is_night() ? "night" : "day").append(".\n");

        appendNearby(sb, owner);

        if (observations != null && !observations.isEmpty()) {
            sb.append("Recent memory:\n");
            for (String obs : observations) {
                sb.append("- ").append(obs).append("\n");
            }
        }

        sb.append("\nDecide what to do next. Reply ONLY with a JSON array of commands. Speek in short sentences.\n");
        sb.append("Commands: {\"verb\":\"goto\",\"target\":\"<home|random|uid>\"}, ");
        sb.append("{\"verb\":\"say\",\"text\":\"<line>\"}, ");
        sb.append("{\"verb\":\"wait\",\"ticks\":<n>}.\n");

        return sb.toString();
    }

    private static void appendNearby(StringBuilder sb, EntityRLHuman owner) {
        Entity[] ents = Fov.get_entity_in_radius(
                owner.getEnvironment().getEntityManager(),
                owner.origin, NEARBY_RADIUS, owner.getLayerId());

        boolean any = false;
        for (Entity ent : ents) {
            if (ent == owner) {
                continue;
            }
            if (ent.isPlayerEnt() || ent instanceof EntityRLHuman) {
                if (!any) {
                    sb.append("Nearby: ");
                    any = true;
                } else {
                    sb.append(", ");
                }
                sb.append(ent.isPlayerEnt() ? "the player" : ent.getName());
            }
        }
        if (any) {
            sb.append(".\n");
        }

        if (Player.get_ent() != null && Fov.in_range(owner.origin, Player.get_origin(), NEARBY_RADIUS)) {
            sb.append("The player is watching you.\n");
        }
    }
}
