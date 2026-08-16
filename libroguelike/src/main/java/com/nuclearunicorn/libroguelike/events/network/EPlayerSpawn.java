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
public class EPlayerSpawn extends NetworkEvent{
    public Point origin;
    public String uid;

    public EPlayerSpawn(Point origin, String uid){
        this.origin = origin;
        this.uid = uid;
    }
}
