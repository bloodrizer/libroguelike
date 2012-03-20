/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.libroguelike.render.overlay;

import com.nuclearunicorn.libroguelike.render.WindowRender;
import org.newdawn.slick.Color;

/**
 *
 * @author Administrator
 */
public class VersionOverlay {

    //to lazy to implement this shit
    private static final String CLIENT_VER = "0.1.5";
    private static final String BUILD_NUMBER = "1050";

public static void render(){
        OverlaySystem.ttf.drawString(WindowRender.get_window_w() - 110, 30,
        "Ver."+CLIENT_VER +
        "("+BUILD_NUMBER+")",
        Color.white);
}
        }
