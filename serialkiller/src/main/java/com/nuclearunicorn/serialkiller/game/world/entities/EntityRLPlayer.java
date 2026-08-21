package com.nuclearunicorn.serialkiller.game.world.entities;

import com.nuclearunicorn.libroguelike.events.ENotificationMessage;
import com.nuclearunicorn.libroguelike.game.combat.BasicCombat;
import com.nuclearunicorn.libroguelike.game.combat.Combat;
import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.render.EntityRenderer;
import com.nuclearunicorn.libroguelike.render.NPCRenderer;
import com.nuclearunicorn.serialkiller.game.character.PlayerRole;

/**
 */
public class EntityRLPlayer extends EntityRLHuman {

    //the cover the killer lives behind, straight off the new-game screen. Nothing branches
    //on it - the player supplies their own behaviour - but the town, and the character
    //sheet, are entitled to know what you tell people you are
    private PlayerRole role = PlayerRole.CITIZEN;

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

    public PlayerRole getRole(){
        return role;
    }

    public void setRole(PlayerRole role){
        this.role = role;
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
