package com.nuclearunicorn.serialkiller.game.character;

import com.nuclearunicorn.libroguelike.game.items.BaseItem;
import com.nuclearunicorn.libroguelike.utils.Rng;
import com.nuclearunicorn.serialkiller.game.ItemFactory;
import com.nuclearunicorn.serialkiller.game.world.entities.EntityRLHuman;

import java.util.List;
import java.util.Random;

/**
 * The one place that knows which life this game is being started with.
 *
 * <p>A preset is picked in the menu, on a screen that is gone by the time the world exists,
 * and is then needed twice in two very different places - the generator, deciding where to
 * put the player, and the spawn, deciding who the player is. So the choice is parked here
 * and read from both.
 *
 * <p>The wildcard ("random"), an ANY gender and an age range are all rolled <i>once</i>, by
 * {@link #current()}, and remembered for the rest of the game. Rolling per caller would mean
 * the generator hunting for a brothel room while the spawn built a postman.
 */
public final class CharacterSetup {

    /** Start the game as this preset without touching the menu: {@code -Dlrl.preset=postman}. */
    private static final String PRESET_PROPERTY = "lrl.preset";

    private static CharacterPreset chosen;   //what the menu picked, null until it does
    private static CharacterPreset active;   //the wildcard resolved, for this world only

    private CharacterSetup() {}

    /** Take the preset the wizard is holding. Does not start a game by itself. */
    public static void choose(CharacterPreset preset) {
        chosen = preset;
        active = null;   //a fresh pick re-rolls whatever the last one settled
    }

    /** What the menu is showing as picked, before any dice. */
    public static CharacterPreset chosen() {
        if (chosen == null) {
            chosen = fromProperty();
        }
        return chosen;
    }

    /**
     * The life this world is being built for: a concrete preset, wildcard resolved.
     * Stable for as long as the world lives.
     */
    public static CharacterPreset current() {
        if (active == null) {
            CharacterPreset pick = chosen();
            active = pick.isWildcard() ? CharacterPresets.roll(rng()) : pick;
            if (pick.isWildcard()) {
                System.out.println("[preset] random rolled " + active.getId());
            }
        }
        return active;
    }

    /** Forget the roll so the next world re-rolls it. Called when a new game starts. */
    public static void reset() {
        active = null;
    }

    /**
     * Make the entity into the person the preset describes: sex, age, and what is in the
     * pockets. Called after the NPC stat roll, which sets those at random - the preset is
     * meant to overrule it.
     */
    public static void apply(EntityRLHuman ent) {
        CharacterPreset preset = current();
        Random rng = rng();

        ent.setSex(preset.rollSex(rng));
        ent.age = preset.rollAge(rng);

        for (CharacterPreset.Gear gear : preset.getGear()) {
            give(ent, gear);
        }

        System.out.println("[preset] " + preset.getId() + ": " + ent.getSex().toString().toLowerCase()
                + ", " + ent.age + ", starting " + preset.getSpawn().displayName());
    }

    private static void give(EntityRLHuman ent, CharacterPreset.Gear gear) {
        BaseItem item = ItemFactory.produce(gear.itemId, gear.count);
        if (item == null) {
            System.err.println("[preset] no such item '" + gear.itemId + "'");
            return;
        }
        ent.getContainer().add_item(item);

        if (!gear.equipped) {
            return;
        }
        //equip the copy the container kept: add_item stores a clone, and equipping the
        //original leaves the slot pointing at an item nobody is carrying
        List<BaseItem> carried = (List<BaseItem>) ent.getContainer().getItems();
        for (BaseItem stored : carried) {
            if (stored.get_type().equals(item.get_type())) {
                ent.equipment.equip_item(stored);
                return;
            }
        }
    }

    /** A stream of its own, so picking a life does not shift the town or the dice in combat. */
    private static Random rng() {
        return Rng.derive(Rng.CHARACTER);
    }

    private static CharacterPreset fromProperty() {
        String id = System.getProperty(PRESET_PROPERTY);
        if (id == null || id.trim().isEmpty()) {
            return CharacterPresets.citizen();
        }
        CharacterPreset preset = CharacterPresets.byId(id.trim());
        if (preset == null) {
            System.err.println("[preset] unknown -D" + PRESET_PROPERTY + "=" + id
                    + " (have: " + CharacterPresets.ids() + "), starting as citizen");
            return CharacterPresets.citizen();
        }
        System.out.println("[preset] " + PRESET_PROPERTY + "=" + preset.getId());
        return preset;
    }
}
