package com.nuclearunicorn.negame.server.game.world.entities.environment.foliage;

import com.nuclearunicorn.negame.client.render.ASCIISpriteEntityRenderer;
import com.nuclearunicorn.negame.server.game.world.entities.EntityCommon;
import org.newdawn.slick.Color;

/**
 * @author Bloodrizer
 */
public class EntityGrass extends EntityCommon {
    public EntityGrass(){
        setRenderer( new ASCIISpriteEntityRenderer("\"", Color.green) );
    }
}
