package com.nuclearunicorn.serialkiller.render;

import com.nuclearunicorn.libroguelike.core.Input;
import com.nuclearunicorn.libroguelike.game.ent.Entity;
import com.nuclearunicorn.libroguelike.game.ent.ItemEntity;
import com.nuclearunicorn.libroguelike.game.player.Player;
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
 *
 * People are drawn in three passes: the baked neutral figure, then a coat and a
 * hair mask tinted from the entity's own palette, so a street full of NPCs is
 * not a street full of identical grey dolls. An actor standing on a tile the
 * player remembers but cannot see becomes a rimmed silhouette instead.
 */
public class AsciiEntRenderer extends EntityRenderer {

    private static final int KIND_UNRESOLVED = -2;
    private static final int KIND_NONE = -1;

    /** Coats, in the mock's palette: muted, one saturated accent each. */
    private static final Color[] COATS = {
            new Color(180, 69, 60), new Color(62, 92, 140), new Color(78, 107, 74),
            new Color(122, 78, 140), new Color(140, 106, 58), new Color(56, 94, 99),
            new Color(160, 164, 174), new Color(107, 58, 68),
    };

    private static final Color[] HAIRS = {
            new Color(43, 32, 25), new Color(74, 53, 36), new Color(138, 106, 60),
            new Color(110, 74, 58), new Color(154, 154, 154),
    };

    /** The player reads as the one pale figure in the frame. */
    private static final Color PLAYER_COAT = new Color(228, 226, 214);

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
        boolean debugView = Input.key_state_alt || RenderConfig.REVEAL;
        if (!tile.isExplored() && !tile.isVisible() && !debugView) {
            return;
        }

        int i = ent.x();
        int j = ent.y();
        if (!Grid.onScreen(i, j)) {
            return;
        }

        float x = Grid.cellX(i);
        float y = Grid.cellY(j);

        // remembered, but out of the player's field of view
        boolean unseen = !tile.isVisible() && !debugView;

        int sprite = spriteKind();
        boolean drewSprite = RenderConfig.PIXEL_SPRITES && sprite != KIND_NONE;
        if (drewSprite) {
            SpriteAtlas atlas = SpriteAtlas.get();
            SpriteAtlas.beginSprites();
            if (sprite == SpriteAtlas.OBJ_PERSON) {
                person(atlas, x, y, unseen);
            } else {
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
            }
            SpriteAtlas.endSprites();
        }

        if (!RenderConfig.ASCII_LAYER) {
            return;
        }
        if (!drewSprite || RenderConfig.ASCII_OVER_SPRITES) {
            Glyphs.draw(x, y, symbol, color, 0);
        } else if (isStateGlyph(sprite)) {
            // small marker above the head, not on top of the sprite
            Glyphs.draw(x, y, symbol, color, RenderConfig.spriteH() - 2);
        }
    }

    /** Body, then the tinted coat and hair on top — or one flat silhouette. */
    private void person(SpriteAtlas atlas, float x, float y, boolean unseen) {
        if (unseen) {
            atlas.object(SpriteAtlas.OBJ_PERSON_DARK, x, y, 1, 1, 1, 1);
            return;
        }
        atlas.object(SpriteAtlas.OBJ_SHADOW, x, y, 1, 1, 1, 0.85f);
        atlas.object(SpriteAtlas.OBJ_PERSON, x, y, 1, 1, 1, 1);

        Color coat = coatColor();
        atlas.object(SpriteAtlas.OBJ_PERSON_COAT, x, y, coat.r, coat.g, coat.b, 1);
        Color hair = HAIRS[pick(HAIRS.length, 977)];
        atlas.object(SpriteAtlas.OBJ_PERSON_HAIR, x, y, hair.r, hair.g, hair.b, 1);
    }

    /**
     * An explicit colour on the renderer wins — that is how the generator marks
     * police out. Everyone else gets a stable colour from their uid.
     */
    private Color coatColor() {
        if (ent == Player.get_ent()) {
            return PLAYER_COAT;
        }
        if (color != null && !isGlyphOnly(color)) {
            return color;
        }
        return COATS[pick(COATS.length, 31)];
    }

    /**
     * Two colours mean "nobody chose one": white, and the mint green
     * {@code NPCGenerator} paints every generic NPC. Those are glyph colours for
     * the ASCII layer, and dressing 50 people in the same mint coat is not what
     * they were for.
     */
    private static boolean isGlyphOnly(Color c) {
        if (c.r > 0.92f && c.g > 0.92f && c.b > 0.92f) {
            return true;
        }
        return Math.abs(c.r - 150 / 255f) < 0.02f
                && Math.abs(c.g - 250 / 255f) < 0.02f
                && Math.abs(c.b - 150 / 255f) < 0.02f;
    }

    /** Stable per-entity choice from a palette — the same NPC never changes coat. */
    private int pick(int count, int salt) {
        String uid = ent.get_uid();
        int h = (uid == null ? 0 : uid.hashCode()) * 0x9E3779B1 + salt * 0x85EBCA77;
        h ^= h >>> 15;
        h *= 0x27D4EB2D;
        h ^= h >>> 13;
        return (h & 0x7FFFFFFF) % count;
    }

    /**
     * Only actors get a state glyph, and only when they are not just idling.
     *
     * <p>Under ALT this used to append the entity's uid to the glyph and draw the pair in
     * cell-sized type — a UUID over every head, which is neither readable at that size nor
     * an answer to anything. Who an NPC is and what is driving it now belong to {@link
     * com.nuclearunicorn.serialkiller.render.overlays.NpcDebugOverlay}, which has the room
     * to say it.
     */
    private boolean isStateGlyph(int sprite) {
        if (sprite != SpriteAtlas.OBJ_PERSON) {
            return false;
        }
        return !"@".equals(symbol);
    }

    private static boolean castsShadow(int sprite) {
        return sprite == SpriteAtlas.OBJ_TREE
                || sprite == SpriteAtlas.OBJ_LAMP
                || sprite == SpriteAtlas.OBJ_ITEM;
    }

    /**
     * Map the entity onto a baked sprite. Doors change appearance as they open
     * so they are re-resolved every frame; everything else resolves once.
     */
    private int spriteKind() {
        if (ent instanceof EntityDoor) {
            boolean horizontal = inHorizontalWall();
            if ("/".equals(symbol)) {
                return horizontal ? SpriteAtlas.OBJ_DOOR_EW_OPEN
                        : SpriteAtlas.OBJ_DOOR_NS_OPEN;
            }
            return horizontal ? SpriteAtlas.OBJ_DOOR_EW : SpriteAtlas.OBJ_DOOR_NS;
        }
        if (ent instanceof EntityRLHuman) {
            return "%".equals(symbol) ? SpriteAtlas.OBJ_CORPSE : SpriteAtlas.OBJ_PERSON;
        }
        if (kind == KIND_UNRESOLVED) {
            kind = resolve(ent);
        }
        if (kind == SpriteAtlas.OBJ_WINDOW_EW) {
            return inHorizontalWall() ? SpriteAtlas.OBJ_WINDOW_EW
                    : SpriteAtlas.OBJ_WINDOW_NS;
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
            return SpriteAtlas.OBJ_WINDOW_EW;
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
        if (name.startsWith("fridge")) {
            return SpriteAtlas.OBJ_FRIDGE;
        }
        if (name.startsWith("shelf") || name.startsWith("locker")
                || name.startsWith("bookcase")) {
            return SpriteAtlas.OBJ_SHELF;
        }
        if (name.startsWith("safe")) {
            return SpriteAtlas.OBJ_SAFE;
        }
        if (name.startsWith("crate") || name.startsWith("box")) {
            return SpriteAtlas.OBJ_CRATE;
        }
        if (name.startsWith("bed")) {
            return SpriteAtlas.OBJ_BED;
        }
        if (name.startsWith("sofa") || name.startsWith("couch")) {
            return SpriteAtlas.OBJ_SOFA;
        }
        if (name.startsWith("chair") || name.startsWith("stool")) {
            return SpriteAtlas.OBJ_CHAIR;
        }
        if (name.startsWith("counter")) {
            return SpriteAtlas.OBJ_COUNTER;
        }
        if (name.startsWith("bathtub")) {
            return SpriteAtlas.OBJ_BATHTUB;
        }
        // "reception desk" and friends: the qualifier comes first
        if (name.contains("desk") || name.contains("table")
                || name.contains("workbench")) {
            return SpriteAtlas.OBJ_DESK;
        }
        return KIND_NONE;
    }
}
