package com.nuclearunicorn.serialkiller.render;

import com.nuclearunicorn.libroguelike.core.Input;
import com.nuclearunicorn.libroguelike.core.client.ClientGameEnvironment;
import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.game.world.layers.WorldLayer;
import com.nuclearunicorn.libroguelike.render.EntityRenderer;
import com.nuclearunicorn.libroguelike.render.overlay.OverlaySystem;
import com.nuclearunicorn.serialkiller.game.social.SocialController;
import com.nuclearunicorn.serialkiller.game.world.RLTile;
import org.newdawn.slick.Color;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws the world in the pseudo-isometric grid projection.
 *
 * Order matters and is the whole trick:
 *   1. flat floor quads for every visible cell,
 *   2. rows north to south — walls first, then whatever stands in that row, so
 *      a tall object always overlaps the row behind it,
 *   3. the light field, multiplied over the finished frame.
 */
public class SceneRenderer {

    /** The window being drawn this frame; entity renderers query it for context. */
    private static TileWindow active;

    public static TileWindow activeView() {
        return active;
    }

    private final TileWindow view = new TileWindow();
    private final LightMap light = new LightMap();

    /** Entities bucketed per screen row for this frame. */
    private final List<List<Entity>> rows = new ArrayList<List<Entity>>();

    public TileWindow getView() {
        return view;
    }

    public LightMap getLight() {
        return light;
    }

    public void render(WorldLayer layer) {
        int x0 = Grid.firstVisibleX();
        int x1 = Grid.lastVisibleX();
        int y0 = Grid.firstVisibleY();
        int y1 = Grid.lastVisibleY();

        view.refresh(layer, x0, y0, x1, y1);
        active = view;
        light.update(view, x0, y0, x1, y1);

        renderFloors(x0, y0, x1, y1);
        bucketEntities(y0, y1);
        renderRows(x0, y0, x1, y1);
        renderDecals(x0, y0, x1, y1);

        if (RenderConfig.SMOOTH_LIGHT) {
            light.render(x0, y0, x1, y1);
        }
    }

    // ------------------------------------------------------------------ floor

    private void renderFloors(int x0, int y0, int x1, int y1) {
        int cell = RenderConfig.CELL;

        if (RenderConfig.PIXEL_SPRITES) {
            SpriteAtlas.get().bind();
            SpriteAtlas.beginSprites();
            for (int j = y0; j <= y1; j++) {
                for (int i = x0; i <= x1; i++) {
                    RLTile tile = view.at(i, j);
                    if (!isKnown(tile)) {
                        continue;
                    }
                    SpriteAtlas.get().floor(tile, i, j, Grid.cellX(i), Grid.cellY(j), cell);
                }
            }
            SpriteAtlas.endSprites();
            return;
        }

        Draw.beginFlat();
        for (int j = y0; j <= y1; j++) {
            for (int i = x0; i <= x1; i++) {
                RLTile tile = view.at(i, j);
                if (!isKnown(tile)) {
                    continue;
                }
                float[] c = Palette.floor(tile, i, j);
                Draw.quad(Grid.cellX(i), Grid.cellY(j), cell, cell, c[0], c[1], c[2]);
            }
        }
        Draw.endFlat();
    }

    // ------------------------------------------------------------- row passes

    private void bucketEntities(int y0, int y1) {
        int count = y1 - y0 + 1;
        while (rows.size() < count) {
            rows.add(new ArrayList<Entity>(4));
        }
        for (int i = 0; i < count; i++) {
            rows.get(i).clear();
        }

        List<Entity> all = ClientGameEnvironment.getEntityManager()
                .getList(com.nuclearunicorn.libroguelike.game.world.WorldView.get_zindex());

        // defensive copy: the entity list is mutated by the simulation thread
        Entity[] snapshot = all.toArray(new Entity[0]);
        for (Entity ent : snapshot) {
            if (ent == null || ent.origin == null) {
                continue;
            }
            int j = ent.y();
            if (j < y0 || j > y1 || !Grid.onScreen(ent.x(), j)) {
                continue;
            }
            rows.get(j - y0).add(ent);
        }
    }

    private void renderRows(int x0, int y0, int x1, int y1) {
        for (int j = y0; j <= y1; j++) {
            renderWallRow(x0, x1, j);
            for (Entity ent : rows.get(j - y0)) {
                EntityRenderer renderer = ent.get_render();
                if (renderer != null) {
                    renderer.render();
                }
            }
        }
    }

    private void renderWallRow(int x0, int x1, int j) {
        if (RenderConfig.PIXEL_SPRITES) {
            SpriteAtlas.get().bind();
            SpriteAtlas.beginSprites();
            for (int i = x0; i <= x1; i++) {
                RLTile tile = view.at(i, j);
                if (tile == null || !tile.isWall() || !isKnown(tile)) {
                    continue;
                }
                SpriteAtlas.get().wall(view, i, j);
            }
            SpriteAtlas.endSprites();
            return;
        }

        Draw.beginFlat();
        for (int i = x0; i <= x1; i++) {
            RLTile tile = view.at(i, j);
            if (tile == null || !tile.isWall() || !isKnown(tile)) {
                continue;
            }
            float[] c = Palette.wallTop(tile, i, j);
            if (RenderConfig.WALL_PSEUDOGRAPHICS) {
                WallPainter.paint(view, i, j, c[0], c[1], c[2]);
            } else {
                Draw.quad(Grid.cellX(i), Grid.cellY(j), RenderConfig.CELL, RenderConfig.CELL,
                        c[0] * 0.5f, c[1] * 0.5f, c[2] * 0.5f);
            }
        }
        Draw.endFlat();

        // classic mode: the wall is a '#' on a dark tile
        if (!RenderConfig.WALL_PSEUDOGRAPHICS && RenderConfig.ASCII_LAYER) {
            for (int i = x0; i <= x1; i++) {
                RLTile tile = view.at(i, j);
                if (tile != null && tile.isWall() && isKnown(tile)) {
                    Glyphs.draw(i, j, "#", Color.white);
                }
            }
        }
    }

    // ----------------------------------------------------------- glyph decals

    private void renderDecals(int x0, int y0, int x1, int y1) {
        if (!RenderConfig.ASCII_LAYER) {
            return;
        }
        for (int j = y0; j <= y1; j++) {
            for (int i = x0; i <= x1; i++) {
                RLTile tile = view.at(i, j);
                if (!isKnown(tile) || tile.isWall()) {
                    continue;
                }
                if (SocialController.hasCrimeplace(tile.origin)) {
                    Glyphs.draw(i, j, "X", Color.red);
                } else if (!tile.getTileModel().isEmpty() && tile.getTileModelColor() != null) {
                    Glyphs.draw(i, j, tile.getTileModel(), tile.getTileModelColor());
                }
            }
        }
    }

    static boolean isKnown(RLTile tile) {
        if (tile == null) {
            return false;
        }
        return tile.isExplored() || tile.isVisible()
                || Input.key_state_alt || RenderConfig.REVEAL;
    }
}
