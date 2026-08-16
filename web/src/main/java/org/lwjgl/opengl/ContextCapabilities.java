package org.lwjgl.opengl;

/**
 * Web shim. WebGL 1 always provides buffer objects and framebuffers as core
 * features, so the extension probes the engine performs all answer true; the
 * debug-output extensions have no browser equivalent.
 */
public final class ContextCapabilities {

    public final boolean GL_ARB_vertex_buffer_object = true;
    public final boolean GL_EXT_framebuffer_object = true;
    public final boolean GL_EXT_texture_mirror_clamp = false;
    public final boolean GL_ARB_debug_output = false;
    public final boolean GL_AMD_debug_output = false;
}
