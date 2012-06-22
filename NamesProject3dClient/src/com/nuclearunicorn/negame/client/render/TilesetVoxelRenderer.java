package com.nuclearunicorn.negame.client.render;

import com.nuclearunicorn.libroguelike.game.world.WorldTile;
import com.nuclearunicorn.libroguelike.game.world.WorldView;
import com.nuclearunicorn.libroguelike.render.WindowRender;
import com.nuclearunicorn.libroguelike.render.layers.LayerRenderer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.vector.Vector3f;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;

/**
 * Created with IntelliJ IDEA.
 * User: Administrator
 * Date: 21.06.12
 * Time: 0:35
 * To change this template use File | Settings | File Templates.
 */
public class TilesetVoxelRenderer extends LayerRenderer {

    //TODO: move to the global lighting class

    private float lightAmbient[] = { 0.5f, 0.5f, 0.5f, 1.0f };
    private float lightDiffuse[] = { 1.0f, 1.0f, 1.0f, 1.0f };
    private float lightPosition[] = { 30.0f, 60.0f, 0.0f, 0.0f };

    float lightSpecular[] = { 1.0f, 1.0f, 1.0f, 1.0f };

    static Voxel voxelRenderer = new Voxel(0,0,0);
    static VBO vboRenderer = new VBO();

    public static Camera3D  camera = new Camera3D(0, -25, 0 );
    
    
    static {
        camera.pitch(30.0f);
        camera.yaw(30.0f);
    }

    public TilesetVoxelRenderer(){
    }

    @Override
    public void render_tile(WorldTile tile, int tile_x, int tile_y) {
        //TODO: render voxel tile

        int height = WorldView.getYOffset(tile);

        glColor3f(0.3f, 0.8f, 0.3f);
        voxelRenderer.set_origin(tile_x*1.1f, height*0.05f , tile_y*1.1f);
        voxelRenderer.render();

    }

    @Override
    public void beforeRender() {

        /*  SET 3D RENDER MODE */

        glViewport(0,0,WindowRender.get_window_w(),WindowRender.get_window_h());
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();

        GLU.gluPerspective(45.0f,((float)WindowRender.get_window_w())/((float)WindowRender.get_window_h()),0.1f,100.0f);
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();

        //glPolygonMode( GL_FRONT_AND_BACK, GL_LINE );

        glDisable(GL11.GL_TEXTURE_2D);
        glColor3f(0.3f, 0.8f, 0.3f);


        /*  SET CAMERA POSITION TO THE SCENE */

        //camera.update();
        camera.setMatrix();

        /*  SET LIGHTING TO THE SCENE */

        setSceneLighting();
    }

    private void setSceneLighting() {

        /*Vector3f sun_color = World.__enviroment.get_sun_color();

        lightAmbient[0] = sun_color.x;
        lightAmbient[1] = sun_color.y;
        lightAmbient[2] = sun_color.z;*/

        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT|GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glTranslatef(0.0f,-2.0f,0.0f); // Move Into The Screen 5 Units

        ByteBuffer temp = ByteBuffer.allocateDirect(16);
        temp.order(ByteOrder.nativeOrder());
        GL11.glLight(GL11.GL_LIGHT1, GL11.GL_AMBIENT, (FloatBuffer)temp.asFloatBuffer().put(lightAmbient).flip());
        GL11.glLight(GL11.GL_LIGHT1, GL11.GL_DIFFUSE, (FloatBuffer)temp.asFloatBuffer().put(lightDiffuse).flip());
        GL11.glLight(GL11.GL_LIGHT1, GL11.GL_POSITION,(FloatBuffer)temp.asFloatBuffer().put(lightPosition).flip());
        GL11.glEnable(GL11.GL_LIGHT1);
        GL11.glEnable(GL11.GL_LIGHTING);

        GL11.glEnable(GL11.GL_COLOR_MATERIAL);
        GL11.glColorMaterial ( GL11.GL_FRONT, GL11.GL_AMBIENT_AND_DIFFUSE ) ;
        GL11.glMaterialf( GL11.GL_FRONT, GL11.GL_SHININESS, 100.0f);
    }

    @Override
    public void afterRender() {

        //vboRenderer.render();


        //reset from 3d mode back to 2d
        //TODO: move to WindowRender.switch2d() / switch3d()
        glEnable(GL11.GL_TEXTURE_2D);


        GL11.glLoadIdentity();

        GL11.glViewport(0, 0, WindowRender.get_window_w(), WindowRender.get_window_h());
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();


        GL11.glOrtho(0.0f, WindowRender.get_window_w(), WindowRender.get_window_h(), 0.0f, -1.0f, 1.0f);
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();

        //glPolygonMode( GL_FRONT_AND_BACK, GL_FILL );
    }
}