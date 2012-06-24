package com.nuclearunicorn.negame.client.render;

import com.nuclearunicorn.libroguelike.core.Input;
import com.nuclearunicorn.libroguelike.game.world.WorldChunk;
import com.nuclearunicorn.libroguelike.game.world.WorldTile;
import com.nuclearunicorn.libroguelike.game.world.WorldView;
import com.nuclearunicorn.libroguelike.game.world.layers.WorldLayer;
import com.nuclearunicorn.libroguelike.render.Render;
import com.nuclearunicorn.libroguelike.render.WindowRender;
import com.nuclearunicorn.libroguelike.render.layers.LayerChunkRenderer;
import com.nuclearunicorn.negame.client.game.world.NEVoxelTile;
import com.nuclearunicorn.negame.client.game.world.NEWorldView;
import com.nuclearunicorn.negame.client.render.overlays.NEDebugOverlay;
import com.nuclearunicorn.negame.client.render.utils.Raycast;
import org.lwjgl.util.Point;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;

/**
 * Created with IntelliJ IDEA.
 * User: Administrator
 * Date: 21.06.12
 * Time: 0:35
 * To change this template use File | Settings | File Templates.
 */
public class TilesetVoxelRenderer extends LayerChunkRenderer {

    //TODO: move to the global lighting class

    public static Camera3D  camera = new Camera3D(0, -25, 0 );
    static {
        camera.pitch(30.0f);
        camera.yaw(30.0f);
    }

    VAVoxel vaVoxel;
    VAVoxelRenderer vaRenderer;
    LightEnvironment lightEnv;
    private Voxel voxelRenderer = new Voxel(0,0,0);

    public TilesetVoxelRenderer(){
        vaVoxel = new VAVoxel();
        vaRenderer = new VAVoxelRenderer();
        lightEnv = new LightEnvironment();
    }

    public static Camera3D getCamera() {
        return camera;
    }

    @Override
    public void renderChunk(WorldLayer layer, WorldChunk chunk, int i, int j) {
        super.renderChunk(layer, chunk, i, j);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void render_tile(WorldTile tile, int tile_x, int tile_y) {
        //TODO: render voxel tile

        int height = WorldView.getYOffset(tile);

        //glColor3f(0.3f, 0.8f, 0.3f);


        //SELECTION DEBUG START
        Point selectedTileCoord = NEWorldView.getSelectedTileCoord();
        if (selectedTileCoord.getX() == tile_x && selectedTileCoord.getY() == tile_y){
            vaVoxel.topTileId = 7;
        }else{
            vaVoxel.topTileId = 1;
        }

        vaVoxel.setOrigin(tile_x * 1.000005f, height * 0.05f, tile_y * 1.000005f);
        vaVoxel.renderIntoVA(vaRenderer, (NEVoxelTile)tile);


        //voxelRenderer.set_origin(tile_x * 1.1f, height * 0.05f, tile_y * 1.1f);
        //voxelRenderer.render();

        //voxelRenderer.set_origin(tile_x * 1.1f, height * 0.05f, tile_y * 1.1f);
        //voxelRenderer.build_vbo(vboRenderer);
    }

    @Override
    public void beforeRender() {
        WindowRender.set3DMode();


        //camera.update();
        camera.setMatrix();

        /*  SET LIGHTING TO THE SCENE */
        setSceneLighting();

        //GL11.glPolygonMode( GL11.GL_FRONT_AND_BACK, GL11.GL_LINE );

        vaRenderer.clearBuffers();

        Render.bind_texture("/resources/terrain.png");
    }


    @Override
    public void afterRender() {

        vaRenderer.flushBuffers();
        vaRenderer.render();

        FloatBuffer World_Ray = Raycast.getMousePosition(Input.get_mx(), Input.get_my());
        /*NEDebugOverlay.wx = World_Ray.get(0);
        NEDebugOverlay.wy = World_Ray.get(1);
        NEDebugOverlay.wz = World_Ray.get(2);*/
        float wx = World_Ray.get(0);
        float wy = World_Ray.get(1);
        float wz = World_Ray.get(2);

        NEDebugOverlay.wx = wx;
        NEDebugOverlay.wy = wy;
        NEDebugOverlay.wz = wz;

        /*
            Voxel render uses half voxel offset from 0,0 origin, so mouse coords actually start at -0.5f, -0.5f
         */
        NEWorldView.setMouseXWorld(wx+VAVoxel.VOXEL_SIZE/2f);
        NEWorldView.setMouseYWorld(wz+VAVoxel.VOXEL_SIZE/2f);

        //reset from 3d mode back to 2d
        //TODO: move to WindowRender.switch2d() / switch3d()
        WindowRender.set2DMode();

    }

    private void setSceneLighting() {

        /*Vector3f sun_color = World.__enviroment.get_sun_color();

        lightAmbient[0] = sun_color.x;
        lightAmbient[1] = sun_color.y;
        lightAmbient[2] = sun_color.z;*/

        lightEnv.setSceneLighting();

        glEnable(GL_COLOR_MATERIAL);
        glColorMaterial(GL_FRONT, GL_AMBIENT_AND_DIFFUSE) ;
        glMaterialf(GL_FRONT, GL_SHININESS, 100.0f);
    }

}