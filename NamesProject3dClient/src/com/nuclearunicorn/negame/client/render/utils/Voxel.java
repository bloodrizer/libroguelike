/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.negame.client.render.utils;

import com.nuclearunicorn.negame.client.render.math.Vector3;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector3f;
import org.newdawn.slick.opengl.Texture;

/**
 * Class that supports basic block rendering
 * @author red
 */
public class Voxel {


    public Texture texture;
    private Vector3f origin = new Vector3f(0,0,0);
    public int textureTileId = 1;

    boolean fv, kv, lv, rv, tv, bv; //side visiblility flags

    static Vector3f vertex_normal = new Vector3f(0.0f, 0.0f, 0.0f);

    public float getVoxelSize() {
        return voxelSize;
    }

    public void setVoxelSize(float voxelSize) {
        this.voxelSize = voxelSize;
    }

    public float voxelSize = 1.0f;

    public Voxel(float x, float y, float z){
        origin = new Vector3f(x,y,z);
        init();
    }

    public Voxel(Vector3 vec){
        origin = new Vector3f(vec.x(),vec.y(),vec.z());
        init();
    }

    public void set_origin(float x, float y, float z){
        origin.set(x,y,z);
    }

    public void init(){
        fv = kv = lv = rv = tv = bv = true;
        /*try {
            texture = TextureLoader.getTexture("PNG", new FileInputStream("Data/terrain.png"));
        }
        catch (IOException ex) {
             Logger.getLogger(Voxel.class.getName()).log(Level.SEVERE, null, ex);
        }*/
    }


    public float get_texture_size(){
        return 1.0f / 16;
    }

    public float get_texture_x(){
        return 1.0f / 16 * (textureTileId -1);
    }

    public float get_texture_y(){
        return 1.0f / 16 * (textureTileId % 16 );
    }



    //fill vbo buffer with voxel coord data
    public static VBO _vbo = null;

    public void build_vbo(VBO vbo ){
         float tx = get_texture_x();
         float ty = get_texture_y();
         float ts = get_texture_size();

         float vo = voxelSize /2;

         float  x = origin.x* voxelSize;
         float  y = origin.y* voxelSize;
         float  z = origin.z* voxelSize;


         _vbo = vbo;

        //put_vertex(-1.0f + x, -1.0f + y, 1.0f + z, tx, ty);


        //f k l r t b
         //front
         if (fv) {
             put_normal(0.0f, 0.0f, 0.5f);

             put_vertex(-vo + x, -vo + y, vo + z, tx, ty);
             put_vertex( vo + x, -vo + y, vo + z, tx +ts, ty);
             put_vertex( vo + x, vo + y,  vo + z, tx +ts, ty + ts);
             put_vertex(-vo + x, vo + y,  vo + z, tx, ty + ts);
        }
         //back
         if (kv) {
             put_normal(0.0f, 0.0f, -0.5f);

             put_vertex(-vo + x, -vo + y,  -vo + z, tx+ts, ty);
             put_vertex(-vo + x,  vo + y,  -vo + z, tx+ts, ty+ts);
             put_vertex( vo + x, vo + y,  -vo + z,  tx,    ty+ts);
             put_vertex(vo + x,  -vo + y,  -vo + z, tx,    ty);
        }

          //left?
         if (lv) {
            put_normal(-0.5f, 0.0f, 0.0f);

            put_vertex(-vo + x, -vo + y, -vo + z,    tx, ty);
            put_vertex(-vo + x, -vo + y, vo + z,     tx+ts, ty);
            put_vertex(-vo + x, vo + y,  vo + z,     tx+ts, ty+ts);
            put_vertex(-vo + x, vo + y,  -vo + z,    tx, ty+ts);
        }

         //*top*
         if (tv) {
             put_normal(0.0f, 0.5f, 0.0f);

             put_vertex(-vo + x, vo + y,  -vo + z, tx , ty+ts);
             put_vertex(-vo + x, vo + y,  vo + z,  tx, ty);
             put_vertex( vo + x, vo + y,  vo + z,  tx+ts, ty);
             put_vertex( vo + x, vo + y,  -vo + z, tx+ts, ty+ts);
        }

            
         //*bottom*
         if (bv) {
            put_normal(0.0f, -0.5f, 0.0f);

            put_vertex(-vo + x, -vo + y,  -vo + z, tx+ts, ty+ts);
            put_vertex(vo + x,  -vo + y,  -vo + z, tx, ty+ts);
            put_vertex( vo + x, -vo + y,  vo + z,  tx, ty);
            put_vertex( -vo + x, -vo + y, vo + z,  tx+ts, ty);
        }

         
         //right
         if (rv) {
            put_normal(0.5f, 0.0f, 0.0f);

            put_vertex(vo + x, -vo + y, -vo + z, tx+ts, ty);
            put_vertex(vo + x, vo + y,  -vo + z, tx+ts, ty+ts);
            put_vertex(vo + x, vo + y,  vo + z,  tx, ty+ts);
            put_vertex(vo + x, -vo + y, vo + z,  tx, ty);
        }

        

    }

    public void put_vertex(float x, float y, float z, float tx, float ty){

        if (_vbo.VBO_buffer_size >= _vbo.VBO_max_buffer_size){
            return; //safe switch
        }

        //#hacky safe switch
        // HACK HACK HACK

        //if (_vbo.vertexPositionAttributes != null) {


            
         _vbo.get_vpa().putFloat(x);
         _vbo.get_vpa().putFloat(y);
         _vbo.get_vpa().putFloat(z);

         _vbo.get_vpa().putFloat(vertex_normal.x);
         _vbo.get_vpa().putFloat(vertex_normal.y);
         _vbo.get_vpa().putFloat(vertex_normal.z);

         _vbo.get_vpa().putFloat(tx);
         _vbo.get_vpa().putFloat(ty);

       //}


         _vbo.get_vi().putInt(_vbo.vertex_index++);

         _vbo.VBO_buffer_size++;
    }

    public void put_normal(float nx, float ny, float nz){
        vertex_normal.set(nx, ny, nz);
    }

    public void render(){

        GL11.glPushMatrix();    //save for the fuck's safe

        //GL11.glTranslatef(origin.x,origin.y,origin.z);
        //GL11.glScalef(0.5f,0.5f,0.5f);

        float x = origin.x;
        float y = origin.y;
        float z = origin.z;

        float vo = voxelSize /2;

         GL11.glBegin(GL11.GL_QUADS);

         float tx = get_texture_x();
         float ty = get_texture_y();
         float ts = get_texture_size();


         // Front Face

         GL11.glNormal3f(0, 0, 0.5f);
         GL11.glTexCoord2f(tx, ty);
         GL11.glVertex3f(-vo+x, -vo+y,  vo+z);   // Bottom Left Of The Texture and Quad

         GL11.glNormal3f(0, 0, 0.5f);
         GL11.glTexCoord2f(tx+ts, ty);
         GL11.glVertex3f( vo+x, -vo+y,  vo+z);   // Bottom Right Of The Texture and Quad

         GL11.glNormal3f(0, 0, 0.5f);
         GL11.glTexCoord2f(tx+ts, ty+ts);
         GL11.glVertex3f( vo+x,  vo+y,  vo+z);   // Top Right Of The Texture and Quad

         GL11.glNormal3f(0, 0, 0.5f);
         GL11.glTexCoord2f(tx, ty+ts);
         GL11.glVertex3f(-vo+x,  vo+y,  vo+z);   // Top Left Of The Texture and Quad

        // Right face
        GL11.glNormal3f(0.5f, 0, 0);
        GL11.glTexCoord2f(tx+ts, ty);
        GL11.glVertex3f( vo+x, -vo+y, -vo+z);   // Bottom Right Of The Texture and Quad

        GL11.glNormal3f(0.5f, 0, 0);
        GL11.glTexCoord2f(tx+ts, ty+ts);
        GL11.glVertex3f( vo+x,  vo+y, -vo+z);   // Top Right Of The Texture and Quad

        GL11.glNormal3f(0.5f, 0, 0);
        GL11.glTexCoord2f(tx, ty+ts);
        GL11.glVertex3f( vo+x,  vo+y,  vo+z);   // Top Left Of The Texture and Quad

        GL11.glNormal3f(0.5f, 0, 0);
        GL11.glTexCoord2f(tx, ty);
        GL11.glVertex3f( vo+x, -vo+y,  vo+z);   // Bottom Left Of The Texture and Quad


        // Top Face
        GL11.glNormal3f(0, 0.5f, 0);
        GL11.glTexCoord2f(tx, ty+ts);
        GL11.glVertex3f(-vo+x,  vo+y, -vo+z);   // Top Left Of The Texture and Quad

        GL11.glNormal3f(0, 0.5f, 0);
        GL11.glTexCoord2f(tx, ty);
        GL11.glVertex3f(-vo+x,  vo+y,  vo+z);   // Bottom Left Of The Texture and Quad

        GL11.glNormal3f(0, 0.5f, 0);
        GL11.glTexCoord2f(tx+ts, ty);
        GL11.glVertex3f( vo+x,  vo+y,  vo+z);   // Bottom Right Of The Texture and Quad

        GL11.glNormal3f(0, 0.5f, 0);
        GL11.glTexCoord2f(tx+ts, ty+ts);
        GL11.glVertex3f( vo+x,  vo+y, -vo+z);   // Top Right Of The Texture and Quad

         // Back Face


         GL11.glNormal3f(0, 0, -0.5f);
         GL11.glTexCoord2f(tx+ts, ty);
         GL11.glVertex3f(-vo+x, -vo+y, -vo+z);   // Bottom Right Of The Texture and Quad

         GL11.glNormal3f(0, 0, -0.5f);
         GL11.glTexCoord2f(tx+ts, ty+ts);
         GL11.glVertex3f(-vo+x,  vo+y, -vo+z);   // Top Right Of The Texture and Quad

         GL11.glNormal3f(0, 0, -0.5f);
         GL11.glTexCoord2f(tx, ty+ts);
         GL11.glVertex3f( vo+x,  vo+y, -vo+z);   // Top Left Of The Texture and Quad

         GL11.glNormal3f(0, 0, -0.5f);
         GL11.glTexCoord2f(tx, ty);
         GL11.glVertex3f( vo+x, -vo+y, -vo+z);   // Bottom Left Of The Texture and Quad

        // Left Face

        GL11.glNormal3f(-0.5f, 0, 0);
        GL11.glVertex3f(-vo+x, -vo+y, -vo+z);   // Bottom Left Of The Texture and Quad
        GL11.glTexCoord2f(tx+ts, ty);

        GL11.glNormal3f(-0.5f, 0, 0);
        GL11.glVertex3f(-vo+x, -vo+y,  vo+z);   // Bottom Right Of The Texture and Quad
        GL11.glTexCoord2f(tx+ts, ty+ts);

        GL11.glNormal3f(-0.5f, 0, 0);
        GL11.glVertex3f(-vo+x,  vo+y,  vo+z);   // Top Right Of The Texture and Quad

        GL11.glTexCoord2f(tx, ty+ts);
        GL11.glVertex3f(-vo+x,  vo+y, -vo+z);   // Top Left Of The Texture and Quad

         // Bottom Face

         GL11.glNormal3f(0, -0.5f, 0);
         GL11.glTexCoord2f(tx+ts, ty+ts);
         GL11.glVertex3f(-vo+x, -vo+y, -vo+z);   // Top Right Of The Texture and Quad

         GL11.glNormal3f(0, -0.5f, 0);
         GL11.glTexCoord2f(tx, ty+ts);
         GL11.glVertex3f( vo+x, -vo+y, -vo+z);   // Top Left Of The Texture and Quad

         GL11.glNormal3f(0, -0.5f, 0);
         GL11.glTexCoord2f(tx, ty);
         GL11.glVertex3f( vo+x, -vo+y,  vo+z);   // Bottom Left Of The Texture and Quad

         GL11.glNormal3f(0, -0.5f, 0);
         GL11.glTexCoord2f(tx+ts, ty);
         GL11.glVertex3f(-vo+x, -vo+y,  vo+z);   // Bottom Right Of The Texture and Quad  */





         GL11.glEnd();

        GL11.glPopMatrix();

    }
}
