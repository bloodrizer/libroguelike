package com.nuclearunicorn.serialkiller.game.world.fov;

import rlforj.los.IFovAlgorithm;

/**
 * Web build: rlforj's PrecisePermissive is unusable here — TeaVM miscompiles it
 * and it null-derefs on any board with walls, which is every board the game has.
 * Verified with com.nuclearunicorn.web.WebProbe: the same call succeeds on the
 * JVM and throws under TeaVM, for both the vendored jar and its recompiled
 * sources, so it is a backend defect rather than stale bytecode.
 *
 * {@link Shadowcast} stands in. It is a different algorithm, so the visible set
 * differs slightly from the desktop build around corners.
 */
public final class FovFactory {

    private FovFactory() {
    }

    public static IFovAlgorithm create() {
        return new Shadowcast();
    }
}
