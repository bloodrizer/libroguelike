package com.nuclearunicorn.serialkiller.game.social;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 Type of crimes Actor can commit.
 List of crimes are shown in the player profile
 */
public enum CrimeType {

    CRIME_VANDALISM,    //broked a window
    CRIME_TRESSPASSING, //entered a restricted area
    CRIME_THIEVERY, //stole something
    CRIME_ASSAULT,   //hit someone
    CRIME_MURDER;    //killed someone

    private static final List<CrimeType> VALUES =
            Collections.unmodifiableList(Arrays.asList(values()));
    private static final int SIZE = VALUES.size();
    private static final Random RANDOM = new Random();

    public static CrimeType getRandomCrime()  {
        return VALUES.get(RANDOM.nextInt(SIZE));
    }

}
