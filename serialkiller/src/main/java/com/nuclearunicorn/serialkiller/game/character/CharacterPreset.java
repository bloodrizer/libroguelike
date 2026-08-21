package com.nuclearunicorn.serialkiller.game.character;

import com.nuclearunicorn.serialkiller.game.world.entities.EntityRLHuman;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * One line on the new-game menu: everything that differs between two starts.
 *
 * <p>Modelled on how the roguelikes that do this well split it up — Cataclysm keeps a
 * <i>profession</i> (who you are, what you carry) apart from a <i>scenario</i> (where and
 * when you wake up), and it is the pairing of the two that makes a start. Here they are one
 * object, because the town is one town and the pairings that make sense are few: a postman
 * starts his round on the street, a working girl starts in a room at the brothel. Everything
 * a start can vary is a field of this class, so adding one is a table entry in
 * {@link CharacterPresets} rather than a branch in the spawn code.
 *
 * <p>Nothing here is applied by this class. {@link CharacterSetup} rolls the loose ends
 * (ANY gender, an age range, the "random" entry itself) exactly once per game and hands the
 * result to the two places that need it: the generator, which finds the spot, and
 * {@code InGameMode.spawn_player}, which builds the person.
 */
public final class CharacterPreset {

    /** Which sex the preset asks for. ANY leaves it to the dice. */
    public enum Gender {
        MALE("male"), FEMALE("female"), ANY("either");

        private final String displayName;

        Gender(String displayName) {
            this.displayName = displayName;
        }

        public String displayName() {
            return displayName;
        }

        /** The concrete sex to spawn with; ANY rolls a coin. */
        public EntityRLHuman.Sex roll(Random rng) {
            switch (this) {
                case MALE:   return EntityRLHuman.Sex.MALE;
                case FEMALE: return EntityRLHuman.Sex.FEMALE;
                default:     return rng.nextBoolean()
                        ? EntityRLHuman.Sex.MALE : EntityRLHuman.Sex.FEMALE;
            }
        }
    }

    /** One line of starting kit: what, how many, and whether it starts in hand. */
    public static final class Gear {
        public final String itemId;
        public final int count;
        public final boolean equipped;

        Gear(String itemId, int count, boolean equipped) {
            this.itemId = itemId;
            this.count = count;
            this.equipped = equipped;
        }

        @Override
        public String toString() {
            return count > 1 ? itemId + " x" + count : itemId;
        }
    }

    private final String id;
    private final String name;
    private final String blurb;
    private final PlayerRole role;
    private final SpawnPlace spawn;
    private final Gender gender;
    private final int minAge;
    private final int maxAge;
    private final List<Gear> gear;
    /** The "surprise me" entry: not a person, an instruction to pick one of the others. */
    private final boolean wildcard;

    private CharacterPreset(Builder b) {
        this.id = b.id;
        this.name = b.name;
        this.blurb = b.blurb;
        this.role = b.role;
        this.spawn = b.spawn;
        this.gender = b.gender;
        this.minAge = b.minAge;
        this.maxAge = b.maxAge;
        this.gear = Collections.unmodifiableList(new ArrayList<Gear>(b.gear));
        this.wildcard = b.wildcard;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getBlurb() { return blurb; }
    public PlayerRole getRole() { return role; }
    public SpawnPlace getSpawn() { return spawn; }
    public Gender getGender() { return gender; }
    public List<Gear> getGear() { return gear; }
    public boolean isWildcard() { return wildcard; }

    public EntityRLHuman.Sex rollSex(Random rng) {
        return gender.roll(rng);
    }

    public int rollAge(Random rng) {
        return minAge + rng.nextInt(Math.max(1, maxAge - minAge + 1));
    }

    /** "24-60", for the menu. */
    public String ageRange() {
        return minAge + "-" + maxAge;
    }

    /** Comma-separated kit, or "nothing but your hands". */
    public String gearSummary() {
        if (gear.isEmpty()) {
            return "nothing but your hands";
        }
        StringBuilder sb = new StringBuilder();
        for (Gear g : gear) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(g);
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return "CharacterPreset[" + id + "]";
    }

    public static Builder preset(String id, String name) {
        return new Builder(id, name);
    }

    /** Reads like the table it builds - see {@link CharacterPresets}. */
    public static final class Builder {
        private final String id;
        private final String name;
        private String blurb = "";
        private PlayerRole role = PlayerRole.CITIZEN;
        private SpawnPlace spawn = SpawnPlace.HOME;
        private Gender gender = Gender.ANY;
        private int minAge = 24;
        private int maxAge = 60;
        private final List<Gear> gear = new ArrayList<Gear>();
        private boolean wildcard = false;

        private Builder(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public Builder blurb(String blurb) { this.blurb = blurb; return this; }
        public Builder role(PlayerRole role) { this.role = role; return this; }
        public Builder spawn(SpawnPlace spawn) { this.spawn = spawn; return this; }
        public Builder gender(Gender gender) { this.gender = gender; return this; }

        public Builder age(int min, int max) {
            this.minAge = min;
            this.maxAge = max;
            return this;
        }

        /** Carried in the backpack. */
        public Builder gear(String itemId) { return gear(itemId, 1); }

        public Builder gear(String itemId, int count) {
            this.gear.add(new Gear(itemId, count, false));
            return this;
        }

        /** Carried and already in the slot it belongs to. */
        public Builder wearing(String itemId) {
            this.gear.add(new Gear(itemId, 1, true));
            return this;
        }

        /** Marks the "random" entry; it has no kit and no spawn of its own. */
        public Builder wildcard() { this.wildcard = true; return this; }

        public CharacterPreset build() {
            return new CharacterPreset(this);
        }
    }
}
