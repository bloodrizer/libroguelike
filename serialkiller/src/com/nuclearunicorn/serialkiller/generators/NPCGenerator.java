package com.nuclearunicorn.serialkiller.generators;

import com.nuclearunicorn.serialkiller.game.social.CrimeRecord;
import com.nuclearunicorn.serialkiller.game.social.CrimeType;
import com.nuclearunicorn.serialkiller.game.world.entities.EntityRLHuman;
import org.newdawn.slick.Color;

import java.util.Random;

/**

 */
public class NPCGenerator {
    public static EntityRLHuman generateNPC(Random chunk_random, TownChunkGenerator generator, int x, int y) {

        EntityRLHuman entity = new EntityRLHuman();
        generator.placeEntity(x, y, entity, "NPC", "@", new Color(150,250,150));

        entity = generateNPCStats(chunk_random, entity);
        return entity;
    }

    public static EntityRLHuman generateNPCStats(Random chunk_random, EntityRLHuman entity ){
        NameGenerator namegen = new NameGenerator();

        boolean isMale = false;
        if (chunk_random.nextInt(100)>50){
            entity.setSex(EntityRLHuman.Sex.MALE);
            isMale = true;
        } else {
            entity.setSex(EntityRLHuman.Sex.FEMALE);
        }

        entity.setName(namegen.generate(isMale));
        entity.age = 8 + chunk_random.nextInt(87);

        entity.race = EntityRLHuman.Race.getRandomRace();


        //generate criminal background
        int criminalRate = 30;  //30%
        if (chunk_random.nextInt(100) < criminalRate){
            int crimeTypes = chunk_random.nextInt(5);  //random criminal records
            for (int i = 0; i< crimeTypes; i++){
                entity.addCrimeRecord(new CrimeRecord(CrimeType.getRandomCrime()));
            }
        }

        return entity;
    }
}
