package com.nuclearunicorn.libroguelike.core.debug;

/**
 * Standing instruments, switched on from the command line.
 *
 * <p>These exist because the alternative is what actually happens otherwise: a bug turns up,
 * someone sprinkles {@code System.err.println} through four files, rebuilds five times to
 * narrow it down, and deletes the lot. The probes are worth more than the bug — every flag
 * here started as a throwaway print that found something.
 *
 * <pre>
 *   -Ddebug.path=validate    every route returned by A* is checked: contiguous, and never
 *                            crossing a tile the pathfinder itself calls blocked
 *   -Ddebug.world=ready      one dump of the map and its connected components at world-ready
 *   -Ddebug.census=&lt;n&gt;       every n turns, a town-wide tally of what the NPCs are doing
 *   -Ddebug.strict=true      a violated check throws instead of printing (for CI)
 * </pre>
 *
 * Findings go to stderr <i>and</i> to the replay log as observations, so they end up in the
 * same JSONL a harness is already reading.
 */
public final class DebugFlags {

    private static final String path = System.getProperty("debug.path", "");
    private static final String world = System.getProperty("debug.world", "");
    private static final int census = Integer.getInteger("debug.census", 0);
    private static final boolean strict = Boolean.getBoolean("debug.strict");

    private DebugFlags() {}

    public static boolean validatePaths() {
        return "validate".equalsIgnoreCase(path) || "1".equals(path) || "true".equalsIgnoreCase(path);
    }

    public static boolean dumpWorldAtReady() {
        return "ready".equalsIgnoreCase(world) || "1".equals(world) || "true".equalsIgnoreCase(world);
    }

    /** Turns between censuses, or 0 for off. */
    public static int censusEvery() {
        return census;
    }

    /** In strict mode a failed check is fatal — which is what you want from a test run. */
    public static boolean strict() {
        return strict;
    }

    /** Report a violated invariant. Throws under {@code -Ddebug.strict}. */
    public static void violation(String what, String detail) {
        String line = "DEBUG-VIOLATION " + what + ": " + detail;
        System.err.println(line);
        if (strict) {
            throw new IllegalStateException(line);
        }
    }
}
