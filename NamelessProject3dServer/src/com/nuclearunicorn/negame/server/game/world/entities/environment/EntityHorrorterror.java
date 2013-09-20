package com.nuclearunicorn.negame.server.game.world.entities.environment;

import com.nuclearunicorn.negame.client.render.ASCIISpriteEntityRenderer;
import com.nuclearunicorn.negame.server.game.world.entities.EntityCommon;
import org.newdawn.slick.Color;

/**
 * @author Bloodrizer
 */
public class EntityHorrorterror extends EntityCommon {
    public EntityHorrorterror(){
        setRenderer( new ASCIISpriteEntityRenderer("H", Color.red) );

        setName("Horrorterror");
        setDescription("Cute horrorterror");
    }


}
