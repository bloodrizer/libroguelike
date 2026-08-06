package com.nuclearunicorn.serialkiller.render;

import com.nuclearunicorn.libroguelike.core.Input;
import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.game.ent.ItemEntity;
import com.nuclearunicorn.libroguelike.render.EntityRenderer;
import com.nuclearunicorn.serialkiller.game.world.RLTile;
import com.nuclearunicorn.serialkiller.game.world.entities.EntLadder;
import com.nuclearunicorn.serialkiller.game.world.entities.EntityDoor;
import com.nuclearunicorn.serialkiller.game.world.entities.EntityRLHuman;
import org.newdawn.slick.Color;

/**
 * Entity renderer for the hybrid look: a baked pixel sprite where we have one
 * for that kind of entity, the ASCII glyph where we don't. Both are bottom
 * aligned inside the cell's 2:3 box, so anything standing up overlaps the row
 * behind it.
 *
 * Actors keep their state glyph ('!' fleeing, 'Z' asleep, ...) floating over the
 * sprite — the pixel layer shows what a thing is, the ASCII layer what it does.
 */
public class AsciiEntRenderer extends EntityRenderer {

    private static final int KIND_UNRESOLVED = -2;
    private static final int KIND_NONE = -1;

    public String symbol = "?";
    Color color = Color.white;

    private int kind = KIND_UNRESOLVED;

    public AsciiEntRenderer(String s) {
        super();
        this.symbol = s;
    }

    public AsciiEntRenderer(String s, Color color) {
        super();
        this.symbol = s;
        this.color = color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public void render() {
        if (ent == null || ent.tile == null) {
            return;
        }
        if (!(ent.tile instanceof RLTile)) {
            return;
        }
        RLTile tile = (RLTile) ent.tile;
        if (!tile.isExplored() && !tile.isVisible()
                && !Input.key_state_alt && !RenderConfig.REVEAL) {
            return;
        }

        int i = ent.x();
        int j = ent.y();
        if (!Grid.onScreen(i, j)) {
            return;
        }

        float x = Grid.cellX(i);
        float y = Grid.cellY(j);

        int sprite = spriteKind();
        boolean drewSprite = RenderConfig.PIXEL_SPRITES && sprite != KIND_NONE;
        if (drewSprite) {
            SpriteAtlas atlas = SpriteAtlas.get();
            SpriteAtlas.beginSprites();
            if (castsShadow(sprite)) {
                atlas.object(SpriteAtlas.OBJ_SHADOW, x, y, 1, 1, 1, 0.85f);
            }
            if (SpriteAtlas.isTinted(sprite)) {
                // pull the entity colour towards neutral: the sprite's own
                // shading has to survive the tint
                atlas.object(sprite, x, y,
                        0.45f + 0.55f * color.r,
                        0.45f + 0.55f * color.g,
                        0.45f + 0.55f * color.b, 1.0f);
            } else {
                atlas.object(sprite, x, y, 1.0f, 1.0f, 1.0f, 1.0f);
            }
            SpriteAtlas.endSprites();
        }

        if (!RenderConfig.ASCII_LAYER) {
            return;
        }
        if (!drewSprite || RenderConfig.ASCII_OVER_SPRITES) {
            Glyphs.draw(x, y, debugSymbol(), color, 0);
        } else if (isStateGlyph(sprite)) {
            // small marker above the head, not on top of the sprite
            Glyphs.draw(x, y, debugSymbol(), color, RenderConfig.spriteH() - 2);
        }
    }

    private String debugSymbol() {
        if (ent instanceof EntityRLHuman && Input.key_state_alt) {
            return symbol + " [" + ent.get_uid() + "]";
        }
        return symbol;
    }

    /** Only actors get a state glyph, and only when they are not just idling. */
    private boolean isStateGlyph(int sprite) {
        if (sprite != SpriteAtlas.OBJ_HUMAN) {
            return false;
        }
        return !"@".equals(symbol) || Input.key_state_alt;
    }

    private static boolean castsShadow(int sprite) {
        return sprite == SpriteAtlas.OBJ_HUMAN
                || sprite == SpriteAtlas.OBJ_BOX_LOW
                || sprite == SpriteAtlas.OBJ_BOX_TALL
                || sprite == SpriteAtlas.OBJ_TREE
                || sprite == SpriteAtlas.OBJ_LAMP;
    }

    /**
     * Map the entity onto a baked sprite. Doors change appearance as they open
     * so they are re-resolved every frame; everything else resolves once.
     */
    private int spriteKind() {
        if (ent instanceof EntityDoor) {
            if ("/".equals(symbol)) {
                return SpriteAtlas.OBJ_DOOR_OPEN;
            }
            return inHorizontalWall() ? SpriteAtlas.OBJ_DOOR : SpriteAtlas.OBJ_DOOR_NS;
        }
        if (ent instanceof EntityRLHuman) {
            return "%".equals(symbol) ? SpriteAtlas.OBJ_CORPSE : SpriteAtlas.OBJ_HUMAN;
        }
        if (kind == KIND_UNRESOLVED) {
            kind = resolve(ent);
        }
        if (kind == SpriteAtlas.OBJ_WINDOW) {
            return inHorizontalWall() ? SpriteAtlas.OBJ_WINDOW : SpriteAtlas.OBJ_WINDOW_NS;
        }
        return kind;
    }

    /** Doors and windows have to follow the wall run they were punched into. */
    private boolean inHorizontalWall() {
        TileWindow view = SceneRenderer.activeView();
        return view == null || view.isWallRunHorizontal(ent.x(), ent.y());
    }

    private static int resolve(Entity ent) {
        if (ent instanceof EntLadder) {
            return SpriteAtlas.OBJ_LADDER;
        }
        if (ent instanceof ItemEntity) {
            return SpriteAtlas.OBJ_ITEM;
        }

        String name = ent.getName();
        if (name == null) {
            return KIND_NONE;
        }
        name = name.toLowerCase();

        if (name.startsWith("lamppost")) {
            return SpriteAtlas.OBJ_LAMP;
        }
        if (name.startsWith("window")) {
            return SpriteAtlas.OBJ_WINDOW;
        }
        if (name.startsWith("tree")) {
            return SpriteAtlas.OBJ_TREE;
        }
        if (name.startsWith("grass")) {
            return SpriteAtlas.OBJ_GRASS;
        }
        if (name.startsWith("ladder")) {
            return SpriteAtlas.OBJ_LADDER;
        }
        if (name.startsWith("fridge") || name.startsWith("shelf")
                || name.startsWith("safe") || name.startsWith("locker")) {
            return SpriteAtlas.OBJ_BOX_TALL;
        }
        if (name.startsWith("bed") || name.startsWith("sofa") || name.startsWith("desk")
                || name.startsWith("chair") || name.startsWith("crate")
                || name.startsWith("counter") || name.startsWith("bathtub")
                || name.startsWith("table")) {
            return SpriteAtlas.OBJ_BOX_LOW;
        }
        return KIND_NONE;
    }
}
