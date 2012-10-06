package com.nuclearunicorn.negame.client.render;

import com.nuclearunicorn.libroguelike.game.world.WorldView;
import com.nuclearunicorn.libroguelike.render.EntityRenderer;
import com.nuclearunicorn.libroguelike.render.WindowRender;
import com.nuclearunicorn.negame.client.render.utils.Voxel;

/**
 * @author bloodrizer
 * This is simple voxel rendre for entity that renders single solid voxel cube
 *
 * TODO: introduce complex composite voxel render featuring render of voxel array using VA / VBO
 */
public class VoxelEntityRenderer extends EntityRenderer {

    private Voxel voxelRenderer = new Voxel(0,0,0);

    @Override
    public void render() {
        WindowRender.set3DMode();   //this is BS
        TilesetVoxelRenderer.camera.setMatrix();

        //ent   <<-- render me

        int tileHeight = WorldView.getYOffset(ent.tile);

        //System.out.println("rendering voxel @" + ent.tile.origin.getX() + "," + ent.tile.origin.getY());

        //20 is magic constant from getYOffset, it is single atomic step offset
        voxelRenderer.set_origin(ent.origin.getX() * 1.00005f, (tileHeight + 20 ) * 0.05f, ent.origin.getY() * 1.00005f);
        voxelRenderer.render();


        WindowRender.set2DMode();   //this is BS too
    }
}
