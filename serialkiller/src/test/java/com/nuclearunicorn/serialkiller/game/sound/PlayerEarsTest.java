package com.nuclearunicorn.serialkiller.game.sound;

import com.nuclearunicorn.libroguelike.game.world.layers.WorldLayer;
import com.nuclearunicorn.serialkiller.game.sound.PlayerEars.Heard;
import com.nuclearunicorn.serialkiller.game.sound.PlayerEars.Verdict;
import com.nuclearunicorn.serialkiller.generators.TileGrid;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What the player gets out of a line somebody said (SOUND_DESIGN.md 4.6).
 *
 * <p>Sight and hearing are separate inputs here, so all four combinations exist and each one
 * has to render differently. The two that used to be indistinguishable are the interesting
 * ones: before this, a line spoken behind a closed door and a line spoken to your face
 * produced exactly the same bubble in exactly the same place, and the place was frequently
 * off the edge of what the player could see at all.
 *
 * <p>Thresholds are the plain {@link SoundConfig#HEAR_BASE}: a bare {@code RLTile} is neither
 * indoors nor a road, so {@link Ambient} reads it as parkland, whose noise floor is below the
 * listener's own threshold at every hour. That keeps these tests about geometry and off the
 * clock — {@code AcousticsTest} owns the ambient arithmetic.
 */
class PlayerEarsTest {

    private static final int EARS = SoundConfig.HEAR_BASE;

    // -------------------------------------------------------------- seen and heard

    @Test
    void someoneTalkingInFrontOfYouArrivesWithTheWords() {
        Verdict v = heard(TileGrid.open(25, 5), 0, 2, 3, 2, true);
        assertEquals(Heard.WORDS, v.heard);
    }

    /**
     * Nine tiles of open air is the last tile that hears anything (32 − 2·9 = 14, over the
     * threshold of 12); the tenth receives exactly 12 and "heard" is strict. So this is the
     * boundary itself, not a value near it.
     */
    @Test
    void theSameLineOneTileFurtherIsOnlyLips() {
        WorldLayer street = TileGrid.open(25, 5);
        assertEquals(Heard.WORDS, heard(street, 0, 2, 9, 2, true).heard);
        assertEquals(Heard.LIPS, heard(street, 0, 2, 10, 2, true).heard,
                "you can see them talking across the square without making out a word");
    }

    @Test
    void watchingSomeoneShoutFromTheFarSideOfTownIsStillJustLips() {
        //well outside the flood: the field does not even allocate this far out
        assertEquals(Heard.LIPS, heard(TileGrid.open(60, 5), 0, 2, 50, 2, true).heard);
    }

    // ------------------------------------------------------------ heard, not seen

    @Test
    void aVoiceThroughAnOpenDoorIsHeardWithoutBeingSeen() {
        Verdict v = heard(TileGrid.doorway(SoundConfig.TL_DOOR_OPEN, 25), 0, 2, 2, 2, false);
        assertEquals(Heard.EARSHOT, v.heard, "an open doorway is a hole; you hear right through it");
        assertEquals("west", v.bearing, "and you can tell which way it came from");
    }

    @Test
    void earAgainstTheDoorStillWorks() {
        //the eavesdrop case: 32 - (2+14) - 2 = 14, over the threshold by two
        assertEquals(Heard.EARSHOT,
                heard(TileGrid.doorway(SoundConfig.TL_DOOR_SHUT, 25), 0, 2, 2, 2, false).heard);
    }

    /**
     * The bearing is where the sound <i>arrived from</i>, not where the speaker is. Those are
     * different whenever a wall is involved, and the difference is the whole reason the
     * direction field exists.
     */
    @Test
    void theBearingPointsAtTheWayOutNotAtTheSpeaker() {
        WorldLayer l = TileGrid.parse(
                "======",
                "=S...=",
                "====/=",
                "......",
                "......");
        Verdict v = heard(l, 1, 1, 1, 4, false);
        assertEquals(Heard.EARSHOT, v.heard);
        assertNotEquals("north", v.bearing,
                "the speaker is due north, but there is a wall in the way");
        assertTrue(v.bearing.contains("east"),
                "the voice comes out of the door off to the east; got " + v.bearing);
    }

    // -------------------------------------------------------------------- neither

    @Test
    void aWallLeavesNothingAtAll() {
        Verdict v = heard(TileGrid.barrier(25, 5, 1, -1, 0, SoundConfig.TL_WALL_INNER),
                0, 2, 2, 2, false);
        assertEquals(Heard.NOTHING, v.heard);
        assertNull(v.bearing);
    }

    @Test
    void aShutDoorTwoTilesBackIsAlreadyNothing() {
        //one step off the door: exactly 12, which does not clear a threshold of 12
        assertEquals(Heard.NOTHING,
                heard(TileGrid.doorway(SoundConfig.TL_DOOR_SHUT, 25), 0, 2, 3, 2, false).heard);
    }

    // ------------------------------------------------------------- what gets drawn

    /**
     * The mapping the renderer and the chat log actually consume. Both of the middle rows are
     * new: a bubble that says nothing, and words with no bubble anywhere.
     */
    @Test
    void eachVerdictDrawsSomethingDifferent() {
        assertEquals("hello", PlayerEars.bubbleText(Heard.WORDS, "hello"));
        assertEquals(PlayerEars.INAUDIBLE_BUBBLE, PlayerEars.bubbleText(Heard.LIPS, "hello"));
        assertNull(PlayerEars.bubbleText(Heard.EARSHOT, "hello"),
                "heard through a wall - there is nothing on screen to hang a bubble on");
        assertNull(PlayerEars.bubbleText(Heard.NOTHING, "hello"));

        assertTrue(PlayerEars.audible(Heard.WORDS));
        assertTrue(PlayerEars.audible(Heard.EARSHOT), "you got the words, so the log gets them");
        assertFalse(PlayerEars.audible(Heard.LIPS), "watching lips move is not a transcript");
        assertFalse(PlayerEars.audible(Heard.NOTHING));
    }

    // ------------------------------------------------------- acts, not only words

    /**
     * Anything an NPC <i>does</i> goes through the same four verdicts, at its own level.
     * Which is the point of the overload: "X had sex with Y" used to be an unconditional
     * console line, so the player was told about every coupling in town from anywhere in it.
     */
    @Test
    void anActIsPerceivedOnTheSameTermsAsALine() {
        WorldLayer street = TileGrid.open(60, 5);
        int moan = SoundKind.MOAN.db();

        assertEquals(Heard.EARSHOT, act(street, 0, 2, 10, 2, false, moan).heard,
                "ten tiles down an open street, unseen: you hear it and nothing more");
        assertEquals(Heard.NOTHING, act(street, 0, 2, 40, 2, false, moan).heard,
                "the far side of town hears nothing, and the console must say nothing");
        assertEquals(Heard.WORDS, act(street, 0, 2, 3, 2, true, moan).heard,
                "in the same room, watching: the full named line");
    }

    /** Loudness is what separates them: a scream carries where a moan does not. */
    @Test
    void aLouderActCarriesFurther() {
        WorldLayer street = TileGrid.open(60, 5);
        assertEquals(Heard.NOTHING, act(street, 0, 2, 20, 2, false, SoundKind.MOAN.db()).heard);
        assertEquals(Heard.EARSHOT, act(street, 0, 2, 20, 2, false, SoundKind.SCREAM.db()).heard,
                "a rape is a scream, and a scream is heard two streets away");
    }

    /** Walls stop an act exactly as they stop a line. */
    @Test
    void aWallStopsAnActToo() {
        assertEquals(Heard.NOTHING,
                act(TileGrid.barrier(25, 5, 1, -1, 0, SoundConfig.TL_WALL_INNER),
                        0, 2, 2, 2, false, SoundKind.MOAN.db()).heard);
    }

    // ------------------------------------------------------------------- helpers

    private static Verdict heard(WorldLayer layer, int sx, int sy, int lx, int ly,
                                 boolean visible) {
        return PlayerEars.perceive(layer, sx, sy, lx, ly, EARS, visible);
    }

    private static Verdict act(WorldLayer layer, int sx, int sy, int lx, int ly,
                               boolean visible, int loudness) {
        return PlayerEars.perceive(layer, sx, sy, lx, ly, EARS, visible, loudness);
    }
}
