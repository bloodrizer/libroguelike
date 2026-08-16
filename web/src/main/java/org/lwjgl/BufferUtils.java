package org.lwjgl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * Web replacement for LWJGL's BufferUtils. Direct buffers are used so TeaVM can
 * hand them to WebGL as typed-array views without copying.
 */
public final class BufferUtils {

    private BufferUtils() {
    }

    public static ByteBuffer createByteBuffer(int capacity) {
        return ByteBuffer.allocateDirect(capacity).order(ByteOrder.nativeOrder());
    }

    public static IntBuffer createIntBuffer(int capacity) {
        return createByteBuffer(capacity * 4).asIntBuffer();
    }

    public static FloatBuffer createFloatBuffer(int capacity) {
        return createByteBuffer(capacity * 4).asFloatBuffer();
    }
}
