package com.nuclearunicorn.web;

import org.teavm.jso.JSBody;
import org.teavm.jso.dom.html.HTMLImageElement;

/**
 * Read side of the asset registry the page fills before wasm starts.
 *
 * Images cannot be decoded synchronously in a browser, and the engine's texture
 * loading is synchronous, so index.html preloads everything listed in
 * assets.json and parks it on window.__assets. By the time main() runs, every
 * lookup here is a hit.
 */
public final class Assets {

    private Assets() {
    }

    /** Preloaded image by resource path, e.g. "/resources/ui/window_ui_modern.png". */
    @JSBody(params = "name",
            script = "return (window.__assets && window.__assets.images[name]) || null;")
    public static native HTMLImageElement image(String name);

    /** Preloaded text file by resource path; null when absent. */
    @JSBody(params = "name",
            script = "return (window.__assets && window.__assets.text[name]) || null;")
    public static native String text(String name);

    @JSBody(params = "msg", script = "console.log(msg);")
    public static native void log(String msg);
}
