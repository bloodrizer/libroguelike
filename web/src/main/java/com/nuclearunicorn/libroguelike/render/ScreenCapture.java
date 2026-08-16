package com.nuclearunicorn.libroguelike.render;

/**
 * Web build: screenshot capture is a headless-desktop debugging aid driven by
 * -Dlrl.capture.* and writes PNGs to disk, neither of which exists here.
 */
public final class ScreenCapture {

    private ScreenCapture() {
    }

    public static boolean isArmed() {
        return false;
    }

    public static void tick() {
    }

    public static void save(String path) {
    }
}
