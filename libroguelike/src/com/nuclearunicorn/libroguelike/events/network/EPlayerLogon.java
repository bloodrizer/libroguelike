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
@NetID(id="0x0000")
public class EPlayerLogon extends NetworkEvent{
    public Point origin = null;
    public EPlayerLogon(Point origin){
        this.origin = origin;
    }
}
