package com.nuclearunicorn.serialkiller.game.sound;

/**
 * The catalogue of noises the game can make, each with a source level in dB
 * (SOUND_DESIGN.md 4.2).
 *
 * <p>One enum for every modality on purpose. Speech, footsteps and a broken window all
 * propagate through the same field, so "can he hear me talking through that door" and
 * "can he hear me killing someone through that door" are the same question asked twice
 * — which is exactly what the old two-path radius model could not express.
 */
public enum SoundKind {

    WHISPER(18, false),
    TALK(32, false),
    SHOUT(55, false),
    /** Not speech: a scream is a noise, and it is reported as a crime. */
    SCREAM(70, true),

    /** Below {@link SoundConfig#FLOOR}, so it is inaudible everywhere by construction. */
    FOOTSTEP_SNEAK(8, false),
    FOOTSTEP_WALK(20, false),
    FOOTSTEP_RUN(34, false),

    DOOR_OPEN(22, false),
    DOOR_SLAM(45, false),
    DOOR_KICK(60, true),

    PUNCH(32, true),
    KNIFE(26, true),
    BONE_BREAK(48, true),
    BODY_FALL(40, true),
    GLASS_BREAK(58, true),
    GUNSHOT(92, true);

    private final int db;
    private final boolean suspicious;

    SoundKind(int db, boolean suspicious) {
        this.db = db;
        this.suspicious = suspicious;
    }

    /** Source level at the emitting tile, before any spreading or transmission loss. */
    public int db() {
        return db;
    }

    /**
     * Whether hearing this is worth calling the police over. Footsteps and conversation
     * are heard and ignored; a scream or a gunshot is a crime report with no suspect
     * named, which is the only thing an unseen noise can ever produce.
     */
    public boolean isSuspicious() {
        return suspicious;
    }

    /** Whether this is a spoken line, and so belongs to the conversation pipeline. */
    public boolean isSpeech() {
        return this == WHISPER || this == TALK || this == SHOUT;
    }
}
