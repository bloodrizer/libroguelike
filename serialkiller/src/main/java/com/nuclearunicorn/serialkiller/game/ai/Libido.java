package com.nuclearunicorn.serialkiller.game.ai;

/**
 * The libido drive thresholds (see {@link com.nuclearunicorn.serialkiller.game.bodysim.BodySimulation}).
 *
 * <p>Libido rises steadily every turn. Below {@link #NEEDY} it is inert; between {@link #NEEDY}
 * and {@link #FRENZY} an adult seeks a lawful partner (a mate, or a prostitute at the brothel);
 * at {@link #FRENZY} — maxed out — there is no outlet left, and the need curdles into violence.
 *
 * <p>One number, three places (the trigger, the action, and the prompt), so they cannot
 * disagree about what "in the mood" means.
 */
public final class Libido {

    /** Above this, seek a partner: a mate at home, or a prostitute at the brothel. */
    public static final float NEEDY = 70f;

    /** Maxed out with no partner available. The frenzy threshold ({@code BloodSimulation}
     *  caps the attribute at 100, so this is the ceiling). */
    public static final float FRENZY = 100f;

    private Libido() {}
}
