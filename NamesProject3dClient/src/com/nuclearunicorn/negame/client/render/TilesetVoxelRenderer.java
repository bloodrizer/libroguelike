package com.nuclearunicorn.negame.client.render;

import com.nuclearunicorn.libroguelike.game.world.WorldTile;
import com.nuclearunicorn.libroguelike.render.WindowRender;
import com.nuclearunicorn.libroguelike.render.layers.LayerRenderer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import static org.lwjgl.opengl.GL11.*;

/**
 * Created with IntelliJ IDEA.
 * User: Administrator
 * Date: 21.06.12
 * Time: 0:35
 * To change this template use File | Settings | File Templates.
 */
public class TilesetVoxelRenderer extends LayerRenderer {

    static Voxel voxelRenderer = new Voxel(0,0,0);
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
        voxelRenderer.set_origin(tile_x, 0, tile_y);
        voxelRenderer.render();
    }

    @Override
    public void beforeRender() {

        glViewport(0,0,WindowRender.get_window_w(),WindowRender.get_window_h());
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();

        GLU.gluPerspective(45.0f,((float)WindowRender.get_window_w())/((float)WindowRender.get_window_h()),0.1f,100.0f);
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();

        glPolygonMode( GL_FRONT_AND_BACK, GL_LINE );

        glDisable(GL11.GL_TEXTURE_2D);
        glColor3f(0.5f, 0.5f, 0.5f);

        //voxelRenderer.set_origin(0, 0, 0);
        //voxelRenderer.render();

        voxelRenderer.set_origin(1, 1, 1);
        voxelRenderer.render();

        //camera.update();
        camera.setMatrix();
    }

    @Override
    public void afterRender() {
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

        glPolygonMode( GL_FRONT_AND_BACK, GL_FILL );
    }
}
