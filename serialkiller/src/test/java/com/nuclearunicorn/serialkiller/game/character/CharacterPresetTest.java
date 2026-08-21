package com.nuclearunicorn.serialkiller.game.character;

import com.nuclearunicorn.libroguelike.game.items.BaseItem;
import com.nuclearunicorn.serialkiller.game.ItemFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The preset table, checked for the mistakes a table invites.
 *
 * <p>Kit is named by string, and a name that matches nothing produces a character missing an
 * item with one line on stderr to say so — during world generation, where nobody is reading.
 * That is exactly the class of typo a table should not be able to ship with.
 */
class CharacterPresetTest {

    /** The choice is global state; leave it as the rest of the suite expects to find it. */
    @AfterEach
    void restoreDefaultPreset() {
        CharacterSetup.choose(CharacterPresets.citizen());
        CharacterSetup.reset();
    }

    @Test
    void everyPresetCarriesItemsThatExist() {
        List<String> missing = new ArrayList<String>();

        for (CharacterPreset preset : CharacterPresets.all()) {
            for (CharacterPreset.Gear gear : preset.getGear()) {
                BaseItem item = ItemFactory.produce(gear.itemId, gear.count);
                if (item == null) {
                    missing.add(preset.getId() + " -> '" + gear.itemId + "'");
                }
            }
        }

        assertTrue(missing.isEmpty(), () -> "no such item: " + missing);
    }

    @Test
    void presetIdsAreUniqueAndTheWildcardIsLast() {
        Set<String> ids = new HashSet<String>();
        List<CharacterPreset> all = CharacterPresets.all();

        for (CharacterPreset preset : all) {
            assertTrue(ids.add(preset.getId()), "duplicate preset id " + preset.getId());
        }

        for (int i = 0; i < all.size() - 1; i++) {
            assertFalse(all.get(i).isWildcard(),
                    "the wildcard belongs at the end of the menu, not at " + i);
        }
        assertTrue(all.get(all.size() - 1).isWildcard(), "the menu has no 'random' entry");
    }

    /**
     * "Random" has to become somebody before the world is built and stay that somebody
     * afterwards: the generator and the spawn both ask, and a second roll between the two
     * would hunt for a brothel room and then spawn a postman in it.
     */
    @Test
    void theWildcardRollsOnceAndHoldsUntilTheNextGame() {
        CharacterSetup.choose(CharacterPresets.byId("random"));

        CharacterPreset rolled = CharacterSetup.current();
        assertFalse(rolled.isWildcard(), "random resolved to itself");
        assertEquals(rolled, CharacterSetup.current(), "the roll was not remembered");

        CharacterSetup.reset();
        assertFalse(CharacterSetup.current().isWildcard(), "the re-roll resolved to itself");
    }

    @Test
    void choosingAPresetIsWhatTheGameThenStartsWith() {
        CharacterPreset postman = CharacterPresets.byId("postman");
        CharacterSetup.choose(postman);

        assertEquals(postman, CharacterSetup.chosen());
        assertEquals(postman, CharacterSetup.current());
        assertEquals(SpawnPlace.STREET, CharacterSetup.current().getSpawn());
    }
}
