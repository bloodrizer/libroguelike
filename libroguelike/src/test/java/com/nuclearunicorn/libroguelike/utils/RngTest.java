package com.nuclearunicorn.libroguelike.utils;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * The property that makes a seed worth having: a stream depends on its name and the seed,
 * and on nothing else. Without it "same seed, same town" stops being true the moment anyone
 * adds a roll anywhere, and before/after comparison across a change is worthless.
 */
class RngTest {

    private static List<Integer> draw(String stream, int n) {
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            out.add(Rng.nextInt(stream, 1000));
        }
        return out;
    }

    @Test
    void sameSeedSameStream() {
        Rng.seed(42);
        List<Integer> first = draw(Rng.WORLDGEN, 20);
        Rng.seed(42);
        List<Integer> second = draw(Rng.WORLDGEN, 20);
        assertEquals(first, second);
    }

    @Test
    void streamsDifferFromEachOther() {
        Rng.seed(42);
        List<Integer> worldgen = draw(Rng.WORLDGEN, 20);
        Rng.seed(42);
        List<Integer> combat = draw(Rng.COMBAT, 20);
        assertNotEquals(worldgen, combat, "distinct names must give distinct sequences");
    }

    /** The whole point: rolling in one stream must not move any other stream along. */
    @Test
    void oneStreamIsUnaffectedByAnother() {
        Rng.seed(7);
        List<Integer> aiAlone = draw(Rng.AI, 10);

        Rng.seed(7);
        draw(Rng.WORLDGEN, 500);          // a generator change: hundreds of extra rolls
        draw(Rng.NAMES, 37);
        List<Integer> aiAfterOthers = draw(Rng.AI, 10);

        assertEquals(aiAlone, aiAfterOthers,
                "AI rolls moved because worldgen rolled more - the streams are still coupled");
    }

    /** Nor may the order in which streams are first touched matter. */
    @Test
    void firstTouchOrderIsIrrelevant() {
        Rng.seed(99);
        List<Integer> combatFirst = draw(Rng.COMBAT, 10);

        Rng.seed(99);
        draw(Rng.NAMES, 1);
        draw(Rng.WORLD, 1);
        List<Integer> combatLater = draw(Rng.COMBAT, 10);

        assertEquals(combatFirst, combatLater);
    }

    /** derive() hands out a private Random; two of them must not be the same sequence. */
    @Test
    void derivedStreamsAreDistinctButReproducible() {
        Rng.seed(5);
        long a1 = Rng.derive(Rng.WORLDGEN).nextLong();
        long b1 = Rng.derive(Rng.WORLDGEN).nextLong();
        assertNotEquals(a1, b1, "two derived streams must not be identical");

        Rng.seed(5);
        assertEquals(a1, Rng.derive(Rng.WORLDGEN).nextLong());
        assertEquals(b1, Rng.derive(Rng.WORLDGEN).nextLong());
    }

    @Test
    void nextIntToleratesNonPositiveBound() {
        Rng.seed(1);
        assertEquals(0, Rng.nextInt(Rng.AI, 0));
        assertEquals(0, Rng.nextInt(Rng.AI, -5));
    }

    @Test
    void reseedingResetsEveryStream() {
        Rng.seed(3);
        int before = Rng.nextInt(Rng.WORLD, 1000);
        Rng.nextInt(Rng.WORLD, 1000);
        Rng.seed(3);
        assertEquals(before, Rng.nextInt(Rng.WORLD, 1000));
    }
}
