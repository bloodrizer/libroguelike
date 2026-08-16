// Shim: LWJGL 3 replaced GLContext.getCapabilities() with GL.getCapabilities().
// slick-util's compiled bytecode needs the LWJGL 2 ContextCapabilities return type.
package org.lwjgl.opengl;

public final class GLContext {
    private GLContext() {}

    public static ContextCapabilities getCapabilities() {
        return new ContextCapabilities(GL.getCapabilities());
    }
}
