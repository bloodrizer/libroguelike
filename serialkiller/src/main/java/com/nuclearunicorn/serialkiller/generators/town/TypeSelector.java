package com.nuclearunicorn.serialkiller.generators.town;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Per-chunk building-type picker (TOWN_GENERATION_DESIGN.md 6). Filters
 * templates by lot fit, enforces the chunk-wide caps (>=60% apartments, <=1
 * bank / police / brothel), then does a weighted random pick. One instance per
 * chunk so it can keep the running counts.
 */
public class TypeSelector {

    private int total = 0;
    private int apartments = 0;
    private int banks = 0;
    private int police = 0;
    private int brothels = 0;

    private static final float MIN_APARTMENT_RATIO = 0.6f;

    /** Pick a type for a lot of the given size and record it. Never returns null. */
    public BuildingType pick(int lotW, int lotH, Random rng) {
        List<BuildingTemplate> candidates = new ArrayList<BuildingTemplate>();
        float weightSum = 0f;

        //keep apartments at >=60%: if a non-apartment pick would break that,
        //only APARTMENT is allowed this round.
        boolean apartmentsOnly = apartments < MIN_APARTMENT_RATIO * (total + 1);

        for (int i = 0; i < BuildingTemplates.all().size(); i++) {
            BuildingTemplate t = BuildingTemplates.all().get(i);
            if (!t.fitsLot(lotW, lotH)) {
                continue;
            }
            if (t.type != BuildingType.APARTMENT && apartmentsOnly) {
                continue;
            }
            if (t.type == BuildingType.BANK && banks >= 1) {
                continue;
            }
            if (t.type == BuildingType.POLICE_STATION && police >= 1) {
                continue;
            }
            if (t.type == BuildingType.BROTHEL && brothels >= 1) {
                continue;
            }
            candidates.add(t);
            weightSum += t.weight;
        }

        BuildingType chosen = BuildingType.APARTMENT;
        if (!candidates.isEmpty() && weightSum > 0f) {
            float roll = rng.nextFloat() * weightSum;
            float acc = 0f;
            for (int i = 0; i < candidates.size(); i++) {
                acc += candidates.get(i).weight;
                if (roll < acc) {
                    chosen = candidates.get(i).type;
                    break;
                }
            }
        }

        record(chosen);
        return chosen;
    }

    private void record(BuildingType t) {
        total++;
        if (t == BuildingType.APARTMENT) apartments++;
        else if (t == BuildingType.BANK) banks++;
        else if (t == BuildingType.POLICE_STATION) police++;
        else if (t == BuildingType.BROTHEL) brothels++;
    }

    /** Register a type that was assigned outside pick() (e.g. the forced safehouse). */
    public void forceRecord(BuildingType t) {
        record(t);
    }
}
