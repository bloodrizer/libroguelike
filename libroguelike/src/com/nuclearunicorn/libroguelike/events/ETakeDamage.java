/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.events;

import com.nuclearunicorn.libroguelike.game.combat.Damage;
import com.nuclearunicorn.libroguelike.game.ent.Entity;

/**
 *
 * This is client-side transitional damage event
 * The main purpose of this event is to notify
 * view FX system to draw damage effect
 *
 */
public class ETakeDamage extends Event{
    public Entity ent;
    public Damage dmg;

    public ETakeDamage(Entity taker, Damage dmg){
        this.ent = taker;
        this.dmg = dmg;
    }
}
