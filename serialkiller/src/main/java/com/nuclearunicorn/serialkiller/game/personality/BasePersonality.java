package com.nuclearunicorn.serialkiller.game.personality;

import com.nuclearunicorn.serialkiller.game.world.entities.EntityRLHuman;

/**
    Base class for personality traits
 */
public class BasePersonality {
    public enum EYE_COLOR {
        BLUE,
        GREEN,
        BROWN
    }

    public enum HAIR_COLOR {
        BLACK,
        GRAY,
        BROWN,
        BLONDE
    }

    public EYE_COLOR eyeColor;
    public HAIR_COLOR hairColor;

    protected VictimPreferences victimPreferences = new VictimPreferences();

    //TODO: move various shit like age/race/etc there

    public void registerVictim(EntityRLHuman victim){
        victimPreferences.registerVictim(victim);
    }

    public float getVictimModifier(EntityRLHuman victim){
        return victimPreferences.getTotalModifier(victim);
    }
}
