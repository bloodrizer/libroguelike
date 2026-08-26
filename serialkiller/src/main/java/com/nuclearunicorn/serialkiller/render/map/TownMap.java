package com.nuclearunicorn.serialkiller.render.map;

import com.nuclearunicorn.libroguelike.game.world.WorldChunk;
import com.nuclearunicorn.libroguelike.game.world.WorldTile;
import com.nuclearunicorn.libroguelike.game.world.layers.WorldLayer;
import com.nuclearunicorn.serialkiller.game.world.RLTile;
import com.nuclearunicorn.serialkiller.game.world.entities.EntityDoor;
import com.nuclearunicorn.serialkiller.generators.town.Building;
import com.nuclearunicorn.serialkiller.generators.town.BuildingType;
import com.nuclearunicorn.serialkiller.generators.town.GridMask;
import org.lwjgl.util.Point;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The town, one pixel per tile, as a plain ARGB raster plus a list of places worth naming.
 *
 * <p>No GL here at all. The map is a projection of the world model — which tiles you have
 * walked, which building owns each of them — and that is a question a unit test can ask as
 * easily as a renderer can, so the answer is computed as data and {@link
 * com.nuclearunicorn.serialkiller.render.overlays.MiniMap} does nothing but upload it.
 *
 * <p>Rebuilding walks the chunk's own tile map rather than calling {@code get_tile} per cell:
 * that path allocates a {@link Point} and hashes it for every one of ~16k tiles, and the map
 * is rebuilt on every turn the player takes.
 */
public final class TownMap {

    /** Widest span we will raster. A town is one 128-tile chunk; this is headroom. */
    public static final int MAX_SPAN = 256;

    /** A building the map is willing to name. */
    public static final class Landmark {
        public final BuildingType type;
        public final String label;
        /** World tile the label hangs off — the footprint's centre of mass. */
        public final int x, y;
        public final boolean home;
        /** True once the player has been inside; landmarks show up before that, dimmed. */
        public final boolean visited;
        public final Point entrance;

        Landmark(BuildingType type, String label, int x, int y,
                 boolean home, boolean visited, Point entrance) {
            this.type = type;
            this.label = label;
            this.x = x;
            this.y = y;
            this.home = home;
            this.visited = visited;
            this.entrance = entrance;
        }

        public int color() {
            return home ? MapPalette.HOME_WALL : MapPalette.wall(type);
        }
    }

    private int ox, oy, w, h;
    private int[] pixels = new int[0];
    private short[] owner = new short[0];       //0 = open air, i+1 = buildings.get(i)
    private final List<Landmark> landmarks = new ArrayList<Landmark>();

    public int originX() { return ox; }
    public int originY() { return oy; }
    public int width()   { return w; }
    public int height()  { return h; }

    /** Row-major ARGB, stride {@link #width()}. Live buffer: read it before the next build. */
    public int[] pixels() { return pixels; }

    public List<Landmark> landmarks() { return landmarks; }

    public boolean contains(int worldX, int worldY) {
        int lx = worldX - ox, ly = worldY - oy;
        return lx >= 0 && ly >= 0 && lx < w && ly < h;
    }

    public int colorAt(int worldX, int worldY) {
        return contains(worldX, worldY) ? pixels[(worldY - oy) * w + (worldX - ox)] : 0;
    }

    /**
     * Re-raster the layer. Returns false when there is nothing loaded to draw.
     *
     * @param buildings what the generator raised on this layer; empty for a basement
     * @param centreX   the player, used only to window a layer too wide to fit MAX_SPAN
     * @param reveal    ignore fog, as {@code -Dlrl.reveal} does for the world view
     */
    public boolean build(WorldLayer layer, List<Building> buildings,
                         int centreX, int centreY, boolean reveal) {
        landmarks.clear();
        if (layer == null || layer.chunk_data.isEmpty()) {
            w = 0;
            h = 0;
            return false;
        }
        if (!bound(layer, centreX, centreY)) {
            return false;
        }

        int cells = w * h;
        if (pixels.length != cells) {
            pixels = new int[cells];
            owner = new short[cells];
        }
        for (int i = 0; i < cells; i++) {
            pixels[i] = MapPalette.UNMAPPED;
            owner[i] = 0;
        }

        stampFootprints(buildings);
        boolean[] visited = new boolean[buildings.size()];
        paint(layer, buildings, visited, reveal);
        nameLandmarks(buildings, visited, reveal);
        return true;
    }

    /** Tile-space bounds of every loaded chunk, windowed onto the player if that is too big. */
    private boolean bound(WorldLayer layer, int centreX, int centreY) {
        int size = WorldChunk.CHUNK_SIZE;
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        for (Point origin : layer.chunk_data.keySet()) {
            minX = Math.min(minX, origin.getX() * size);
            minY = Math.min(minY, origin.getY() * size);
            maxX = Math.max(maxX, origin.getX() * size + size);
            maxY = Math.max(maxY, origin.getY() * size + size);
        }
        if (minX > maxX) {
            return false;
        }
        w = Math.min(MAX_SPAN, maxX - minX);
        h = Math.min(MAX_SPAN, maxY - minY);
        ox = clamp(centreX - w / 2, minX, maxX - w);
        oy = clamp(centreY - h / 2, minY, maxY - h);
        return w > 0 && h > 0;
    }

    /** Paint each building's id over the tiles it owns, so the tile walk can look it up. */
    private void stampFootprints(List<Building> buildings) {
        for (int i = 0; i < buildings.size(); i++) {
            Building building = buildings.get(i);
            GridMask mask = building.footprint;
            short id = (short) (i + 1);
            if (mask == null) {
                //no mask (nothing generates one today, but the field is nullable): the lot
                //rect is a fair stand-in, and a wrong tint beats a building that isn't there
                stampRect(building.getX(), building.getY(),
                        building.getW() + 1, building.getH() + 1, id);
                continue;
            }
            for (int ly = 0; ly < mask.h; ly++) {
                for (int lx = 0; lx < mask.w; lx++) {
                    if (mask.get(lx, ly)) {
                        put(mask.ox + lx, mask.oy + ly, id);
                    }
                }
            }
        }
    }

    private void stampRect(int x, int y, int rw, int rh, short id) {
        for (int j = 0; j < rh; j++) {
            for (int i = 0; i < rw; i++) {
                put(x + i, y + j, id);
            }
        }
    }

    private void put(int worldX, int worldY, short id) {
        int lx = worldX - ox, ly = worldY - oy;
        if (lx >= 0 && ly >= 0 && lx < w && ly < h) {
            owner[ly * w + lx] = id;
        }
    }

    private void paint(WorldLayer layer, List<Building> buildings,
                       boolean[] visited, boolean reveal) {
        for (WorldChunk chunk : layer.chunk_data.values()) {
            for (Map.Entry<Point, WorldTile> entry : chunk.tile_data.entrySet()) {
                Point at = entry.getKey();
                int lx = at.getX() - ox, ly = at.getY() - oy;
                if (lx < 0 || ly < 0 || lx >= w || ly >= h) {
                    continue;
                }
                if (!(entry.getValue() instanceof RLTile)) {
                    continue;
                }
                RLTile tile = (RLTile) entry.getValue();
                int index = ly * w + lx;
                int id = owner[index];
                Building building = id > 0 ? buildings.get(id - 1) : null;

                boolean known = reveal || tile.isExplored();
                if (known && building != null) {
                    visited[id - 1] = true;
                }
                if (known) {
                    pixels[index] = material(tile, building);
                } else if (building != null && rumoured(building)) {
                    pixels[index] = MapPalette.hearsay(material(tile, building));
                }
            }
        }
    }

    /** What this tile is made of, ignoring whether anyone has seen it. */
    private int material(RLTile tile, Building building) {
        int tint = building == null ? MapPalette.WALL
                : (building.isPlayerHome ? MapPalette.HOME_WALL : MapPalette.wall(building.type));
        if (tile.isWallGap()) {
            //windows are cut into every street-facing wall, so painting every gap bright
            //turned each house into a dashed gold rectangle and the shape was lost. Only a
            //door is worth a colour of its own: it is the tile you can actually walk through.
            return isDoor(tile) ? MapPalette.DOOR : tint;
        }
        if (tile.isWall()) {
            return tint;
        }
        if (tile.isIndoor()) {
            return building == null ? MapPalette.FLOOR
                    : MapPalette.shade(tint, MapPalette.FLOOR_AMT);
        }
        RLTile.TileType type = tile.getTileType();
        if (type == RLTile.TileType.ROAD) {
            return com.nuclearunicorn.serialkiller.render.Palette.isSidewalk(tile)
                    ? MapPalette.SIDEWALK : MapPalette.ASPHALT;
        }
        if (type == RLTile.TileType.GRASS) {
            return MapPalette.GRASS;
        }
        if (type == RLTile.TileType.WALL) {
            return MapPalette.WALL;
        }
        return MapPalette.GROUND;
    }

    private boolean isDoor(RLTile tile) {
        for (int i = 0; i < tile.ent_list.size(); i++) {
            if (tile.ent_list.get(i) instanceof EntityDoor) {
                return true;
            }
        }
        return false;
    }

    /** A place the town would tell you about without your having gone there. */
    private boolean rumoured(Building building) {
        return building.isPlayerHome || MapPalette.isLandmark(building.type);
    }

    private void nameLandmarks(List<Building> buildings, boolean[] visited, boolean reveal) {
        for (int i = 0; i < buildings.size(); i++) {
            Building building = buildings.get(i);
            //only places worth a name: a label on each of twenty visited flats is not a map
            if (!rumoured(building)) {
                continue;
            }
            long sumX = 0, sumY = 0, n = 0;
            GridMask mask = building.footprint;
            if (mask != null) {
                for (int ly = 0; ly < mask.h; ly++) {
                    for (int lx = 0; lx < mask.w; lx++) {
                        if (mask.get(lx, ly)) {
                            sumX += mask.ox + lx;
                            sumY += mask.oy + ly;
                            n++;
                        }
                    }
                }
            }
            int cx = n > 0 ? (int) (sumX / n) : building.getX() + building.getW() / 2;
            int cy = n > 0 ? (int) (sumY / n) : building.getY() + building.getH() / 2;
            if (!contains(cx, cy)) {
                continue;
            }
            landmarks.add(new Landmark(building.type,
                    building.isPlayerHome ? "HOME" : MapPalette.label(building.type),
                    cx, cy, building.isPlayerHome, visited[i] || reveal, building.entrance));
        }
    }

    private static int clamp(int v, int lo, int hi) {
        if (hi < lo) {
            return lo;
        }
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
