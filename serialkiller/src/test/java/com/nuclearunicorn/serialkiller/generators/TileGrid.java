package com.nuclearunicorn.serialkiller.generators;

import com.nuclearunicorn.libroguelike.game.world.WorldTile;
import com.nuclearunicorn.libroguelike.game.world.layers.WorldLayer;
import com.nuclearunicorn.serialkiller.game.world.RLTile;
import com.nuclearunicorn.serialkiller.game.sound.SoundConfig;

/**
 * Hand-built geometry: a grid of tiles with nothing else attached.
 *
 * <p>{@link TownFixture} exists for properties of a whole finished town; these are properties
 * of the arithmetic, and a synthetic grid is the only way to state them exactly — a real town
 * cannot be asked for "a speaker two tiles from a listener with one shut door between them"
 * without first searching for somewhere that happens to look like that.
 *
 * <p>Out of bounds returns null, which the flood treats as sealed. So a barrier spanning the
 * grid really is a barrier, and sound cannot creep round its ends.
 */
public final class TileGrid {

    private TileGrid() {}

    /** All-open grid. */
    public static WorldLayer open(int w, int h) {
        return new TestLayer(w, h);
    }

    /** A full-height barrier at {@code wallX}, optionally with one gap punched in it. */
    public static WorldLayer barrier(int w, int h, int wallX, int gapY, int gapLoss, int wallLoss) {
        TestLayer layer = new TestLayer(w, h);
        for (int y = 0; y < h; y++) {
            layer.set(wallX, y, y == gapY ? gapLoss : wallLoss);
            layer.wall(wallX, y, y != gapY);
        }
        return layer;
    }

    /** An exterior wall down column 1 with a single door at row 2. */
    public static WorldLayer doorway(int gapLoss, int w) {
        return barrier(w, 5, 1, 2, gapLoss, SoundConfig.TL_WALL_OUTER);
    }

    /**
     * A grid from ASCII. {@code .} open, {@code #} interior wall, {@code =} exterior wall,
     * {@code +} shut door, {@code /} open door, {@code w} window. Any other character is
     * open floor, so letters can be used as position markers.
     */
    public static WorldLayer parse(String... rows) {
        TestLayer layer = new TestLayer(rows[0].length(), rows.length);
        for (int y = 0; y < rows.length; y++) {
            for (int x = 0; x < rows[y].length(); x++) {
                char c = rows[y].charAt(x);
                layer.set(x, y, lossOf(c));
                //a wall to sound is a wall to sight; doors and windows are holes in it
                layer.wall(x, y, c == '#' || c == '=');
            }
        }
        return layer;
    }

    public static int lossOf(char c) {
        switch (c) {
            case '#': return SoundConfig.TL_WALL_INNER;
            case '=': return SoundConfig.TL_WALL_OUTER;
            case '+': return SoundConfig.TL_DOOR_SHUT;
            case '/': return SoundConfig.TL_DOOR_OPEN;
            case 'w': return SoundConfig.TL_WINDOW;
            default:  return SoundConfig.TL_OPEN;
        }
    }

    private static final class TestLayer extends WorldLayer {
        private final int w;
        private final int h;
        private final RLTile[] tiles;

        TestLayer(int w, int h) {
            this.w = w;
            this.h = h;
            this.tiles = new RLTile[w * h];
            for (int i = 0; i < tiles.length; i++) {
                tiles[i] = new RLTile();
            }
        }

        void set(int x, int y, int loss) {
            tiles[y * w + x].setSoundLoss(loss);
        }

        void wall(int x, int y, boolean isWall) {
            tiles[y * w + x].setWall(isWall);
        }

        @Override
        public WorldTile get_tile(int x, int y) {
            if (x < 0 || y < 0 || x >= w || y >= h) {
                return null;
            }
            return tiles[y * w + x];
        }
    }
}
