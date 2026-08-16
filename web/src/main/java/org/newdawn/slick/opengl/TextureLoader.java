package org.newdawn.slick.opengl;

import com.nuclearunicorn.web.Assets;
import org.lwjgl.opengl.GLES;
import org.teavm.jso.dom.html.HTMLImageElement;
import org.teavm.jso.typedarrays.Uint8Array;
import org.teavm.jso.webgl.WebGLRenderingContext;
import org.teavm.jso.webgl.WebGLTexture;

import java.io.IOException;
import java.io.InputStream;

/**
 * Web replacement for slick-util's TextureLoader.
 *
 * PNG decoding is left to the browser: the page has already loaded each image,
 * so uploading is a direct texImage2D from the HTMLImageElement rather than a
 * decode inside wasm.
 */
public final class TextureLoader {

    private TextureLoader() {
    }

    /** Uploads a preloaded image by its classpath-style resource name. */
    public static Texture getTexture(String name) throws IOException {
        HTMLImageElement img = Assets.image(name);
        if (img == null) {
            throw new IOException("asset not preloaded: " + name);
        }
        return upload(img);
    }

    /**
     * Kept for source compatibility with the desktop callsites. The browser build
     * never has a usable stream, so callers must go through the name-based form.
     */
    public static Texture getTexture(String format, InputStream in) throws IOException {
        throw new IOException("stream texture loading is unavailable in the web build");
    }

    /** Last-resort 1x1 opaque-magenta texture, so a draw never sees a null handle. */
    public static Texture blank() {
        WebGLRenderingContext gl = GLES.gl;
        WebGLTexture tex = gl.createTexture();
        int id = GLES.registerTexture(tex);
        gl.bindTexture(WebGLRenderingContext.TEXTURE_2D, tex);
        Uint8Array px = Uint8Array.create(4);
        px.set(0, (short) 255);
        px.set(1, (short) 0);
        px.set(2, (short) 255);
        px.set(3, (short) 255);
        gl.texImage2D(WebGLRenderingContext.TEXTURE_2D, 0, WebGLRenderingContext.RGBA,
                1, 1, 0, WebGLRenderingContext.RGBA, WebGLRenderingContext.UNSIGNED_BYTE, px);
        gl.texParameteri(WebGLRenderingContext.TEXTURE_2D,
                WebGLRenderingContext.TEXTURE_MIN_FILTER, WebGLRenderingContext.NEAREST);
        gl.texParameteri(WebGLRenderingContext.TEXTURE_2D,
                WebGLRenderingContext.TEXTURE_MAG_FILTER, WebGLRenderingContext.NEAREST);
        return new Texture(id, 1, 1, 1, 1);
    }

    private static Texture upload(HTMLImageElement img) {
        WebGLRenderingContext gl = GLES.gl;
        WebGLTexture tex = gl.createTexture();
        int id = GLES.registerTexture(tex);

        gl.bindTexture(WebGLRenderingContext.TEXTURE_2D, tex);
        gl.pixelStorei(WebGLRenderingContext.UNPACK_FLIP_Y_WEBGL, 0);
        gl.pixelStorei(WebGLRenderingContext.UNPACK_PREMULTIPLY_ALPHA_WEBGL, 0);
        gl.texImage2D(WebGLRenderingContext.TEXTURE_2D, 0,
                WebGLRenderingContext.RGBA, WebGLRenderingContext.RGBA,
                WebGLRenderingContext.UNSIGNED_BYTE, img);
        // NPOT textures in WebGL 1 must clamp and cannot mipmap.
        gl.texParameteri(WebGLRenderingContext.TEXTURE_2D,
                WebGLRenderingContext.TEXTURE_MIN_FILTER, WebGLRenderingContext.LINEAR);
        gl.texParameteri(WebGLRenderingContext.TEXTURE_2D,
                WebGLRenderingContext.TEXTURE_MAG_FILTER, WebGLRenderingContext.LINEAR);
        gl.texParameteri(WebGLRenderingContext.TEXTURE_2D,
                WebGLRenderingContext.TEXTURE_WRAP_S, WebGLRenderingContext.CLAMP_TO_EDGE);
        gl.texParameteri(WebGLRenderingContext.TEXTURE_2D,
                WebGLRenderingContext.TEXTURE_WRAP_T, WebGLRenderingContext.CLAMP_TO_EDGE);

        int w = img.getWidth();
        int h = img.getHeight();
        return new Texture(id, w, h, w, h);
    }
}
