# Rendering

The world renderer draws a square tile grid in a pseudo-isometric projection,
with pixel sprites baked at run time and a smooth light field applied over the
finished frame. Everything lives in
[serialkiller/src/main/java/.../render/](serialkiller/src/main/java/com/nuclearunicorn/serialkiller/render/).

## 1. Projection: square grid, 2:3 boxes, bottom aligned

The world model is unchanged — still a plain square grid, still one entity per
tile. Only the projection changed ([Grid.java](serialkiller/src/main/java/com/nuclearunicorn/serialkiller/render/Grid.java)):

| | |
|---|---|
| cell footprint | `CELL x CELL` at `(i*CELL, j*CELL)` |
| object box | `CELL x CELL*1.5` (2:3), **bottom aligned** to the cell |
| overhang | `riseH() = CELL/2`, upwards, into the row above |

Anything that stands up — walls, doors, furniture, actors — is drawn into that
2:3 box, so it overlaps the row behind it. That is the whole 2.5D effect: no
rotation, no isometric axes, no change to pathfinding or picking.

It only works if rows are painted north to south, so
[SceneRenderer](serialkiller/src/main/java/com/nuclearunicorn/serialkiller/render/SceneRenderer.java)
drives the frame instead of the engine's chunk walk:

1. floor quads for every visible cell,
2. **for each row**: wall slabs, then the entities standing in that row,
3. the light field, multiplied over the result.

Entities are bucketed per row each frame; `EntityRenderer.render()` is still the
extension point, it is just called in depth order now. Only the on-screen
window is visited (the old path walked a 128x128 chunk and culled per tile).
Inside a row, props are drawn before actors — an NPC asleep in a bed shares the
bed's tile, and spawn order used to decide which of the two you saw.

`CELL` is the zoom, and the only thing that sets the apparent sprite size: art
is baked at that resolution, so `-Dlrl.cell=40` gives bigger art, not a blurry
upscale of small art. It defaults to 32.

## 2. Smooth lighting

[LightMap.java](serialkiller/src/main/java/com/nuclearunicorn/serialkiller/render/LightMap.java)
replaces the per-tile colour maths that used to live in the tile renderer.

- **Emitters** — the player's torch, street lamps and lit windows (picked up
  from the entity list by name). Each gets a shadow-cast visibility mask from
  rlforj, cached until a door opens (`LightMap.invalidate()`), so light spills
  through doorways and windows instead of through walls.
- **Per cell** — ambient (time of day) plus every emitter in range with a
  quadratic falloff; out-of-FOV but explored cells get a flat "memory" tint.
  Wall cells take the brightest of their open neighbours, so the face pointing
  at a lamp is lit and the far side stays dark.
- **Per corner** — each cell corner averages the four cells touching it. This
  is what smooths the field: one Gouraud quad per cell makes the GL interpolate
  it per pixel, and FOV/shadow edges soften on their own.
- **Two passes** — a multiply pass (`dst *= light`) and a small additive pass.
  Multiplying alone can only darken, which turns a remembered room muddy; the
  additive term puts a little of the light's own colour back and gives the
  moonlit blue of unlit rooms.

## 3. ASCII + pixel sprites

There are no art assets, so
[SpriteAtlas](serialkiller/src/main/java/com/nuclearunicorn/serialkiller/render/SpriteAtlas.java)
rasterises one at startup with
[PixelCanvas](serialkiller/src/main/java/com/nuclearunicorn/serialkiller/render/PixelCanvas.java):
floor materials (asphalt, sidewalk, boards, carpet, grass, dirt — four variants
each), 16 wall auto-tiles, and the object sprites. Art resolution equals
`RenderConfig.CELL`, so pixels land 1:1; changing the cell size re-bakes.

Entities draw their sprite when they have one and their glyph when they don't
([AsciiEntRenderer](serialkiller/src/main/java/com/nuclearunicorn/serialkiller/render/AsciiEntRenderer.java)).
Actors keep their state glyph (`!` fleeing, `Z` asleep, `%` dead) floating above
the sprite: the pixel layer says what a thing is, the ASCII layer what it does.
Furniture resolves by entity name, so a bed, a shelf, a crate and a bathtub are
four different sprites rather than four tinted boxes.

Every object sprite is traced with a 1px dark rim after it is drawn. That rim is
what keeps a shape readable once the light pass has multiplied a dim room over
it — without it the pixel layer turns to smudges at low light.

**Wall connections.** A wall tile is a 3x3 stencil — centre always solid, an arm
towards each neighbouring wall — which is exactly how box-drawing characters
join, and gives the `|`, `L`, `T` and `+` joints from a 4-bit N/E/S/W mask. A
side arm also claims its corner square when both its neighbours are walls, so
solid blocks have no holes. Each stencil column is then extruded upwards by the
box rise: the raised copy is the slab's top face, the strip left behind is its
front face.

The contrast is in three hard lines, not in the fill: a bright lip on the north
cap, a near-black crease where the top face folds into the front, and a black
contact line where the front meets the floor. The crease is drawn **only where
the front face is exposed** — on a run continuing south it lands one pixel above
the next tile's top face and shows through as a seam every cell. For the same
reason the arms reaching into a neighbouring tile must not be capped, or every
run breaks into separate blocks.

**Doors and windows** are punched *out* of walls by the generator, so their
sprites rebuild the missing wall segment (`isWallGap` on the tile keeps the run
connected) and inset wood or glass into it. Each has an east-west and a
north-south variant (open doors too), picked per frame from
`TileWindow.isWallRunHorizontal()`: in an east-west run the leaf faces the
player and is drawn across the slab's front face; in a north-south run it is
seen from above and lies along the run instead.

**People** are four sprites over one geometry
(`SpriteAtlas.person`): a complete figure in a neutral palette, a coat-only and
a hair-only mask that the draw call tints per NPC, and the same figure again as
a flat silhouette with a bright rim. Coat and hair come from a stable hash of
the entity uid, so a crowd is not fifty identical grey dolls and nobody changes
clothes between frames; an explicit colour on the renderer overrides it, which
is how police stay blue. Two colours count as "nobody chose one" — white, and
the mint green `NPCGenerator` paints on every generic NPC, which is a glyph
colour and not a coat.

An actor standing on a tile the player *remembers but cannot currently see* is
drawn as the silhouette. That is the whole reason the rim is baked: at memory
light levels a black figure on a dark floor is invisible without it.

## Toggles

Each layer can be switched at run time or from the command line:

| key | property | effect |
|---|---|---|
| F1 | `-Dlrl.sprites=false` | pixel sprites off — pure ASCII |
| F2 | `-Dlrl.light=false` | light pass off — flat albedo |
| F3 | `-Dlrl.asciiOver=true` | draw glyphs on top of sprites too |
| F4 | `-Dlrl.reveal=true` | draw the map as if fully explored |
| | `-Dlrl.walls=false` | `#` glyphs instead of connected slabs |
| | `-Dlrl.cell=32` | zoom: screen pixels per tile, and the art resolution |
| | `-Dlrl.fov=6` | pin the player's sight radius (what memory looks like) |
| | `-Dlrl.seed=7` | fix the town, so two builds can be compared |

## Working on the art

**`scripts/atlas.sh out.png [cell] [zoom]`** bakes the whole atlas *without a GL
context* and writes two PNGs: the raw atlas, and a contact sheet with every
object sprite over a half-light/half-dark checker, all 16 wall masks and every
floor material, magnified. It runs in about a second, which is the difference
between iterating on a crate sprite and hunting for one in a dark room.

**`scripts/shot.sh out.png [frame]`** renders the game into a hidden window and
dumps one frame to a PNG (`render/ScreenCapture.java`, driven by
`-Dlrl.capture.*`). Useful on Wayland, where the GLFW window is invisible to
X11 screenshot tools. `LRL_SEED=7` fixes the town and `LRL_OPTS="..."` passes
anything else through:

```sh
LRL_SEED=7 LRL_OPTS="-Dlrl.reveal=true" ./scripts/shot.sh /tmp/a.png 300
```

For anything that needs the player somewhere specific — memory, silhouettes, a
lit room two doors away — drive it from a replay scenario rather than by hand;
see [REPLAY.md](REPLAY.md). `tp`, `spawn` and `tick` set up a frame in three
lines, and `-Dlrl.capture.*` still applies:

```sh
./scripts/mkreplay.py /tmp/s.jsonl "tick 2; tp 66 12; tick 3; tp 68 18; tick 2"
java -Dreplay.play=/tmp/s.jsonl -Dlrl.fov=6 -Dlrl.capture.file=/tmp/b.png ... -jar ...
```
