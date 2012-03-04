/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.vgui.effects;

import com.nuclearunicorn.libroguelike.utils.Timer;

import java.util.ArrayList;
import java.util.Collection;

/**
 *
 * @author Administrator
 */
public class Effect_Element {

    public long spawn_time = 0;
    public int life_time = 5000;

    Collection<Effect_Element> children = new ArrayList<Effect_Element>();

    protected Effect_Element parent = null;

    public void set_parent(Effect_Element parent){
        this.parent = parent;
    }

    public synchronized void add(Effect_Element child){
        children.add(child);
        child.set_parent(this);
        child.spawn();
    }

    public boolean remove(Effect_Element child){
        return children.remove(child);
    }

    public void spawn(){
        spawn_time = Timer.get_time();
    }

    protected void render_children(){
        if (children.isEmpty()){
            return;
        }

        Object[] elem =  children.toArray();
        for(int i = 0; i<elem.length; i++){
            Effect_Element __elem = (Effect_Element)elem[i];
            __elem.render();
        }
    }

    public void render() {
        render_children();
    }

    public void update(){
        Object[] elem =  children.toArray();
        for(int i = 0; i<elem.length; i++){
            Effect_Element __elem = (Effect_Element)elem[i];
            __elem.update();
        }
        gc();
    }

    /*
     * Check if any child object is expired and remove it
     */

    public void gc(){
        Object[] elem =  children.toArray();
        for(int i = 0; i<elem.length; i++){
            Effect_Element __elem = (Effect_Element)elem[i];
            __elem.gc();    //make sure this element subchildren are cleared successfully

            if (__elem.is_expired()){
                //__elem.clear();
                remove(__elem);
            }
        }
    }

    /*
     * Check if temporary object life time is expired
     */
    private boolean is_expired() {
        return spawn_time+life_time < Timer.get_time();
    }

    public int get_life_left(){
        return (int) (spawn_time + life_time - Timer.get_time());
    }

}
