package com.nuclearunicorn.serialkiller.game.ai;

/**
 * The libido drive thresholds (see {@link com.nuclearunicorn.serialkiller.game.bodysim.BodySimulation}).
 *
 * <p>Libido rises by {@link #PER_TURN} every turn. Below {@link #NEEDY} it is inert; between {@link #NEEDY}
 * and {@link #FRENZY} an adult seeks a lawful partner (a mate, or a prostitute at the brothel);
 * at {@link #FRENZY} — maxed out — there is no outlet left, and the need curdles into violence.
 *
 * <p>One number, three places (the trigger, the action, and the prompt), so they cannot
 * disagree about what "in the mood" means — and one rate, read by the body simulation, so
 * how often the town is in the mood is tuned here rather than inside a physiology loop.
 */
public final class Libido {

    /**
     * How fast the need builds, per turn — and one turn is one minute on the world clock,
     * which is the only unit this number means anything in.
     *
     * <p>It was 0.5, which is 200 minutes from indifferent to the ceiling: <i>seven times a
     * day</i>, for every adult in town. Nobody can cross a town and find a partner seven
     * times a day, so the whole population lived at the ceiling and {@link
     * com.nuclearunicorn.serialkiller.game.ai.behavior.RapeAction} became the town's normal
     * behaviour rather than its pathology. At 0.08 the climb to {@link #NEEDY} is about one
     * waking day, and there are then some six hours of daylight left to do something about
     * it before {@link #FRENZY}.
     */
    public static final float PER_TURN = 0.08f;

    /** Above this, seek a partner: a mate at home, or a prostitute at the brothel. */
    public static final float NEEDY = 70f;

    /** Maxed out with no partner available. The frenzy threshold ({@code BloodSimulation}
     *  caps the attribute at 100, so this is the ceiling). */
    public static final float FRENZY = 100f;

    private Libido() {}
}
