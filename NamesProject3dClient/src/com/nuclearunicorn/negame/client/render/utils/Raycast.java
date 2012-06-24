package com.nuclearunicorn.negame.client.render.utils;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * Created with IntelliJ IDEA.
 * User: Administrator
 * Date: 25.06.12
 * Time: 0:04
 * To change this template use File | Settings | File Templates.
 */
public class Raycast {

    static public FloatBuffer getMousePosition(int mouseX, int mouseY)
    {
        IntBuffer viewport = BufferUtils.createIntBuffer(16);
        FloatBuffer modelview = BufferUtils.createFloatBuffer(16);
        FloatBuffer projection = BufferUtils.createFloatBuffer(16);
        FloatBuffer winZ = BufferUtils.createFloatBuffer(1);

        float winX, winY;
        FloatBuffer position = BufferUtils.createFloatBuffer(3);

        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelview);
        GL11.glGetFloat( GL11.GL_PROJECTION_MATRIX, projection );
        GL11.glGetInteger( GL11.GL_VIEWPORT, viewport );

        winX = (float)mouseX;
        winY = (float)viewport.get(3) - (float)mouseY;

        GL11.glReadPixels(mouseX, (int)winY, 1, 1, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, winZ);

        GLU.gluUnProject(winX, winY, winZ.get(), modelview, projection, viewport, position);

        return position;
    }
}