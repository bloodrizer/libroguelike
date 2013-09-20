package com.nuclearunicorn.negame.server.game.world.entities;

import com.nuclearunicorn.libroguelike.game.ent.EntityNPC;
import com.nuclearunicorn.negame.server.core.User;
import com.nuclearunicorn.negame.server.game.world.entities.actors.EntityCommonNPC;

/**
 * Created with IntelliJ IDEA.
 * User: Administrator
 * Date: 28.06.12
 * Time: 0:21
 * To change this template use File | Settings | File Templates.
 */
public class EntityPlayerNPC extends EntityCommonNPC {
    User user;
    public EntityPlayerNPC(User user){
        this.user = user;
    }

    public User getUser() {
        return user;
    }
}
