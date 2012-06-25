package com.nuclearunicorn.negame.client.render.utils;

import org.lwjgl.opengl.GL11;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Created with IntelliJ IDEA.
 * User: Administrator
 * Date: 24.06.12
 * Time: 11:15
 * To change this template use File | Settings | File Templates.
 */
public class LightEnvironment {

    private float lightAmbient[] = { 0.5f, 0.5f, 0.5f, 1.0f };
    private float lightDiffuse[] = { 1.0f, 1.0f, 1.0f, 1.0f };
    private float lightPosition[] = { 30.0f, 60.0f, 0.0f, 0.0f };

    float lightSpecular[] = { 1.0f, 1.0f, 1.0f, 1.0f };
    ByteBuffer temp = ByteBuffer.allocateDirect(16);

    public LightEnvironment(){

    }

    public void setSceneLighting(){

        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT|GL11.GL_DEPTH_BUFFER_BIT);
        //GL11.glTranslatef(0.0f,-2.0f,0.0f); // Move Into The Screen 5 Units

        temp.clear();
        temp.order(ByteOrder.nativeOrder());

        GL11.glLight(GL11.GL_LIGHT1, GL11.GL_AMBIENT, (FloatBuffer) temp.asFloatBuffer().put(lightAmbient).flip());
        GL11.glLight(GL11.GL_LIGHT1, GL11.GL_DIFFUSE, (FloatBuffer)temp.asFloatBuffer().put(lightDiffuse).flip());
        GL11.glLight(GL11.GL_LIGHT1, GL11.GL_POSITION,(FloatBuffer)temp.asFloatBuffer().put(lightPosition).flip());
        GL11.glEnable(GL11.GL_LIGHT1);
        GL11.glEnable(GL11.GL_LIGHTING);

    }


}
