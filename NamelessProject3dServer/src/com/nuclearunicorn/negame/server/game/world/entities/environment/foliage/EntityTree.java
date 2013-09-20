package com.nuclearunicorn.negame.server.game.world.entities.environment.foliage;

import com.nuclearunicorn.negame.client.render.ASCIISpriteEntityRenderer;
import com.nuclearunicorn.negame.server.game.world.entities.EntityCommon;
import org.newdawn.slick.Color;

/**
 * @author Bloodrizer
 */
public class EntityTree extends EntityCommon {
    public EntityTree(){
        //BAD BAD BAD idea for server
        //setRenderer(new ASCIISpriteEntityRenderer("T", Color.green));
    }

    @Override
    public void initClient() {
        super.initClient();
        //setRenderer(new ASCIISpriteEntityRenderer("T", Color.green));
    }
}
