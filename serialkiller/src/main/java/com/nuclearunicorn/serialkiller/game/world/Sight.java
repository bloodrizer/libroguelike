package com.nuclearunicorn.serialkiller.game.world;

import com.nuclearunicorn.libroguelike.core.client.ClientGameEnvironment;
import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.game.world.WorldTile;
import com.nuclearunicorn.libroguelike.game.world.layers.WorldLayer;
import com.nuclearunicorn.serialkiller.game.world.entities.EntityRLActor;
import rlforj.los.BresLos;
import rlforj.los.ILosBoard;

/**
 * Whether one person can see another: a ray cast against the tiles between them.
 *
 * <p>Everything in the codebase that wanted this asked {@code Fov.in_range} instead, which
 * despite the class name is a plain squared-distance test — {@code Fov} contains no field of
 * view at all. So "who is near me" meant "who is within eight tiles", counting the people on
 * the far side of your bedroom wall, which is how an NPC ended up greeting someone standing
 * in the next house.
 *
 * <p>The other candidate, {@code RLTile.isVisible()}, is the <i>player's</i> lighting mask
 * and belongs to the renderer; using it for an NPC is what once made a crate a witness to
 * murder. This is a symmetric Bresenham ray with no radius of its own — pair it with a range
 * check, which is cheap and rejects almost everything first.
 */
public final class Sight {

    /**
     * Deliberately <i>not</i> {@code BresLos(true)}, though symmetry is what we want.
     *
     * <p>rlforj's own symmetric branch dereferences the projection path it was told not to
     * compute — {@code oldpath.size()} on a null — so it throws the first time a ray is
     * blocked and then silently stops throwing, because the failed attempt left the field
     * non-null. Casting the reverse ray here costs the same and cannot do that. In this
     * configuration the algorithm touches no state at all, so one instance is safe to share.
     */
    private static final BresLos LOS = new BresLos(false);

    private Sight() {}

    public static boolean canSee(Entity looker, Entity target) {
        if (looker == null || target == null
                || looker.origin == null || target.origin == null) {
            return false;
        }
        if (looker.getLayerId() != target.getLayerId()) {
            return false;   //no seeing into the basement through the floor
        }
        return clearLine(ClientGameEnvironment.getWorldLayer(looker.getLayerId()),
                looker.x(), looker.y(), target.x(), target.y());
    }

    /** True if nothing between the two tiles blocks the view. Endpoints never block. */
    public static boolean clearLine(WorldLayer layer, int x0, int y0, int x1, int y1) {
        if (layer == null) {
            return false;
        }
        if (x0 == x1 && y0 == y1) {
            return true;
        }
        //symmetric by hand: if you can see me, I can see you, whichever way the ray rounds
        ILosBoard ray = new Ray(layer, x0, y0, x1, y1);
        return LOS.existsLineOfSight(ray, x0, y0, x1, y1, false)
                || LOS.existsLineOfSight(ray, x1, y1, x0, y0, false);
    }

    /**
     * The tiles as an rlforj board.
     *
     * <p>Two deliberate differences from {@code RLWorldModel}, which is the same idea for the
     * player's FOV. Endpoints are transparent: an actor's own tile counts as blocked (they
     * are standing on it) and rlforj tests the starting cell, so without this nobody could
     * see anything. And people do not block each other — a crowded street would otherwise
     * make everyone in it invisible to everyone else, which is the opposite of what a crowd
     * is. Furniture still does.
     */
    private static final class Ray implements ILosBoard {
        private final WorldLayer layer;
        private final int ax, ay, bx, by;

        Ray(WorldLayer layer, int ax, int ay, int bx, int by) {
            this.layer = layer;
            this.ax = ax;
            this.ay = ay;
            this.bx = bx;
            this.by = by;
        }

        public boolean contains(int x, int y) {
            return true;
        }

        public boolean isObstacle(int x, int y) {
            if ((x == ax && y == ay) || (x == bx && y == by)) {
                return false;
            }
            WorldTile tile = layer.get_tile(x, y);
            if (tile == null) {
                return true;
            }
            if (tile instanceof RLTile && ((RLTile) tile).isWall()) {
                return true;
            }
            if (!tile.isBlocked()) {
                return false;
            }
            return !(tile.getEntity() instanceof EntityRLActor);
        }

        public void visit(int x, int y) {
        }
    }
}
