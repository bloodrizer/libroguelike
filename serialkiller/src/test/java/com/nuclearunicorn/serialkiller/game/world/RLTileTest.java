package com.nuclearunicorn.serialkiller.game.world;

import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.game.ent.controller.IEntityController;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The distinction the pathfinder got wrong for years: <i>blocked</i> and <i>blocked for
 * routing purposes</i> are different questions.
 *
 * <p>Answering "is this tile impassable" with {@code isWall()} alone made every window a
 * legal one-tile shortcut into a house, so A* routed residents through the glass on purpose
 * and the obstacle handler broke it to make the step legal. Answering it with
 * {@code isBlocked()} instead makes people into masonry, so one neighbour standing in a
 * doorway seals a house and everybody behind him goes looking for another way in.
 *
 * <p>{@link RLTile#isPathBlocked()} is the third answer, and these are its rules.
 */
class RLTileTest {

    /** A controller is what marks an entity as something that moves off on its own. */
    private static class DummyController implements IEntityController {
        public void attach(Entity entity) { }
        public void think() { }
    }

    private static Entity prop(boolean blocking) {
        Entity ent = new Entity();
        ent.set_blocking(blocking);
        return ent;
    }

    private static Entity person() {
        Entity ent = new Entity();
        ent.set_blocking(true);
        ent.controller = new DummyController();
        return ent;
    }

    @Test
    void emptyGroundIsPassable() {
        RLTile tile = new RLTile();
        assertFalse(tile.isPathBlocked());
        assertFalse(tile.isBlocked());
    }

    @Test
    void wallIsImpassableToEverything() {
        RLTile tile = new RLTile();
        tile.setWall(true);
        assertTrue(tile.isPathBlocked());
        assertTrue(tile.isBlocked());
    }

    /** A window: not a wall, solid, and nothing is going to move it out of the way. */
    @Test
    void solidPropBlocksARoute() {
        RLTile tile = new RLTile();
        tile.setWallGap(true);            // a window sits in a hole punched in a wall
        tile.add_entity(prop(true));
        assertTrue(tile.isPathBlocked(), "a route through a window is a broken window");
    }

    @Test
    void nonBlockingPropDoesNotBlockARoute() {
        RLTile tile = new RLTile();
        tile.add_entity(prop(false));     // an open door, a bed, grass
        assertFalse(tile.isPathBlocked());
    }

    /** The rule that keeps a doorway from becoming a wall the moment someone stands in it. */
    @Test
    void personDoesNotBlockARoute() {
        RLTile tile = new RLTile();
        tile.add_entity(person());
        assertFalse(tile.isPathBlocked(), "people walk on; a route may be planned through them");
        assertTrue(tile.isBlocked(), "the step itself still has to wait for them");
    }

    /** Both on one tile — someone standing next to the fridge. The fridge decides. */
    @Test
    void personStandingOnSolidGroundStillBlocksARoute() {
        RLTile tile = new RLTile();
        tile.add_entity(person());
        tile.add_entity(prop(true));
        assertTrue(tile.isPathBlocked());
    }

    /** A broken window stops blocking: EntityFurniture.die() clears the flag. */
    @Test
    void brokenPropStopsBlocking() {
        RLTile tile = new RLTile();
        Entity window = prop(true);
        tile.add_entity(window);
        assertTrue(tile.isPathBlocked());

        window.set_blocking(false);
        assertFalse(tile.isPathBlocked());
    }
}
