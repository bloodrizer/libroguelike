package com.nuclearunicorn.serialkiller.game.personality;

import com.nuclearunicorn.serialkiller.game.world.entities.EntityRLHuman;

import java.util.HashMap;
import java.util.Map;

/**
   Base class for victim preferences
   Contains various modifiers
 */
public class VictimPreferences {

    private float generalModifier = 20.0f;
    private Map<String, Float> modifiers = new HashMap<String, Float>(32);

    public void registerVictim(EntityRLHuman victim) {

        generalModifier -= 5f;

        String attrName;
        for(EntityRLHuman.Race raceType: EntityRLHuman.Race.values()){
            attrName = "RACE_"+raceType.name();
            if(raceType.equals(victim.race)){
                alterModifier(attrName,2f);
            }
            alterModifier(attrName,-0.5f);
        }
        for(EntityRLHuman.Sex sexType: EntityRLHuman.Sex.values()){
            attrName = "SEX_"+sexType.name();
            if(sexType.equals(victim.getSex())){
                alterModifier(attrName,2f);
            }
            alterModifier(attrName,-0.5f);
        }
        for(EntityRLHuman.Religion religionType: EntityRLHuman.Religion.values()){
            attrName = "RELIGION_"+religionType.name();
            if(religionType.equals(victim.religion)){
                alterModifier(attrName,2f);
            }
            alterModifier(attrName,-0.5f);
        }
    }

    private void alterModifier( String attr , float amt){
        if (!modifiers.containsKey(attr)){
            modifiers.put(attr, 0f);
        }
        Float newAmt = modifiers.get(attr) + amt;
        modifiers.put(attr, newAmt);
    }

    public float getTotalModifier(EntityRLHuman victim){
        float modifier = this.generalModifier;
        for(EntityRLHuman.Race raceType: EntityRLHuman.Race.values()){
            if(raceType.equals(victim.race)){
                modifier += modifiers.get("RACE_"+raceType.name());
            }
        }
        for(EntityRLHuman.Sex sexType: EntityRLHuman.Sex.values()){
            if(sexType.equals(victim.race)){
                modifier += modifiers.get("SEX_"+sexType.name());
            }
        }
        for(EntityRLHuman.Religion religionType: EntityRLHuman.Religion.values()){
            if(religionType.equals(victim.race)){
                modifier += modifiers.get("RELIGION_"+religionType.name());
            }
        }
        return modifier;
    }
}
