package com.nuclearunicorn.negame.client.render;

import org.lwjgl.util.vector.Vector3f;

/**
 * Created with IntelliJ IDEA.
 * User: Administrator
 * Date: 23.06.12
 * Time: 18:17
 * To change this template use File | Settings | File Templates.
 */
public class VAVoxel {

    private Vector3f origin = new Vector3f(0,0,0);
    Vector3f vertexVec = new Vector3f(0,0,0);
    Vector3f textureVec = new Vector3f(0,0,0);
    Vector3f normalVec = new Vector3f(0,0,0);

    int textureTileId = 1;

    public float get_texture_size(){
        return 1.0f / 16;
    }

    public float get_texture_x(){
        return 1.0f / 16 * textureTileId -1;
    }

    public float get_texture_y(){
        return 1.0f / 16 * (textureTileId % 16 );
    }

    public void renderIntoVA(VAVoxelRenderer renderer){

        float tx = get_texture_x();
        float ty = get_texture_y();
        float ts = get_texture_size();

        float vo = 0.5f;

        float  x = origin.x*vo*2;
        float  y = origin.y*vo*2;
        float  z = origin.z*vo*2;

        //FRONT

        normalVec.set(0, 0, 0.5f);
        textureVec.set(tx, ty);
        vertexVec.set(-vo+x, -vo+y, vo+z);
        renderer.addVoxedData(vertexVec, normalVec, textureVec);

        textureVec.set(tx+ts, ty);
        vertexVec.set(vo+x, -vo+y, vo+z);
        renderer.addVoxedData(vertexVec, normalVec, textureVec);

        textureVec.set(tx+ts, ty+ts);
        vertexVec.set(vo+x, vo+y, vo+z);
        renderer.addVoxedData(vertexVec, normalVec, textureVec);

        textureVec.set(tx, ty+ts);
        vertexVec.set(-vo+x, vo+y, vo+z);
        renderer.addVoxedData(vertexVec, normalVec, textureVec);

        //RIGHT

        normalVec.set(0.5f, 0, 0);
        textureVec.set(tx+ts, ty);
        vertexVec.set(vo+x, -vo+y, -vo+z);
        renderer.addVoxedData(vertexVec, normalVec, textureVec);

        textureVec.set(tx+ts, ty+ts);
        vertexVec.set(vo+x, vo+y, -vo+z);
        renderer.addVoxedData(vertexVec, normalVec, textureVec);

        textureVec.set(tx, ty+ts);
        vertexVec.set(vo+x, vo+y, vo+z);
        renderer.addVoxedData(vertexVec, normalVec, textureVec);

        textureVec.set(tx, ty);
        vertexVec.set(vo+x, -vo+y, vo+z);
        renderer.addVoxedData(vertexVec, normalVec, textureVec);


    }

    public void setOrigin(float x, float y, float z) {
        origin.set(x, y, z);
    }
}
