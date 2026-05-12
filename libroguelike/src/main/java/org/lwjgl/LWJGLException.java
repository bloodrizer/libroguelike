// Shim: LWJGL 3 dropped this checked exception. Game code still throws/catches it.
package org.lwjgl;

public class LWJGLException extends Exception {
    public LWJGLException() {}
    public LWJGLException(String msg) { super(msg); }
    public LWJGLException(Throwable cause) { super(cause); }
    public LWJGLException(String msg, Throwable cause) { super(msg, cause); }
}
