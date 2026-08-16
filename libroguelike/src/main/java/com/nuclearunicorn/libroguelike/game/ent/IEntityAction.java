/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.game.ent;

/**
 *
 * @author Administrator
 */
public interface IEntityAction {

    public void execute();
    public void set_entity(Entity ent);
    public void set_name(String name);
    public String get_name();
    
}
