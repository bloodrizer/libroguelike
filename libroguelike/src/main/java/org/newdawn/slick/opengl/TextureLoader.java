// Replacement for slick-util TextureLoader: decodes PNG with STBImage and
// uploads to a GL texture. Format string is ignored — STBImage auto-detects.
package org.newdawn.slick.opengl;

import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;

import static org.lwjgl.opengl.GL11.*;

public final class TextureLoader {
    private TextureLoader() {}

    public static Texture getTexture(String format, InputStream stream) throws IOException {
        ByteBuffer encoded = readAll(stream);
        try (MemoryStack stk = MemoryStack.stackPush()) {
            int[] w = new int[1], h = new int[1], comp = new int[1];
            STBImage.stbi_set_flip_vertically_on_load(false);
            ByteBuffer pixels = STBImage.stbi_load_from_memory(encoded, w, h, comp, 4);
            if (pixels == null) {
                throw new IOException("STBImage failed: " + STBImage.stbi_failure_reason());
            }
            int id = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, id);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, w[0], h[0], 0, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
            STBImage.stbi_image_free(pixels);
            // No power-of-two padding — modern GL handles NPOT natively.
            return new Texture(id, w[0], h[0], w[0], h[0]);
        }
    }

    private static ByteBuffer readAll(InputStream stream) throws IOException {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = stream.read(buf)) > 0) out.write(buf, 0, n);
        byte[] all = out.toByteArray();
        ByteBuffer bb = ByteBuffer.allocateDirect(all.length);
        bb.put(all);
        bb.flip();
        return bb;
    }
}
