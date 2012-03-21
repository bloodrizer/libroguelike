package com.nuclearunicorn.serialkiller.generators;

import com.nuclearunicorn.serialkiller.game.world.entities.EntityRLHuman;
import org.newdawn.slick.Color;

import java.util.Random;

/**
 * Created by IntelliJ IDEA.
 * User: dpopov
 * Date: 21.03.12
 * Time: 17:59
 * To change this template use File | Settings | File Templates.
 */
public class NPCGenerator {
    public static EntityRLHuman generateNPC(Random chunk_random, TownChunkGenerator generator, int x, int y) {

        EntityRLHuman entity = new EntityRLHuman();
        generator.placeEntity(x, y, entity, "NPC", "@", new Color(150,250,150));


        NameGenerator namegen = new NameGenerator();

        boolean isMale = false;
        if (chunk_random.nextInt(100)>50){
            entity.setSex(EntityRLHuman.Sex.MALE);
            isMale = true;
        } else {
            entity.setSex(EntityRLHuman.Sex.FEMALE);
        }

        entity.setName(namegen.generate(isMale));

        //generate criminal background
        int criminalRate = 30;  //30%
        if (chunk_random.nextInt(100) < criminalRate){
            int crimeTypes = chunk_random.nextInt(5);  //random criminal records
            for (int i = 0; i< crimeTypes; i++){

            }
        }

        return entity;
    }
}
