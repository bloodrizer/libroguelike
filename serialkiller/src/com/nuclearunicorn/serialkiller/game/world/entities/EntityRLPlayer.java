package com.nuclearunicorn.serialkiller.game.world.entities;

import com.nuclearunicorn.libroguelike.events.ENotificationMessage;
import com.nuclearunicorn.libroguelike.game.combat.BasicCombat;
import com.nuclearunicorn.libroguelike.game.combat.Combat;
import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.render.EntityRenderer;
import com.nuclearunicorn.libroguelike.render.NPCRenderer;

/**
 */
public class EntityRLPlayer extends EntityRLHuman {

    public EntityRLPlayer(){

        Combat __combat = new BasicCombat();
        __combat.set_hp(500);

        set_blocking(true);
        set_combat(__combat);

    }

    @Override
    public boolean isPlayerEnt(){
        return true;
    }

    @Override
    public EntityRenderer build_render(){
        NPCRenderer __render = (NPCRenderer)(super.build_render());
        return __render;
    }

    @Override
    public void die(Entity killer){
        super.die(killer);
        ENotificationMessage msg = new ENotificationMessage("You were killed by a "+killer.getName());
        msg.setManager(env.getEventManager());
        msg.post();
    }
}
