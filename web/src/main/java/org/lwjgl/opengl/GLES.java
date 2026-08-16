package org.lwjgl.opengl;

import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.teavm.jso.webgl.WebGLBuffer;
import org.teavm.jso.webgl.WebGLProgram;
import org.teavm.jso.webgl.WebGLRenderingContext;
import org.teavm.jso.webgl.WebGLShader;
import org.teavm.jso.webgl.WebGLTexture;
import org.teavm.jso.webgl.WebGLUniformLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * WebGL device backing the GL 1.1 emulation in {@link GL11}.
 *
 * Holds the context, the single textured/coloured-quad program every draw goes
 * through, and the int-handle <-> WebGL object tables that let the game keep
 * using GL 1.1's integer names.
 */
public final class GLES {

    private GLES() {
    }

    public static WebGLRenderingContext gl;
    public static HTMLCanvasElement canvas;

    // One program covers every draw the game issues: optional texture x vertex colour.
    static WebGLProgram program;
    static int aPos, aUV, aColor;
    static WebGLUniformLocation uMVP, uTex, uUseTex, uPointSize;
    static WebGLBuffer vbo;

    // GL 1.1 hands out integer names; WebGL hands out objects. Index 0 stays null
    // so that handle 0 keeps its "unbound" meaning.
    static final List<WebGLTexture> textures = new ArrayList<>();
    static final List<Object> framebuffers = new ArrayList<>();

    private static final String VERT =
            "attribute vec2 aPos;\n"
          + "attribute vec2 aUV;\n"
          + "attribute vec4 aColor;\n"
          + "uniform mat4 uMVP;\n"
          + "uniform float uPointSize;\n"
          + "varying vec2 vUV;\n"
          + "varying vec4 vColor;\n"
          + "void main() {\n"
          + "  gl_Position = uMVP * vec4(aPos, 0.0, 1.0);\n"
          + "  gl_PointSize = uPointSize;\n"
          + "  vUV = aUV;\n"
          + "  vColor = aColor;\n"
          + "}\n";

    private static final String FRAG =
            "precision mediump float;\n"
          + "varying vec2 vUV;\n"
          + "varying vec4 vColor;\n"
          + "uniform sampler2D uTex;\n"
          + "uniform float uUseTex;\n"
          + "void main() {\n"
          + "  vec4 c = vColor;\n"
          + "  if (uUseTex > 0.5) { c *= texture2D(uTex, vUV); }\n"
          + "  gl_FragColor = c;\n"
          + "}\n";

    public static void init(HTMLCanvasElement c) {
        canvas = c;
        gl = (WebGLRenderingContext) c.getContext("webgl");
        if (gl == null) {
            gl = (WebGLRenderingContext) c.getContext("experimental-webgl");
        }
        if (gl == null) {
            throw new IllegalStateException("WebGL unavailable");
        }

        textures.add(null);
        framebuffers.add(null);

        program = link(VERT, FRAG);
        gl.useProgram(program);

        aPos = gl.getAttribLocation(program, "aPos");
        aUV = gl.getAttribLocation(program, "aUV");
        aColor = gl.getAttribLocation(program, "aColor");
        uMVP = gl.getUniformLocation(program, "uMVP");
        uTex = gl.getUniformLocation(program, "uTex");
        uUseTex = gl.getUniformLocation(program, "uUseTex");
        uPointSize = gl.getUniformLocation(program, "uPointSize");

        vbo = gl.createBuffer();
        gl.enableVertexAttribArray(aPos);
        gl.enableVertexAttribArray(aUV);
        gl.enableVertexAttribArray(aColor);
        gl.uniform1i(uTex, 0);
        gl.uniform1f(uPointSize, 1.0f);
    }

    private static WebGLProgram link(String vsrc, String fsrc) {
        WebGLShader vs = compile(WebGLRenderingContext.VERTEX_SHADER, vsrc);
        WebGLShader fs = compile(WebGLRenderingContext.FRAGMENT_SHADER, fsrc);
        WebGLProgram p = gl.createProgram();
        gl.attachShader(p, vs);
        gl.attachShader(p, fs);
        gl.linkProgram(p);
        if (!gl.getProgramParameterb(p, WebGLRenderingContext.LINK_STATUS)) {
            throw new IllegalStateException("program link failed: " + gl.getProgramInfoLog(p));
        }
        return p;
    }

    private static WebGLShader compile(int type, String src) {
        WebGLShader s = gl.createShader(type);
        gl.shaderSource(s, src);
        gl.compileShader(s);
        if (!gl.getShaderParameterb(s, WebGLRenderingContext.COMPILE_STATUS)) {
            throw new IllegalStateException("shader compile failed: " + gl.getShaderInfoLog(s));
        }
        return s;
    }

    /** Registers a WebGL texture under a fresh GL 1.1 integer name. */
    public static int registerTexture(WebGLTexture t) {
        textures.add(t);
        return textures.size() - 1;
    }

    static WebGLTexture texture(int id) {
        return id > 0 && id < textures.size() ? textures.get(id) : null;
    }
}
