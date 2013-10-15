package com.nuclearunicorn.negame.client.render.utils;

import org.lwjgl.opengl.ARBFragmentShader;
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.ARBVertexShader;
import org.lwjgl.opengl.GL11;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * @author Bloodrizer
 */
public class ARBShader {
    
    int vertShader;
    int fragShader;
    int program;

    public ARBShader(String vertShaderName, String fragShaderName){
        try {
            vertShader = createShader(vertShaderName, ARBVertexShader.GL_VERTEX_SHADER_ARB);
            fragShader = createShader(fragShaderName, ARBFragmentShader.GL_FRAGMENT_SHADER_ARB);

            program = ARBShaderObjects.glCreateProgramObjectARB();
            ARBShaderObjects.glAttachObjectARB(program, vertShader);
            ARBShaderObjects.glAttachObjectARB(program, fragShader);

            //todo: pixel shader there

            ARBShaderObjects.glLinkProgramARB(program);
            if (ARBShaderObjects.glGetObjectParameteriARB(program, ARBShaderObjects.GL_OBJECT_LINK_STATUS_ARB) == GL11.GL_FALSE) {
                System.err.println(getLogInfo(program));
                return;
            }

            ARBShaderObjects.glValidateProgramARB(program);
            if (ARBShaderObjects.glGetObjectParameteriARB(program, ARBShaderObjects.GL_OBJECT_VALIDATE_STATUS_ARB) == GL11.GL_FALSE) {
                System.err.println(getLogInfo(program));
                return;
            }

        } catch (Exception ex) {
            throw new RuntimeException("Failed to initialize shader", ex);
        }
    }
    
    private int createShader(String filename, int shaderType) throws Exception {
        int shader = 0;
        try {
            shader = ARBShaderObjects.glCreateShaderObjectARB(shaderType);

            if(shader == 0){
                return 0;
            }
            ARBShaderObjects.glShaderSourceARB(shader, readFileAsString(filename));
            ARBShaderObjects.glCompileShaderARB(shader);

            if (ARBShaderObjects.glGetObjectParameteriARB(shader, ARBShaderObjects.GL_OBJECT_COMPILE_STATUS_ARB) == GL11.GL_FALSE) {
                throw new RuntimeException("Error creating shader: " + getLogInfo(shader));
            }

            return shader;
        }
        catch(Exception ex) {
            ARBShaderObjects.glDeleteObjectARB(shader);
            throw ex;
        }
    }

    private static String readFileAsString(String filename) throws Exception {
        StringBuilder source = new StringBuilder();

        //FileInputStream in = new FileInputStream(filename);
        InputStream in = ARBShader.class.getResourceAsStream(filename);

        Exception exception = null;

        BufferedReader reader;
        try{
            reader = new BufferedReader(new InputStreamReader(in,"UTF-8"));
            String line;
            while((line = reader.readLine()) != null){
                source.append(line).append('\n');
            }
        } finally {
            if (in != null){
                in.close();
            }
        }

        return source.toString();
    }

    private static String getLogInfo(int obj) {
        return ARBShaderObjects.glGetInfoLogARB(obj, ARBShaderObjects.glGetObjectParameteriARB(obj, ARBShaderObjects.GL_OBJECT_INFO_LOG_LENGTH_ARB));
    }
    
    public void beginProgram(){
        ARBShaderObjects.glUseProgramObjectARB(program);
    }

    public void endProgram(){
        ARBShaderObjects.glUseProgramObjectARB(0);
    }
}
