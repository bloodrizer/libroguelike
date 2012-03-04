/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.events.network;

import org.lwjgl.util.Point;

/**
 *
 * @author Administrator
 */
public class EBuildStructure extends NetworkEvent {

    private Point origin   = new Point();
    private String item_type = "undefined";
    //public Entity entity = null;

    public EBuildStructure(Point origin, String item_type){
        this.origin.setLocation(origin);
        this.item_type = item_type;
    }



    @Override
    public String get_id(){
        return "0x220";

    }

    @Override
    public String[] serialize(){
        return new String[] {
            get_id(),
            item_type,
            Integer.toString(this.origin.getX()),
            Integer.toString(this.origin.getY())
        };
    }

}
