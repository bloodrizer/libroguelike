package com.nuclearunicorn.serialkiller.game.ai.behavior;

import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.serialkiller.game.world.entities.EntityDoor;
import com.nuclearunicorn.serialkiller.game.world.entities.EntityFurniture;
import com.nuclearunicorn.serialkiller.game.world.entities.EntityRLHuman;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Who breaks what, and when (§3 of INVARIANTS.md).
 *
 * <p>The town used to lose sixty-odd windows a night to people walking home, because a
 * blocked step was treated as an intention: bump the furniture, break the furniture. It is
 * not an intention, it is a stale route. The only behaviour that may go <i>through</i>
 * something is the one running for its life, and even then not through a person.
 *
 * <p>Reaching that state in a live session takes a chase into a dead end, which is rare on
 * purpose — so the decision is tested here rather than waited for.
 */
class ObstacleResponseTest {

    @Test
    void panicGoesThroughFurniture() {
        assertTrue(FleeAction.breaksThrough(new EntityFurniture()),
                "cornered, a person goes through the window rather than back into the room");
    }

    @Test
    void panicDoesNotBreakDoors() {
        assertFalse(FleeAction.breaksThrough(new EntityDoor()),
                "a door opens - there is nothing to break");
    }

    @Test
    void panicDoesNotAttackPeople() {
        assertFalse(FleeAction.breaksThrough(new EntityRLHuman()),
                "shoving past a bystander is not the same as escaping through them");
    }

    @Test
    void panicIgnoresPlainScenery() {
        Entity grass = new Entity();
        assertFalse(FleeAction.breaksThrough(grass));
    }

    @Test
    void nothingInTheWayIsNothingToBreak() {
        assertFalse(FleeAction.breaksThrough(null));
    }
}
