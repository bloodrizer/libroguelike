/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.game.ent.controller;

/**
 *
 * @author Administrator
 */
public class MobController extends NpcController{

    @Override
    public void e_on_obstacle(int x, int y) {


        if (owner.getAI() != null ){
            owner.getAI().e_on_obstacle(x, y);
        }

        //if(actor!=null && owner.get_combat() !=null){
            //owner.get_combat().inflict_damage(actor);
            //TODO: pass event to the ai manager and let it decide weither to attack target or not
        //}
    }
}
