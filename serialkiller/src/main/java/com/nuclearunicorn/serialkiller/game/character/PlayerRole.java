package com.nuclearunicorn.serialkiller.game.character;

/**
 * What the town takes the player for: the cover the killer lives behind.
 *
 * <p>This is deliberately <i>not</i> the {@code Role} enum that NPCs used to carry and that
 * {@link com.nuclearunicorn.serialkiller.game.ai.PoliceAI} was written to replace. That one
 * tried to decide behaviour — "is this NPC a policeman" asked at four call sites that could
 * disagree — and behaviour belongs in the brain. The player brings their own brain, so
 * nothing here branches on the role: it says who you are and, through
 * {@link CharacterPreset}, what that means for where you wake up and what is in your pockets.
 */
public enum PlayerRole {
    CITIZEN("citizen"),
    PROSTITUTE("prostitute"),
    POSTMAN("postman"),
    SHOPKEEPER("shopkeeper"),
    VAGRANT("vagrant");

    private final String displayName;

    PlayerRole(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
