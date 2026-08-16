package com.nuclearunicorn.serialkiller.game.world.fov;

import rlforj.los.IFovAlgorithm;
import rlforj.los.PrecisePermissive;

/**
 * Where field-of-view comes from on this platform.
 *
 * Callers construct through here rather than naming an algorithm directly, so
 * the browser build can substitute one: TeaVM miscompiles rlforj's
 * PrecisePermissive and it null-derefs inside its scan on any board with walls
 * (see PORTING.md §10).
 */
public final class FovFactory {

    private FovFactory() {
    }

    public static IFovAlgorithm create() {
        return new PrecisePermissive();
    }
}
