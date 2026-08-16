package org.lwjgl.opengl;

/** Web shim matching the LWJGL 2 capability-probe entry point. */
public final class GLContext {

    private GLContext() {
    }

    private static final ContextCapabilities CAPS = new ContextCapabilities();

    public static ContextCapabilities getCapabilities() {
        return CAPS;
    }
}
