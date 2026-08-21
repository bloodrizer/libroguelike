package com.nuclearunicorn.serialkiller.game.character;

import com.nuclearunicorn.serialkiller.game.character.CharacterPreset.Gender;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * The starts you can pick from, in menu order, "random" last as roguelikes have always
 * put it. Built once and immutable, like {@code BuildingTemplates} - one table, so a new
 * life is an entry here and nothing else.
 *
 * <p>The citizen's kit is deliberately the exact kit the game handed every player before
 * presets existed: it is the default, and a default that plays differently from what came
 * before is a regression wearing a feature's clothes.
 */
public final class CharacterPresets {

    private static final List<CharacterPreset> ALL = build();

    private CharacterPresets() {}

    public static List<CharacterPreset> all() {
        return ALL;
    }

    /** The preset with this id, or null. Ids are what {@code -Dlrl.preset} takes. */
    public static CharacterPreset byId(String id) {
        for (CharacterPreset preset : ALL) {
            if (preset.getId().equals(id)) {
                return preset;
            }
        }
        return null;
    }

    /** What you get if you never open the wizard. */
    public static CharacterPreset citizen() {
        return byId("citizen");
    }

    /** A concrete life, never the wildcard - what "random" resolves to. */
    public static CharacterPreset roll(Random rng) {
        List<CharacterPreset> real = new ArrayList<CharacterPreset>();
        for (CharacterPreset preset : ALL) {
            if (!preset.isWildcard()) {
                real.add(preset);
            }
        }
        return real.get(rng.nextInt(real.size()));
    }

    /** Every id, for the "no such preset" complaint. */
    public static String ids() {
        StringBuilder sb = new StringBuilder();
        for (CharacterPreset preset : ALL) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(preset.getId());
        }
        return sb.toString();
    }

    private static List<CharacterPreset> build() {
        List<CharacterPreset> list = new ArrayList<CharacterPreset>();

        list.add(CharacterPreset.preset("citizen", "Citizen")
                .blurb("A quiet man with a flat, a family, and a hammer in the drawer.")
                .role(PlayerRole.CITIZEN)
                .spawn(SpawnPlace.HOME)
                .gender(Gender.ANY)
                .age(24, 60)
                .gear("hammer")
                .gear("knife")
                .gear("taser")
                .gear("valium")
                .gear("food", 5)
                .build());

        list.add(CharacterPreset.preset("prostitute", "Prostitute")
                .blurb("She works nights, and nobody counts the men she leaves with.")
                .role(PlayerRole.PROSTITUTE)
                .spawn(SpawnPlace.BROTHEL_ROOM)
                .gender(Gender.FEMALE)
                .age(19, 35)
                .wearing("pepper spray")
                .gear("knife")
                .gear("valium")
                .gear("cash", 40)
                .build());

        list.add(CharacterPreset.preset("postman", "Postman")
                .blurb("Every door in town opens for the post. Nobody remembers the face.")
                .role(PlayerRole.POSTMAN)
                .spawn(SpawnPlace.STREET)
                .gender(Gender.ANY)
                .age(22, 55)
                .gear("parcel")
                .gear("keys")
                .gear("knife")
                .gear("food", 2)
                .build());

        list.add(CharacterPreset.preset("shopkeeper", "Shopkeeper")
                .blurb("He keeps late hours, and the back room has no windows.")
                .role(PlayerRole.SHOPKEEPER)
                .spawn(SpawnPlace.SHOP_FLOOR)
                .gender(Gender.ANY)
                .age(30, 65)
                .wearing("crowbar")
                .gear("keys")
                .gear("cash", 60)
                .gear("food", 3)
                .build());

        list.add(CharacterPreset.preset("vagrant", "Vagrant")
                .blurb("Nobody looks at him twice. Nobody will miss him either.")
                .role(PlayerRole.VAGRANT)
                .spawn(SpawnPlace.PARK)
                .gender(Gender.ANY)
                .age(30, 70)
                .wearing("bottle")
                .gear("food")
                .build());

        //last, as it has been in every roguelike since the eighties
        list.add(CharacterPreset.preset("random", "Random")
                .blurb("Deal me a life.")
                .wildcard()
                .build());

        return Collections.unmodifiableList(list);
    }
}
