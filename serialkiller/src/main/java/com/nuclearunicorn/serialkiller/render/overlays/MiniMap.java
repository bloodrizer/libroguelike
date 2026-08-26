package com.nuclearunicorn.serialkiller.render.overlays;

import com.nuclearunicorn.libroguelike.game.player.Player;
import com.nuclearunicorn.libroguelike.core.client.ClientGameEnvironment;
import com.nuclearunicorn.libroguelike.game.world.layers.WorldLayer;
import com.nuclearunicorn.libroguelike.render.WindowRender;
import com.nuclearunicorn.libroguelike.utils.Timer;
import com.nuclearunicorn.serialkiller.game.ai.llm.sense.GameTurn;
import com.nuclearunicorn.serialkiller.game.world.RLWorldModel;
import com.nuclearunicorn.serialkiller.generators.town.Building;
import com.nuclearunicorn.serialkiller.generators.town.BuildingType;
import com.nuclearunicorn.serialkiller.render.Draw;
import com.nuclearunicorn.serialkiller.render.RenderConfig;
import com.nuclearunicorn.serialkiller.render.map.MapPalette;
import com.nuclearunicorn.serialkiller.render.map.TownMap;
import org.lwjgl.BufferUtils;
import org.lwjgl.util.Point;
import org.newdawn.slick.Color;
import org.newdawn.slick.TrueTypeFont;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.GL_LINE_LOOP;
import static org.lwjgl.opengl.GL11.GL_NEAREST;
import static org.lwjgl.opengl.GL11.GL_QUADS;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glColor4f;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glLineWidth;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL11.glVertex2f;

/**
 * The town map: a corner plate that is always up, and the same raster blown up full screen
 * on M.
 *
 * <p>A serial killer's game is a game about a specific street. Everything in it — where the
 * girls work, which house has the police in it, how far the bank is from your own front door
 * — is spatial, and until now the only way to hold it was to remember the walk. The map is
 * where that knowledge lives.
 *
 * <p>What it shows is deliberately not everything. Streets and houses appear as you walk
 * them, off {@code RLTile.isExplored} — the same memory the world view dims unlit tiles with.
 * Public buildings are the exception: a town knows where its own bank is, so landmarks are
 * drawn from the start at a fraction of their brightness, and come up to full once you have
 * actually been inside. Your own flat is known for the obvious reason.
 *
 * <p>The raster itself is {@link TownMap}, which knows no GL; this class uploads it as one
 * texture per turn and draws quads. Rebuilding per frame would be 16k tile reads for a
 * picture that only changes when the player moves.
 */
public final class MiniMap {

    private MiniMap() {}

    /** {@code -Dlrl.minimap=false} takes the corner plate off, for clean screenshots. */
    private static final boolean ENABLED = flag("lrl.minimap", true);
    /** {@code -Dlrl.map=true} opens the full sheet at start, for an offscreen capture. */
    private static boolean full = Boolean.getBoolean("lrl.map");

    private static final int MARGIN = 12;
    private static final int PAD = 6;
    /** Corner plate: as many whole pixels per tile as fit in this box, never fewer than one. */
    private static final int MINI_BOX = 160;
    /** Clears the clock plate and the version stamp above it. */
    private static final int TOP = 108;

    private static final int SHEET_MARGIN_X = 40;
    private static final int SHEET_MARGIN_Y = 44;
    private static final int LEGEND_W = 186;
    private static final int LEGEND_GAP = 22;

    private static final Color LABEL_SHADOW = new Color(0, 0, 0, 200);

    private static final TownMap map = new TownMap();

    private static int textureId = 0;
    private static ByteBuffer upload;
    private static WorldLayer builtLayer;
    private static long builtTurn = Long.MIN_VALUE;
    private static int builtX, builtY;
    private static boolean builtReveal;
    private static boolean valid;

    /** M. */
    public static void toggle() {
        full = !full;
    }

    public static boolean isFull() {
        return full;
    }

    public static void close() {
        full = false;
    }

    /** A new game builds a new world; drop the raster cut from the last one. */
    public static void reset() {
        builtLayer = null;
        builtTurn = Long.MIN_VALUE;
        valid = false;
        full = Boolean.getBoolean("lrl.map");
    }

    public static void render() {
        if (!ENABLED && !full) {
            return;
        }
        if (Player.get_ent() == null) {
            return;
        }
        refresh();
        if (!valid) {
            return;
        }
        if (full) {
            sheet();        //the sheet is the same map, bigger: two of them is just clutter
        } else if (ENABLED) {
            corner();
        }
    }

    // ----------------------------------------------------------------- raster

    private static void refresh() {
        WorldLayer layer = Player.get_ent().getLayer();
        long turn = GameTurn.current();
        int px = Player.get_ent().x();
        int py = Player.get_ent().y();
        //what the map is cut from: the layer under foot, the fog as of this turn, and where
        //the player stands, which only matters on a layer too wide for one raster
        if (valid && layer == builtLayer && turn == builtTurn && px == builtX && py == builtY
                && RenderConfig.REVEAL == builtReveal) {
            return;
        }
        builtLayer = layer;
        builtTurn = turn;
        builtX = px;
        builtY = py;
        builtReveal = RenderConfig.REVEAL;

        valid = map.build(layer, buildings(layer), px, py, RenderConfig.REVEAL);
        if (valid) {
            uploadTexture();
        }
    }

    /** The generator only registers buildings on the ground layer; a basement gets none. */
    private static List<Building> buildings(WorldLayer layer) {
        Object world = ClientGameEnvironment.getWorldModel();
        if (layer.get_zindex() == WorldLayer.GROUND_LAYER && world instanceof RLWorldModel) {
            return ((RLWorldModel) world).getBuildings();
        }
        return new ArrayList<Building>();
    }

    private static void uploadTexture() {
        int[] pixels = map.pixels();
        int bytes = pixels.length * 4;
        if (upload == null || upload.capacity() < bytes) {
            upload = BufferUtils.createByteBuffer(bytes);
        }
        upload.clear();
        for (int i = 0; i < pixels.length; i++) {
            int c = pixels[i];
            upload.put((byte) ((c >> 16) & 0xFF));
            upload.put((byte) ((c >> 8) & 0xFF));
            upload.put((byte) (c & 0xFF));
            upload.put((byte) ((c >>> 24) & 0xFF));
        }
        upload.flip();

        if (textureId == 0) {
            textureId = glGenTextures();
        }
        glBindTexture(GL_TEXTURE_2D, textureId);
        //NEAREST both ways: the map is drawn at whole pixels per tile, and a filtered
        //one-tile alley smears into the asphalt either side of it
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, map.width(), map.height(), 0, GL_RGBA,
                GL_UNSIGNED_BYTE, upload);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    // ----------------------------------------------------------------- corner plate

    private static void corner() {
        int zoom = Math.max(1, MINI_BOX / Math.max(map.width(), map.height()));
        float w = map.width() * zoom;
        float h = map.height() * zoom;
        //top right, under the clock and the build stamp: the bottom of the screen is the
        //console's, and a map dropped there sits on top of the last thing anybody said
        float x = WindowRender.get_window_w() - MARGIN - w - PAD;
        float y = TOP;

        Panel.backdrop(x - PAD, y - PAD, w + PAD * 2, h + PAD * 2);
        blit(x, y, w, h);

        //no markers here beyond the player: at one pixel per tile the building already IS
        //its own marker, and a pip on top of it only punches a hole in the shape
        player(sx(x, zoom, Player.get_ent().x()), sy(y, zoom, Player.get_ent().y()), false);

        TrueTypeFont font = Panel.tagFont();
        font.drawString(x - PAD + 2, y + h + PAD + 2, "M  map", Panel.DIM);
    }

    // ----------------------------------------------------------------- full sheet

    private static void sheet() {
        int winW = WindowRender.get_window_w();
        int winH = WindowRender.get_window_h();

        //curtain: the sheet is a stop-everything view, and a half-lit street behind it
        //competes with the very shapes it is trying to show
        Draw.beginFlat();
        Draw.quad(0, 0, winW, winH, 0.02f, 0.02f, 0.03f, 0.88f);
        Draw.endFlat();

        int roomW = winW - SHEET_MARGIN_X * 2 - LEGEND_W - LEGEND_GAP;
        int roomH = winH - SHEET_MARGIN_Y * 2;
        int zoom = Math.max(1, Math.min(roomW / map.width(), roomH / map.height()));
        float w = map.width() * zoom;
        float h = map.height() * zoom;
        float x = (winW - (w + LEGEND_GAP + LEGEND_W)) / 2;
        float y = (winH - h) / 2;

        Panel.backdrop(x - PAD, y - PAD, w + PAD * 2, h + PAD * 2);
        blit(x, y, w, h);

        for (TownMap.Landmark place : map.landmarks()) {
            label(place, sx(x, zoom, place.x), sy(y, zoom, place.y), zoom);
        }
        player(sx(x, zoom, Player.get_ent().x()), sy(y, zoom, Player.get_ent().y()), true);

        legend(x + w + LEGEND_GAP + PAD, y - PAD, LEGEND_W, h + PAD * 2);
    }

    /** A landmark's name, over a chip in its own colour, centred on the footprint. */
    private static void label(TownMap.Landmark place, float cx, float cy, int zoom) {
        int color = place.visited ? place.color() : MapPalette.hearsay(place.color());
        float r = Math.max(3f, zoom * 1.2f);
        pip(cx, cy, r, color);

        if (place.entrance != null && map.contains(place.entrance.getX(), place.entrance.getY())) {
            //the door, not the middle of the house: it is the tile you actually walk to
            pip(cx + (place.entrance.getX() - place.x) * zoom,
                cy + (place.entrance.getY() - place.y) * zoom, 2f, MapPalette.DOOR);
        }

        //the name sits on the building it names, which is already painted in this colour:
        //without a plate under it and a lift towards white it is the same grey on grey
        TrueTypeFont font = Panel.tagFont();
        float tw = font.getWidth(place.label);
        float th = font.getHeight();
        float tx = cx - tw / 2f;
        float ty = cy + r + 2;
        Draw.beginFlat();
        Draw.quad(tx - 3, ty, tw + 6, th - 2, 0.03f, 0.03f, 0.05f, 0.82f);
        Draw.endFlat();
        font.drawString(tx + 1, ty - 1, place.label, LABEL_SHADOW);
        font.drawString(tx, ty - 2, place.label, slick(MapPalette.lift(color, 0.55f)));
    }

    private static void legend(float x, float y, float w, float h) {
        Panel.backdrop(x, y, w, h);
        TrueTypeFont font = Panel.font();
        float row = font.getHeight() + 3;
        float ty = y + 8;
        float tx = x + 10;

        font.drawString(tx, ty, "TOWN MAP", Panel.HEADER);
        ty += row + 4;

        for (BuildingType type : BuildingType.values()) {
            swatch(tx, ty + 3, MapPalette.wall(type));
            font.drawString(tx + 16, ty, MapPalette.label(type).toLowerCase(),
                    slick(MapPalette.wall(type)));
            ty += row;
        }
        swatch(tx, ty + 3, MapPalette.HOME_WALL);
        font.drawString(tx + 16, ty, "your home", slick(MapPalette.HOME_WALL));
        ty += row + 6;

        swatch(tx, ty + 3, MapPalette.SIDEWALK);
        font.drawString(tx + 16, ty, "street", Panel.VALUE);
        ty += row;
        swatch(tx, ty + 3, MapPalette.GRASS);
        font.drawString(tx + 16, ty, "park", Panel.VALUE);
        ty += row;
        swatch(tx, ty + 3, MapPalette.DOOR);
        font.drawString(tx + 16, ty, "door", Panel.VALUE);
        ty += row + 8;

        font.drawString(tx, ty, "dim = heard of,", Panel.DIM);
        ty += row;
        font.drawString(tx, ty, "not yet walked", Panel.DIM);
        ty += row + 8;

        int px = Player.get_ent().x();
        int py = Player.get_ent().y();
        font.drawString(tx, ty, "you  " + px + "," + py, Panel.VALUE);
        ty += row + 8;

        //a directory, nearest first: the map answers "where", this answers "how far"
        font.drawString(tx, ty, "PLACES", Panel.SECTION);
        ty += row + 2;
        float lastRow = y + h - 10 - row;
        for (TownMap.Landmark place : nearest(px, py)) {
            if (ty > lastRow - row) {
                break;
            }
            Point door = place.entrance == null ? new Point(place.x, place.y) : place.entrance;
            font.drawString(tx, ty, Panel.pad(place.label.toLowerCase(), 9)
                            + step(px, py, door.getX(), door.getY()),
                    place.visited ? slick(place.color())
                                  : slick(MapPalette.hearsay(place.color())));
            ty += row;
        }

        font.drawString(tx, lastRow, "M / ESC  close", Panel.DIM);
    }

    /** Landmarks by how far they are, so the top of the list is the part you can act on. */
    private static List<TownMap.Landmark> nearest(final int px, final int py) {
        List<TownMap.Landmark> places = new ArrayList<TownMap.Landmark>(map.landmarks());
        places.sort((a, b) -> steps(px, py, a) - steps(px, py, b));
        return places;
    }

    private static int steps(int px, int py, TownMap.Landmark place) {
        Point door = place.entrance == null ? new Point(place.x, place.y) : place.entrance;
        return Math.max(Math.abs(door.getX() - px), Math.abs(door.getY() - py));
    }

    /** Chebyshev distance: the grid allows diagonals, so this is the walk in turns. */
    private static String step(int px, int py, int x, int y) {
        return Math.max(Math.abs(x - px), Math.abs(y - py)) + "m";
    }

    // ----------------------------------------------------------------- primitives

    private static void blit(float x, float y, float w, float h) {
        glBindTexture(GL_TEXTURE_2D, textureId);
        glEnable(GL_TEXTURE_2D);
        glColor4f(1, 1, 1, 1);
        glBegin(GL_QUADS);
        Draw.sprite(x, y, w, h, 0, 0, 1, 1);
        glEnd();
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    /** Screen centre of a world tile, given the map's top-left corner and its zoom. */
    private static float sx(float originScreenX, int zoom, int worldX) {
        return originScreenX + (worldX - map.originX()) * zoom + zoom / 2f;
    }

    private static float sy(float originScreenY, int zoom, int worldY) {
        return originScreenY + (worldY - map.originY()) * zoom + zoom / 2f;
    }

    /** A filled square with a dark keyline, so it reads over asphalt and over a lit floor. */
    private static void pip(float cx, float cy, float r, int argb) {
        Draw.beginFlat();
        Draw.quad(cx - r - 1, cy - r - 1, r * 2 + 2, r * 2 + 2, 0.02f, 0.02f, 0.03f, 0.85f);
        Draw.quad(cx - r, cy - r, r * 2, r * 2,
                MapPalette.red(argb), MapPalette.green(argb), MapPalette.blue(argb),
                MapPalette.alpha(argb));
        Draw.endFlat();
    }

    private static void swatch(float x, float y, int argb) {
        Draw.beginFlat();
        Draw.quad(x, y, 10, 8, MapPalette.red(argb), MapPalette.green(argb),
                MapPalette.blue(argb), 1f);
        Draw.endFlat();
    }

    /** Where you are: a white pip, and on the sheet a ring that breathes so the eye finds it. */
    private static void player(float cx, float cy, boolean ring) {
        if (ring) {
            float phase = (Timer.get_time() % 1400) / 1400f;
            circle(cx, cy, 6 + phase * 12, 1f - phase);
        }
        pip(cx, cy, 2.5f, MapPalette.PLAYER);
    }

    private static void circle(float cx, float cy, float r, float alpha) {
        glDisable(GL_TEXTURE_2D);
        glLineWidth(2);
        glBegin(GL_LINE_LOOP);
        glColor4f(MapPalette.red(MapPalette.PLAYER_RING), MapPalette.green(MapPalette.PLAYER_RING),
                MapPalette.blue(MapPalette.PLAYER_RING), alpha * 0.8f);
        for (int i = 0; i < 24; i++) {
            double a = Math.PI * 2 * i / 24;
            glVertex2f(cx + (float) Math.cos(a) * r, cy + (float) Math.sin(a) * r);
        }
        glEnd();
        glEnable(GL_TEXTURE_2D);
    }

    private static Color slick(int argb) {
        return new Color(MapPalette.red(argb), MapPalette.green(argb), MapPalette.blue(argb));
    }

    private static boolean flag(String key, boolean fallback) {
        String value = System.getProperty(key);
        return value == null ? fallback : Boolean.parseBoolean(value);
    }
}
