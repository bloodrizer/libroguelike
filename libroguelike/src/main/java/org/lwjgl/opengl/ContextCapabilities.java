// Shim: LWJGL 2's ContextCapabilities. slick-util's bytecode references its
// fields directly (e.g. GL_EXT_texture_mirror_clamp). Game code reads its own
// set of caps via GLContext.getCapabilities() too.
package org.lwjgl.opengl;

public final class ContextCapabilities {
    public final boolean GL_ARB_vertex_buffer_object;
    public final boolean GL_ARB_debug_output;
    public final boolean GL_AMD_debug_output;
    public final boolean GL_EXT_framebuffer_object;
    public final boolean GL_EXT_texture_mirror_clamp;

    ContextCapabilities(GLCapabilities c) {
        this.GL_ARB_vertex_buffer_object  = c.GL_ARB_vertex_buffer_object;
        this.GL_ARB_debug_output          = c.GL_ARB_debug_output;
        this.GL_AMD_debug_output          = c.GL_AMD_debug_output;
        this.GL_EXT_framebuffer_object    = c.GL_EXT_framebuffer_object;
        this.GL_EXT_texture_mirror_clamp  = c.GL_EXT_texture_mirror_clamp;
    }
}
