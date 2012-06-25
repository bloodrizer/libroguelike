/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.negame.client.render.utils;

import com.nuclearunicorn.libroguelike.utils.NLTimer;
import org.lwjgl.opengl.ARBVertexBufferObject;
import org.lwjgl.opengl.GL11;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 *
 * @author Red
 */
public class VBO {

    static int floatSize = 4;
    static int vertexPositionAttributeSize = ((3+3+2) * floatSize);   //3 position coord + 3 normal + 2 texture coord
    int vertexIndexSize = 4;

    static int vboidDataBufferID;
    static int vboidIndexBufferID;

    public static int VBO_buffer_size;

    public static volatile ByteBuffer vertexPositionAttributes;
    public static volatile ByteBuffer vertexIndecies;

    public int vertex_index = 0;

    //32*32*32 * 4 * 6
    public static final  int VBO_max_buffer_size = 32000 * 4 * 6;

    public VBO(){
        this.allocateBuffer();
    }

    /**
     * This method ensures that buffer is initialized and correctly filled
     */
    public void allocateBuffer( ){
        vertexPositionAttributes   = ByteBuffer.
                allocateDirect(vertexPositionAttributeSize * VBO_max_buffer_size).
                order(ByteOrder.nativeOrder());
        vertexIndecies             = ByteBuffer.
                allocateDirect(vertexIndexSize * VBO_max_buffer_size).
                order(ByteOrder.nativeOrder());

        vboidDataBufferID =   createVBOID();
        vboidIndexBufferID = createVBOID();
    }

    public static int createVBOID() {
        return ARBVertexBufferObject.glGenBuffersARB();
    }


    public ByteBuffer get_vpa(){
        return vertexPositionAttributes;
    }

    public ByteBuffer get_vi(){
        return vertexIndecies;
    }

    private static void uploadBufferData(int vbo_id, int TYPE, ByteBuffer buffer){
        ARBVertexBufferObject.glBindBufferARB(TYPE, vbo_id);
        //NLTimer.pop("glBindBufferARB");
        
        ARBVertexBufferObject.glBufferDataARB(TYPE, buffer, ARBVertexBufferObject.GL_STATIC_DRAW_ARB);
        //NLTimer.pop("glBufferDataARB");

        //7 ms flickering there
    }

    //--------------------------------------------------------------------------
    //  Обновить vbo из нового буфера
    //--------------------------------------------------------------------------
    public void flushVBO(){
        System.out.println("updating invalidated VBO");

        NLTimer timer = new NLTimer();
        timer.push();
        uploadBufferData(vboidIndexBufferID, ARBVertexBufferObject.GL_ELEMENT_ARRAY_BUFFER_ARB, vertexIndecies);
        timer.pop("streamed vboidIndexBufferID of " + VBO_buffer_size);

        uploadBufferData(vboidDataBufferID, ARBVertexBufferObject.GL_ARRAY_BUFFER_ARB, vertexPositionAttributes);
        timer.pop("streamed vboidDataBufferID of " + VBO_buffer_size);

    }


    public void render(){
        /*if (texture != null){
            texture.bind();
        }*/

        if (vboidDataBufferID == 0 || vboidIndexBufferID == 0){
            System.err.println("vbo data/index buffer is empty, skipping...");
            return;
        }
        //System.out.println("rendering buffer of size " + VBO_buffer_size);

        GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
        GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
        GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);

        ARBVertexBufferObject.glBindBufferARB(ARBVertexBufferObject.GL_ARRAY_BUFFER_ARB, vboidDataBufferID);
        ARBVertexBufferObject.glBindBufferARB(ARBVertexBufferObject.GL_ELEMENT_ARRAY_BUFFER_ARB, vboidIndexBufferID);

        int stride = (3+3+2) * 4;   //3 vertex + 2 texture

        int offset = 0 * 4;
        GL11.glVertexPointer(3, GL11.GL_FLOAT, stride, offset);

        offset = 3 * 4;
        GL11.glNormalPointer(GL11.GL_FLOAT, stride, offset);

        // 3 vertex coord + 3 normal coord * size of float
        offset = (3+3) * 4;
        GL11.glTexCoordPointer(2, GL11.GL_FLOAT, stride, offset);
        GL11.glDrawElements(GL11.GL_QUADS, VBO_buffer_size, GL11.GL_UNSIGNED_INT, 0);

        //ARBVertexBufferObject.glBindBufferARB(ARBVertexBufferObject.GL_ARRAY_BUFFER_ARB, 0);
	    //ARBVertexBufferObject.glBindBufferARB(ARBVertexBufferObject.GL_ELEMENT_ARRAY_BUFFER_ARB, 0);

        GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
        GL11.glDisableClientState(GL11.GL_NORMAL_ARRAY);
        GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
    }

}
