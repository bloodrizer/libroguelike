package org.lwjgl.opengl;

import org.teavm.jso.typedarrays.Float32Array;
import org.teavm.jso.typedarrays.Uint8Array;
import org.teavm.jso.webgl.WebGLRenderingContext;
import org.teavm.jso.webgl.WebGLTexture;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * GL 1.1 fixed-function / immediate-mode emulation on top of WebGL.
 *
 * The game draws with glBegin/glVertex2f and a matrix stack, neither of which
 * WebGL has. This class keeps that API: vertices accumulate into a client-side
 * array between begin/end and flush as one glDrawArrays; the matrix stack is
 * computed here and handed to the shader as a single MVP uniform.
 *
 * Constant values are the real OpenGL ones, which WebGL shares, so anything not
 * emulated passes straight through.
 */
public final class GL11 {

    private GL11() {
    }

    // --- constants (real GL values; WebGL uses the same numbers) -------------
    public static final int GL_FALSE = 0;
    public static final int GL_TRUE = 1;
    public static final int GL_NO_ERROR = 0;

    public static final int GL_POINTS = 0x0000;
    public static final int GL_LINES = 0x0001;
    public static final int GL_LINE_LOOP = 0x0002;
    public static final int GL_LINE_STRIP = 0x0003;
    public static final int GL_TRIANGLES = 0x0004;
    public static final int GL_TRIANGLE_STRIP = 0x0005;
    public static final int GL_TRIANGLE_FAN = 0x0006;
    public static final int GL_QUADS = 0x0007;

    public static final int GL_DEPTH_BUFFER_BIT = 0x0100;
    public static final int GL_COLOR_BUFFER_BIT = 0x4000;

    public static final int GL_ZERO = 0;
    public static final int GL_ONE = 1;
    public static final int GL_SRC_COLOR = 0x0300;
    public static final int GL_SRC_ALPHA = 0x0302;
    public static final int GL_ONE_MINUS_SRC_ALPHA = 0x0303;
    public static final int GL_DST_ALPHA = 0x0304;

    public static final int GL_BLEND = 0x0BE2;
    public static final int GL_DEPTH_TEST = 0x0B71;
    public static final int GL_POINT_SMOOTH = 0x0B10;
    public static final int GL_TEXTURE_2D = 0x0DE1;
    public static final int GL_CULL_FACE = 0x0B44;

    public static final int GL_LEQUAL = 0x0203;
    public static final int GL_BACK = 0x0405;

    public static final int GL_MODELVIEW = 0x1700;
    public static final int GL_PROJECTION = 0x1701;

    public static final int GL_UNSIGNED_BYTE = 0x1401;
    public static final int GL_FLOAT = 0x1406;
    public static final int GL_RGB = 0x1907;
    public static final int GL_RGBA = 0x1908;
    public static final int GL_RGBA8 = 0x8058;

    public static final int GL_NEAREST = 0x2600;
    public static final int GL_LINEAR = 0x2601;
    public static final int GL_NEAREST_MIPMAP_NEAREST = 0x2700;
    public static final int GL_LINEAR_MIPMAP_NEAREST = 0x2701;
    public static final int GL_NEAREST_MIPMAP_LINEAR = 0x2702;
    public static final int GL_LINEAR_MIPMAP_LINEAR = 0x2703;

    public static final int GL_TEXTURE_MAG_FILTER = 0x2800;
    public static final int GL_TEXTURE_MIN_FILTER = 0x2801;
    public static final int GL_TEXTURE_WRAP_S = 0x2802;
    public static final int GL_TEXTURE_WRAP_T = 0x2803;
    public static final int GL_TEXTURE_MAX_LEVEL = 0x813D;
    public static final int GL_GENERATE_MIPMAP = 0x8191;
    public static final int GL_CLAMP = 0x2900;
    public static final int GL_REPEAT = 0x2901;
    public static final int GL_CLAMP_TO_EDGE = 0x812F;

    public static final int GL_PACK_ALIGNMENT = 0x0D05;
    public static final int GL_UNPACK_ALIGNMENT = 0x0CF5;
    public static final int GL_VIEWPORT = 0x0BA2;

    // --- immediate-mode batch ------------------------------------------------
    private static final int FLOATS_PER_VERTEX = 8; // x,y,u,v,r,g,b,a
    private static float[] batch = new float[FLOATS_PER_VERTEX * 4096];
    private static int batchLen;
    private static int primitive = -1;
    private static boolean inBegin;

    private static float curU, curV;
    private static float curR = 1, curG = 1, curB = 1, curA = 1;

    // --- fixed-function state ------------------------------------------------
    private static boolean texture2D;
    private static int boundTexture;
    private static float pointSize = 1.0f;

    private static final float[][] projStack = new float[32][];
    private static final float[][] mvStack = new float[32][];
    private static int projTop, mvTop;
    private static int matrixMode = GL_MODELVIEW;

    private static final int[] viewport = new int[]{0, 0, 1024, 768};
    private static final float[] mvp = new float[16];

    static {
        projStack[0] = identity();
        mvStack[0] = identity();
    }

    // --- matrix helpers ------------------------------------------------------
    private static float[] identity() {
        float[] m = new float[16];
        m[0] = m[5] = m[10] = m[15] = 1;
        return m;
    }

    private static float[] current() {
        return matrixMode == GL_PROJECTION ? projStack[projTop] : mvStack[mvTop];
    }

    private static void setCurrent(float[] m) {
        if (matrixMode == GL_PROJECTION) {
            projStack[projTop] = m;
        } else {
            mvStack[mvTop] = m;
        }
    }

    /** Column-major multiply, matching OpenGL's convention: out = a * b. */
    private static float[] mul(float[] a, float[] b) {
        float[] o = new float[16];
        for (int c = 0; c < 4; c++) {
            for (int r = 0; r < 4; r++) {
                float s = 0;
                for (int k = 0; k < 4; k++) {
                    s += a[k * 4 + r] * b[c * 4 + k];
                }
                o[c * 4 + r] = s;
            }
        }
        return o;
    }

    private static void applyToCurrent(float[] m) {
        setCurrent(mul(current(), m));
    }

    // --- matrix API ----------------------------------------------------------
    public static void glMatrixMode(int mode) {
        matrixMode = mode;
    }

    public static void glLoadIdentity() {
        setCurrent(identity());
    }

    public static void glPushMatrix() {
        if (matrixMode == GL_PROJECTION) {
            projStack[projTop + 1] = projStack[projTop].clone();
            projTop++;
        } else {
            mvStack[mvTop + 1] = mvStack[mvTop].clone();
            mvTop++;
        }
    }

    public static void glPopMatrix() {
        if (matrixMode == GL_PROJECTION) {
            if (projTop > 0) {
                projTop--;
            }
        } else if (mvTop > 0) {
            mvTop--;
        }
    }

    public static void glOrtho(double l, double r, double b, double t, double n, double f) {
        float[] m = identity();
        m[0] = (float) (2.0 / (r - l));
        m[5] = (float) (2.0 / (t - b));
        m[10] = (float) (-2.0 / (f - n));
        m[12] = (float) (-(r + l) / (r - l));
        m[13] = (float) (-(t + b) / (t - b));
        m[14] = (float) (-(f + n) / (f - n));
        applyToCurrent(m);
    }

    public static void glTranslatef(float x, float y, float z) {
        float[] m = identity();
        m[12] = x;
        m[13] = y;
        m[14] = z;
        applyToCurrent(m);
    }

    public static void glScalef(float x, float y, float z) {
        float[] m = identity();
        m[0] = x;
        m[5] = y;
        m[10] = z;
        applyToCurrent(m);
    }

    public static void glRotatef(float angle, float x, float y, float z) {
        double rad = Math.toRadians(angle);
        float c = (float) Math.cos(rad);
        float s = (float) Math.sin(rad);
        float len = (float) Math.sqrt(x * x + y * y + z * z);
        if (len == 0) {
            return;
        }
        x /= len;
        y /= len;
        z /= len;
        float ic = 1 - c;
        float[] m = identity();
        m[0] = x * x * ic + c;
        m[1] = y * x * ic + z * s;
        m[2] = x * z * ic - y * s;
        m[4] = x * y * ic - z * s;
        m[5] = y * y * ic + c;
        m[6] = y * z * ic + x * s;
        m[8] = x * z * ic + y * s;
        m[9] = y * z * ic - x * s;
        m[10] = z * z * ic + c;
        applyToCurrent(m);
    }

    public static void glMultMatrixf(FloatBuffer m) {
        float[] a = new float[16];
        int p = m.position();
        for (int i = 0; i < 16; i++) {
            a[i] = m.get(p + i);
        }
        applyToCurrent(a);
    }

    public static void glMultMatrixf(float[] m) {
        applyToCurrent(m);
    }

    // --- immediate mode ------------------------------------------------------
    public static void glBegin(int mode) {
        primitive = mode;
        batchLen = 0;
        inBegin = true;
    }

    public static void glEnd() {
        inBegin = false;
        flush();
    }

    public static void glColor4f(float r, float g, float b, float a) {
        curR = r;
        curG = g;
        curB = b;
        curA = a;
    }

    public static void glColor3f(float r, float g, float b) {
        glColor4f(r, g, b, 1.0f);
    }

    public static void glTexCoord2f(float u, float v) {
        curU = u;
        curV = v;
    }

    public static void glVertex2f(float x, float y) {
        if (batchLen + FLOATS_PER_VERTEX > batch.length) {
            float[] bigger = new float[batch.length * 2];
            System.arraycopy(batch, 0, bigger, 0, batchLen);
            batch = bigger;
        }
        batch[batchLen++] = x;
        batch[batchLen++] = y;
        batch[batchLen++] = curU;
        batch[batchLen++] = curV;
        batch[batchLen++] = curR;
        batch[batchLen++] = curG;
        batch[batchLen++] = curB;
        batch[batchLen++] = curA;
    }

    public static void glVertex3f(float x, float y, float z) {
        glVertex2f(x, y);
    }

    public static void glRectf(float x1, float y1, float x2, float y2) {
        glBegin(GL_QUADS);
        glVertex2f(x1, y1);
        glVertex2f(x2, y1);
        glVertex2f(x2, y2);
        glVertex2f(x1, y2);
        glEnd();
    }

    /**
     * Uploads the batch and issues one draw. GL_QUADS has no WebGL equivalent,
     * so quads are expanded to two triangles here.
     */
    private static void flush() {
        int verts = batchLen / FLOATS_PER_VERTEX;
        if (verts == 0) {
            return;
        }
        WebGLRenderingContext gl = GLES.gl;

        float[] data;
        int drawMode;
        int drawCount;

        if (primitive == GL_QUADS) {
            int quads = verts / 4;
            data = new float[quads * 6 * FLOATS_PER_VERTEX];
            int o = 0;
            for (int q = 0; q < quads; q++) {
                int base = q * 4 * FLOATS_PER_VERTEX;
                o = copyVertex(data, o, base, 0);
                o = copyVertex(data, o, base, 1);
                o = copyVertex(data, o, base, 2);
                o = copyVertex(data, o, base, 0);
                o = copyVertex(data, o, base, 2);
                o = copyVertex(data, o, base, 3);
            }
            drawMode = WebGLRenderingContext.TRIANGLES;
            drawCount = quads * 6;
        } else {
            data = new float[batchLen];
            System.arraycopy(batch, 0, data, 0, batchLen);
            drawMode = primitive;
            drawCount = verts;
        }

        gl.bindBuffer(WebGLRenderingContext.ARRAY_BUFFER, GLES.vbo);
        // copyFromJavaArray, not fromJavaArray: the latter is @JSByRef, which Wasm GC cannot do.
        gl.bufferData(WebGLRenderingContext.ARRAY_BUFFER,
                Float32Array.copyFromJavaArray(data), WebGLRenderingContext.STREAM_DRAW);

        int stride = FLOATS_PER_VERTEX * 4;
        gl.vertexAttribPointer(GLES.aPos, 2, WebGLRenderingContext.FLOAT, false, stride, 0);
        gl.vertexAttribPointer(GLES.aUV, 2, WebGLRenderingContext.FLOAT, false, stride, 8);
        gl.vertexAttribPointer(GLES.aColor, 4, WebGLRenderingContext.FLOAT, false, stride, 16);

        float[] m = mul(projStack[projTop], mvStack[mvTop]);
        System.arraycopy(m, 0, mvp, 0, 16);
        gl.uniformMatrix4fv(GLES.uMVP, false, mvp);

        boolean useTex = texture2D && boundTexture > 0;
        gl.uniform1f(GLES.uUseTex, useTex ? 1.0f : 0.0f);
        gl.uniform1f(GLES.uPointSize, pointSize);

        gl.drawArrays(drawMode, 0, drawCount);
        batchLen = 0;
    }

    private static int copyVertex(float[] dst, int o, int base, int idx) {
        System.arraycopy(batch, base + idx * FLOATS_PER_VERTEX, dst, o, FLOATS_PER_VERTEX);
        return o + FLOATS_PER_VERTEX;
    }

    // --- state ---------------------------------------------------------------
    public static void glEnable(int cap) {
        if (cap == GL_TEXTURE_2D) {
            texture2D = true;
            return;
        }
        if (cap == GL_POINT_SMOOTH) {
            return; // always on in WebGL; no toggle exists
        }
        GLES.gl.enable(cap);
    }

    public static void glDisable(int cap) {
        if (cap == GL_TEXTURE_2D) {
            texture2D = false;
            return;
        }
        if (cap == GL_POINT_SMOOTH) {
            return;
        }
        GLES.gl.disable(cap);
    }

    public static void glBlendFunc(int src, int dst) {
        GLES.gl.blendFunc(src, dst);
    }

    public static void glClear(int mask) {
        GLES.gl.clear(mask);
    }

    public static void glClearColor(float r, float g, float b, float a) {
        GLES.gl.clearColor(r, g, b, a);
    }

    public static void glClearDepth(double d) {
        GLES.gl.clearDepth((float) d);
    }

    public static void glDepthFunc(int f) {
        GLES.gl.depthFunc(f);
    }

    public static void glViewport(int x, int y, int w, int h) {
        viewport[0] = x;
        viewport[1] = y;
        viewport[2] = w;
        viewport[3] = h;
        GLES.gl.viewport(x, y, w, h);
    }

    public static void glLineWidth(float w) {
        GLES.gl.lineWidth(w);
    }

    public static void glPointSize(float s) {
        pointSize = s;
    }

    public static void glPixelStorei(int pname, int param) {
        GLES.gl.pixelStorei(pname, param);
    }

    public static void glReadBuffer(int mode) {
        // Single-buffered WebGL canvas; nothing to select.
    }

    public static int glGetError() {
        return GLES.gl.getError();
    }

    public static void glGetIntegerv(int pname, IntBuffer params) {
        if (pname == GL_VIEWPORT) {
            int p = params.position();
            for (int i = 0; i < 4; i++) {
                params.put(p + i, viewport[i]);
            }
        }
    }

    public static void glGetInteger(int pname, IntBuffer params) {
        glGetIntegerv(pname, params);
    }

    // --- textures ------------------------------------------------------------
    public static int glGenTextures() {
        return GLES.registerTexture(GLES.gl.createTexture());
    }

    public static void glGenTextures(IntBuffer out) {
        int p = out.position();
        for (int i = p; i < out.limit(); i++) {
            out.put(i, glGenTextures());
        }
    }

    public static void glBindTexture(int target, int id) {
        boundTexture = id;
        WebGLTexture t = GLES.texture(id);
        GLES.gl.bindTexture(target, t);
    }

    public static void glTexParameteri(int target, int pname, int param) {
        // GL 1.x mipmap auto-generation and max-level have no WebGL 1 equivalent.
        if (pname == GL_GENERATE_MIPMAP || pname == GL_TEXTURE_MAX_LEVEL) {
            return;
        }
        GLES.gl.texParameteri(target, pname, param);
    }

    public static void glTexImage2D(int target, int level, int internalFormat,
                                    int w, int h, int border,
                                    int format, int type, ByteBuffer pixels) {
        GLES.gl.texImage2D(target, level, normalizeFormat(internalFormat),
                w, h, border, format, type, toUint8(pixels));
    }

    public static void glTexImage2D(int target, int level, int internalFormat,
                                    int w, int h, int border,
                                    int format, int type, IntBuffer pixels) {
        // Callers that build RGBA as packed ints (the font atlas) hand us an
        // IntBuffer view; re-read it as bytes in the buffer's own byte order.
        int n = pixels.limit() - pixels.position();
        Uint8Array out = Uint8Array.create(n * 4);
        for (int i = 0; i < n; i++) {
            int v = pixels.get(pixels.position() + i);
            out.set(i * 4, (short) (v & 0xFF));
            out.set(i * 4 + 1, (short) ((v >>> 8) & 0xFF));
            out.set(i * 4 + 2, (short) ((v >>> 16) & 0xFF));
            out.set(i * 4 + 3, (short) ((v >>> 24) & 0xFF));
        }
        GLES.gl.texImage2D(target, level, normalizeFormat(internalFormat),
                w, h, border, format, type, out);
    }

    /**
     * Copies [position, limit) into a fresh typed array.
     *
     * Handing java.nio buffers straight to WebGL relies on a by-reference view
     * that Wasm GC does not provide, and it silently uploads nothing; copying is
     * unambiguous and only happens on texture upload.
     */
    private static Uint8Array toUint8(ByteBuffer buf) {
        int n = buf.limit() - buf.position();
        Uint8Array out = Uint8Array.create(n);
        int p = buf.position();
        for (int i = 0; i < n; i++) {
            out.set(i, (short) (buf.get(p + i) & 0xFF));
        }
        return out;
    }

    /** WebGL 1 rejects sized internal formats such as GL_RGBA8. */
    private static int normalizeFormat(int f) {
        return f == GL_RGBA8 ? GL_RGBA : f;
    }

    public static void glReadPixels(int x, int y, int w, int h,
                                    int format, int type, ByteBuffer pixels) {
        GLES.gl.readPixels(x, y, w, h, format, type, Uint8Array.fromJavaBuffer(pixels));
    }

    public static void glDeleteTextures(int id) {
        WebGLTexture t = GLES.texture(id);
        if (t != null) {
            GLES.gl.deleteTexture(t);
        }
    }

    /** Drains any batch left open, so a frame never ends mid-primitive. */
    public static void endFrame() {
        if (inBegin) {
            inBegin = false;
            flush();
        }
    }
}
