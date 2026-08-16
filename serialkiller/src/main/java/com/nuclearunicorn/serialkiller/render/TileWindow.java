package com.nuclearunicorn.serialkiller.render;

import com.nuclearunicorn.libroguelike.game.world.layers.WorldLayer;
import com.nuclearunicorn.serialkiller.game.world.RLTile;

/**
 * One frame's worth of visible tiles, snapshotted into a flat array.
 *
 * WorldLayer.get_tile() is a pooled-Point hash lookup; the renderer needs every
 * tile plus its four neighbours (wall connectivity, light smoothing), so it
 * pays that cost once per cell and reads the array afterwards.
 */
public final class TileWindow {

    private int x0, y0, w, h;
    private RLTile[] tiles = new RLTile[0];

    public void refresh(WorldLayer layer, int fromX, int fromY, int toX, int toY) {
        // one cell of slack on every side so neighbour queries at the border
        // still see real tiles
        x0 = fromX - 1;
        y0 = fromY - 1;
        w = (toX - fromX) + 3;
        h = (toY - fromY) + 3;

        if (tiles.length < w * h) {
            tiles = new RLTile[w * h];
        }
        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                Object tile = layer.get_tile(x0 + i, y0 + j);
                tiles[j * w + i] = (tile instanceof RLTile) ? (RLTile) tile : null;
            }
        }
    }

    public RLTile at(int i, int j) {
        int lx = i - x0;
        int ly = j - y0;
        if (lx < 0 || ly < 0 || lx >= w || ly >= h) {
            return null;
        }
        return tiles[ly * w + lx];
    }

    public boolean isWall(int i, int j) {
        RLTile tile = at(i, j);
        return tile != null && tile.isWall();
    }

    /**
     * Walls only connect visually to walls the player knows about. A door or
     * window gap counts: the slab baked into its sprite continues the run.
     */
    public boolean isKnownWall(int i, int j) {
        RLTile tile = at(i, j);
        if (tile == null || (!tile.isExplored() && !tile.isVisible())) {
            return false;
        }
        return tile.isWall() || tile.isWallGap();
    }

    /**
     * True when the wall (or wall gap) at i,j is part of an east-west run, i.e.
     * the slab's front face is what the player sees. Used to orient doors and
     * windows so they sit in the wall instead of across it.
     */
    public boolean isWallRunHorizontal(int i, int j) {
        boolean ew = isKnownWall(i - 1, j) || isKnownWall(i + 1, j);
        boolean ns = isKnownWall(i, j - 1) || isKnownWall(i, j + 1);
        return ew || !ns;
    }

    public boolean isVisible(int i, int j) {
        RLTile tile = at(i, j);
        return tile != null && tile.isVisible();
    }

    public boolean isOpaque(int i, int j) {
        RLTile tile = at(i, j);
        return tile == null || tile.isWall();
    }
}
