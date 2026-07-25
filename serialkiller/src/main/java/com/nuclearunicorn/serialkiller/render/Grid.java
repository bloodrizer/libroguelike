package com.nuclearunicorn.serialkiller.render;

import com.nuclearunicorn.libroguelike.game.world.WorldViewCamera;
import com.nuclearunicorn.libroguelike.render.WindowRender;

/**
 * Tile grid <-> screen projection. Screen space here is the world space the
 * camera translate is applied to, so tile (i,j) is always at (i*CELL, j*CELL)
 * and the camera does the scrolling.
 */
public final class Grid {

    private Grid() {
    }

    public static int cellX(int i) {
        return i * RenderConfig.CELL;
    }

    public static int cellY(int j) {
        return j * RenderConfig.CELL;
    }

    /** Top edge of the 2:3 object box standing in row j (overhangs upwards). */
    public static int boxTop(int j) {
        return j * RenderConfig.CELL - RenderConfig.riseH();
    }

    /** Bottom edge of everything that stands in row j. */
    public static int boxBottom(int j) {
        return (j + 1) * RenderConfig.CELL;
    }

    // --- visible window, in tile coords ------------------------------------

    public static int firstVisibleX() {
        return (int) Math.floor(WorldViewCamera.camera_x / RenderConfig.CELL) - 1;
    }

    public static int lastVisibleX() {
        return (int) Math.ceil((WorldViewCamera.camera_x + WindowRender.get_window_w())
                / RenderConfig.CELL) + 1;
    }

    public static int firstVisibleY() {
        // one extra row above: tall objects there overhang into view
        return (int) Math.floor(WorldViewCamera.camera_y / RenderConfig.CELL) - 2;
    }

    public static int lastVisibleY() {
        return (int) Math.ceil((WorldViewCamera.camera_y + WindowRender.get_window_h())
                / RenderConfig.CELL) + 1;
    }

    public static boolean onScreen(int i, int j) {
        return i >= firstVisibleX() && i <= lastVisibleX()
                && j >= firstVisibleY() && j <= lastVisibleY();
    }
}
