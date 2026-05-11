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

    CRIME_VANDALISM("Vandalism"),    //broked a window
    CRIME_TRESPASSING("Trespassing"), //entered a restricted area
    CRIME_THIEVERY("Thievery"), //stole something
    CRIME_ASSAULT("Assault"),   //hit someone
    CRIME_MURDER("Murder");    //killed someone


    String displayName;

    private static final List<CrimeType> VALUES =
            Collections.unmodifiableList(Arrays.asList(values()));
    private static final int SIZE = VALUES.size();
    private static final Random RANDOM = new Random();

    CrimeType(String name) {
        this.displayName = name;
    }
    
    public String diplayName(){
        return displayName;
    }

    public static CrimeType getRandomCrime()  {
        return VALUES.get(RANDOM.nextInt(SIZE));
    }

}
