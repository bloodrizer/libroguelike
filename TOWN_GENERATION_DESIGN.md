# Town & Building Generation — Design Document

Target: replacement of the legacy BSP town generator in the `serialkiller` module.
Audience: implementing agent (Opus). All file paths are repo-relative. Read the
"Current state" section first — it tells you exactly what exists and what to reuse,
so you don't need to re-explore the codebase.

---

## 1. Current state (what exists today)

### 1.1 Files

| File | Role |
|---|---|
| `serialkiller/src/main/java/com/nuclearunicorn/serialkiller/generators/layerGenerators/TownChunkGenerator.java` | Orchestrator: tiles, districts, roads, safehouse, NPCs, police, furniture |
| `serialkiller/.../generators/MapGenerator.java` | BSP splitter (`process()` for districts, `roomProcess()` for rooms, `halfSplit`/`quadSplit`) |
| `serialkiller/.../generators/Block.java` | Axis-aligned rect: x/y/w/h, merge, intersection, `getOuterWall()`, `getFreeTile()` |
| `serialkiller/.../generators/Apartment.java` | `Block` subclass with `rooms` and `beds` lists |
| `serialkiller/.../game/world/RLTile.java` | Tile: `isWall`, `TileType {GROUND, ROAD, WALL, GRASS}`, `owners` list, blood, explored |
| `serialkiller/.../generators/layerGenerators/BasementGenerator.java` | Consumes ladder positions registered via `BasementGenerator.addLadder(zIndex, coord)` |
| `serialkiller/.../utils/pathfinder/adaptive/AdaptivePathfinder.java` | Waypoint graph built from "milestones" (district corners) |

### 1.2 Current pipeline (TownChunkGenerator.generate)

1. `CHUNK_SIZE` is set to **128** in `InGameMode.java:122`. Town area = chunk minus a
   5-tile margin → 118×118 tiles. Seed = `origin.x*10000 + origin.y`.
2. One root `Block` is recursively BSP-split by `MapGenerator.process()`:
   `quadSplit` (4-way, 40–60% split point) or `halfSplit` when aspect > 3:1,
   until area < `MIN_BLOCK_SIZE` (1800).
3. District corners are registered as pathfinder milestones. Roads (width `ROAD_SIZE=3`)
   are traced around each district perimeter, then districts `scale(-3,-3)` inward.
4. One random district becomes the **safehouse** (player apartment + family spawn,
   tiles pre-explored, `RLWorldModel.playerSafeHouseLocation` recorded).
5. Every other district: 80% → `generateHousing()` (one building filling the whole
   district), 20% → `generatePark()` (grass/trees/random NPCs).
6. `generateHousing()`: `traceBlock()` draws the rect perimeter as walls, then
   `MapGenerator.roomProcess()` BSP-splits the interior into ~4 rooms, doors are
   punched at room–room intersection walls, each room gets a window **or** an
   exterior door at the midpoint of any wall segment touching the building perimeter
   (80% window / 20% door).
7. `populateMap()`: pedestrians spawned on roads (35% per road segment), each NPC is
   assigned a random `Apartment` and stamped as `owner` on every tile of it; 4
   policemen with vest + stunstick.
8. `fillApartmentRooms()`: one KITCHEN (fridge + food), one STOREROOM (descending
   ladder → registered with `BasementGenerator`), N BEDROOM beds where N = owner
   count read back from tile owners.

### 1.3 Known problems (why we're replacing it)

- **Rooms too large.** `roomProcess` stops splitting as soon as a pass produces no
  new blocks; `MIN_BLOCK_SIZE` for rooms is `district.area/4`, so a 30×30 district
  yields ~200-tile rooms. There is no max-area or aspect-ratio constraint.
- **Buildings are featureless rectangles.** A building always fills its entire
  district; `traceBlock()` can only trace rects. No L/T/U shapes, no yards, no
  setbacks, no visual variety.
- **Single building type.** Everything is a residential apartment. No offices,
  banks, shops, brothels; furnishing logic is hardcoded in a `switch`.
- Known geometry bug (commented in `generateHousing`): corner rooms can produce
  phantom intersection walls; door punching relies on rect-intersection heuristics.
- `Block.getFreeTile()` **loops forever** if a room has no free tile — any new code
  must keep rooms ≥ 3×3 interior or replace that method (see §7.4).

---

## 2. Goals & non-goals

**Goals**

1. Rooms of controlled, human-scale size (typ. 4×4 … 8×6 interior).
2. Organic building footprints: L/T/U/rect-with-courtyard shapes, setbacks from the
   sidewalk, buildings that do NOT fill the whole lot.
3. Data-driven building types: APARTMENT, OFFICE, BANK, BROTHEL, SHOP, POLICE_STATION,
   PARK — extensible via templates, not code branches.
4. Interior layouts that read like the nanobanana mock: central corridor with rooms
   hanging off both sides, street-facing windows, marked entrances, desks/crates/
   furniture placed against walls.
5. Keep working: roads, milestones/pathfinding, safehouse+family flow, NPC ownership,
   basement ladders, park generation.

**Non-goals (do not build these now)**

- Multi-storey buildings above ground (z-layers stay as-is; basements unchanged).
- Road network beyond the existing perimeter-of-district grid.
- NPC daily schedules (ownership stamping is enough for current AI).

**Constraint: TeaVM.** This branch (`feature/teavm`) compiles Java → JS via TeaVM.
Stick to `java.util` collections, `Random`, plain arrays. No streams-heavy code, no
reflection, no `java.awt`. Match the existing Java-6-ish style of the module.

---

## 3. Architecture overview

Replace the "district == building" identity with a 5-stage pipeline. Each stage is a
pure function over a small data model; stages communicate through the classes in §4.

```
Stage 1  DISTRICTS   BSP split of chunk (KEEP existing MapGenerator.process)
Stage 2  LOTS        subdivide district edge-strips into lots facing streets
Stage 3  FOOTPRINT   per-lot boolean grid mask: union of 2-3 rects → L/T/U shape
Stage 4  INTERIOR    template-driven: corridor spine + room slicing on the mask
Stage 5  FURNISH     per-room-type furniture placement + doors/windows + NPCs
```

Rendering primitives stay the same: `placeWall/clearWall` on `RLTile`, entities via
`placeEntity`. All randomness must come from the seeded `chunk_random` (determinism:
same chunk seed → same town).

---

## 4. Data model (new classes, package `...serialkiller.generators.town`)

```java
/** Boolean occupancy grid local to one lot. (0,0) = lot origin in world coords. */
public class GridMask {
    public final int ox, oy, w, h;      // world-space origin + size
    private final boolean[] cells;      // row-major, w*h

    public GridMask(int ox, int oy, int w, int h) { ... }
    public boolean get(int lx, int ly) { /* bounds-checked, false outside */ }
    public void set(int lx, int ly, boolean v) { ... }
    public void fillRect(int lx, int ly, int rw, int rh, boolean v) { ... }
    public boolean isEdge(int lx, int ly) {
        // true if cell is set and any 4-neighbour is unset/out of bounds
    }
    public boolean isOuterCorner(int lx, int ly) { /* for wall art, optional */ }
}

public enum BuildingType { APARTMENT, OFFICE, BANK, BROTHEL, SHOP, POLICE_STATION }

public enum RoomType {
    // residential
    KITCHEN, BEDROOM, BATHROOM, STOREROOM, LIVING_ROOM,
    // commercial/other
    CORRIDOR, LOBBY, OFFICE_ROOM, VAULT, MANAGER_OFFICE,
    SHOP_FLOOR, BACKROOM, PRIVATE_ROOM, CELL, RECEPTION
}

public class Room extends Block {           // reuse Block: x/y/w/h in WORLD coords
    public RoomType type;
    public List<Room> connected = new ArrayList<Room>();  // door graph
    public Room(int x, int y, int w, int h) { super(x, y, w, h); }
}

/** Replaces/extends Apartment. Keep Apartment as a thin subclass or alias so the
    safehouse & ownership code keeps compiling (see §7.1). */
public class Building extends Apartment {
    public BuildingType type;
    public GridMask footprint;
    public List<Room> roomList = new ArrayList<Room>();
    public Point entrance;                  // world coords of main exterior door
    public Building(Block lot) { super(lot); }
}
```

**Template = data, not code.** One static table drives interior + furnishing:

```java
public class RoomSpec {
    public RoomType type;
    public int minCount, maxCount;      // per building
    public int minArea, maxArea;        // interior tiles
    public boolean wantsWindow;         // prefer exterior wall
    public boolean onCorridor;          // must open onto corridor/lobby
    public RoomSpec(RoomType t, int minC, int maxC, int minA, int maxA,
                    boolean win, boolean corr) { ... }
}

public class BuildingTemplate {
    public BuildingType type;
    public int minLotW, minLotH;        // reject lots too small for this type
    public float weight;                // spawn probability weight per district kind
    public boolean hasCorridor;         // corridor-spine vs. open-plan layout
    public List<RoomSpec> rooms;
}
```

Initial template table (tune later, see §8):

| Type | corridor | rooms (count, interior area, window, onCorridor) |
|---|---|---|
| APARTMENT | no (open plan) | LIVING_ROOM 1× 20–48 win; KITCHEN 1× 12–25 win; BEDROOM 1–3× 12–30 win; BATHROOM 1× 6–12; STOREROOM 0–1× 6–16 |
| OFFICE | yes | LOBBY 1× 16–36 win corr; OFFICE_ROOM 3–6× 12–30 win corr; STOREROOM 1× 6–16 corr; BATHROOM 1× 6–9 corr |
| BANK | yes | LOBBY 1× 25–48 win; OFFICE_ROOM 1–2× 12–20 corr; MANAGER_OFFICE 1× 12–20 corr; VAULT 1× 9–16 (no window, locked steel door) |
| BROTHEL | yes | RECEPTION 1× 16–30 win; PRIVATE_ROOM 3–6× 9–16 corr (bed each); BATHROOM 1× 6–9; STOREROOM 0–1× 6–12 |
| SHOP | no | SHOP_FLOOR 1× 25–60 win; BACKROOM 1× 9–20; BATHROOM 0–1× 6–9 |
| POLICE_STATION | yes | LOBBY 1× 16–30 win; OFFICE_ROOM 2–3× 12–25 corr; CELL 2–3× 6–9 (locked doors, no window) |

Safehouse is always `APARTMENT` (family/bed logic depends on it).

---

## 5. Algorithms per stage

### 5.1 Stage 1 — Districts (KEEP, small tweaks)

Keep `MapGenerator.process()` and the road tracing exactly as-is: it already produces
a believable street grid and feeds the pathfinder milestones. Two changes only:

- Lower `MIN_BLOCK_SIZE` for districts from 1800 → **~900–1200** so blocks are
  smaller and streets denser (closer to the mock's scale).
- After `district.scale(-ROAD_SIZE,-ROAD_SIZE)`, additionally shrink by 1 to leave a
  1-tile **sidewalk** ring inside each district; mark those tiles
  `TileType.ROAD` (or a new `SIDEWALK` enum value if you touch `RLTile` — a new enum
  constant is safe, renderer falls through to ground for unknown types; verify in
  `TilesetRenderer`). Place lampposts (`Entity`, non-blocking-sight, blocking) every
  ~8–10 sidewalk tiles on the street-facing side — this reproduces the mock's street
  furniture and feeds the lighting system if present.

### 5.2 Stage 2 — Lots

A district no longer hosts exactly one building. Split it into lots:

```
splitIntoLots(district):
    if district.w < 14 or district.h < 14:      // too small to split
        return [district]                        // single lot
    // Slice along the LONG axis into 2–3 strips of width 10–18,
    // then optionally halve strips longer than 24 across the other axis.
    // Every lot must touch at least one district edge (== street frontage).
    // Record for each lot which of its 4 sides face a street (the district
    // perimeter sides) — needed for entrance/window orientation in stage 4.
```

Reuse `MapGenerator.halfSplit` with a temporary `MIN_BLOCK_SIZE` if convenient, but
the strip slicing above is ~20 lines and gives better control; prefer writing it
directly. Keep 20% of *districts* (not lots) as parks — reuse `generatePark()`
unchanged.

### 5.3 Stage 3 — Footprint (the "organic" fix)

Per lot, build a `GridMask` instead of tracing the lot rect:

```
generateFootprint(lot, rng):
    mask = new GridMask(lot.x, lot.y, lot.w+1, lot.h+1)
    // 1. Base rect: inset from the lot by a setback:
    //    street-facing sides: setback 1–2 (small front yard / none)
    //    non-street sides:    setback 0–2
    //    base must be >= 8x8; if lot too small, use full lot minus 1.
    mask.fillRect(bx, by, bw, bh, true)
    // 2. Shape carving — pick one op with weights:
    //    40%  none                     -> rectangle
    //    30%  carve one corner rect    -> L-shape
    //    15%  carve two corners same side -> T/U-shape
    //    15%  add a wing: extra rect overlapping one edge -> L/T outward
    // Carved/added rects: between 1/4 and 1/2 of base in each dimension,
    // snapped so remaining limbs are >= 5 tiles thick (room + 2 walls).
    // 3. Validate: single connected region (flood fill), area >= sum of
    //    template minAreas + corridor estimate; else fall back to rectangle.
    return mask
```

**Wall tracing on a mask** (replaces `traceBlock` for buildings):

```java
void traceMask(GridMask m) {
    for (int i = 0; i < m.w; i++)
        for (int j = 0; j < m.h; j++)
            if (m.get(i, j) && m.isEdge(i, j))
                placeWall(m.ox + i, m.oy + j);
}
```

Interior floor = mask cells that are set and not edge. Yard = lot cells outside the
mask: sprinkle grass/crates/trash bins there at low probability (mock shows crates
against exterior walls — 5% crate on yard tiles adjacent to a wall works well).

### 5.4 Stage 4 — Interior layout

Two layout strategies, chosen by `template.hasCorridor`:

**A. Corridor-spine (offices, banks, brothels, police) — matches the mock.**

```
layoutCorridor(building, template, rng):
    // 1. Choose spine: the longest straight interior span along the footprint's
    //    long axis, 2 tiles wide (mock uses ~2), running wall-to-wall.
    //    For L/T shapes: one spine per limb, joined at the elbow (share a tile).
    // 2. Reserve spine tiles as CORRIDOR room(s).
    // 3. The strips on each side of the spine are room bands. Slice each band
    //    perpendicular to the spine using slot widths drawn from the template:
    //       target interior widths 3..6, never below 3.
    //    Greedy fill: while band remains, pick next RoomSpec that still needs
    //    instances and whose minArea fits; else emit filler OFFICE_ROOM/STOREROOM.
    // 4. Wall between band and spine: 1 tile; punch a door per room into the
    //    corridor (see 5.5).
    // 5. LOBBY placement: the room band cell nearest the street-facing side
    //    becomes the LOBBY; main entrance punches from street through lobby wall,
    //    lobby connects to corridor.
```

**B. Constrained BSP (apartments, shops) — fixes "rooms too large".**

Reuse the idea of `roomProcess` but with hard constraints; write it fresh against
`Room`, not `Block` (~40 lines):

```
splitRoom(room, template, rng, out):
    interiorArea = (room.w-1) * (room.h-1)
    if interiorArea <= MAX_ROOM_AREA (from template, e.g. 40) and
       aspect(room) <= 2.5:
        out.add(room); return
    // split the longer axis at 40–60%, but clamp so both halves keep
    // min interior dimension >= 3 (i.e. min span 5 incl shared wall).
    if no valid split point: out.add(room); return
    splitRoom(a, ...); splitRoom(b, ...)
```

Then assign `RoomType`s to the resulting rects: sort rooms by area desc, satisfy
template specs by best-fit (largest spec → largest room), leftover rooms get the
template's filler type (BEDROOM for apartments, SHOP_FLOOR annex for shops).
Rooms with `wantsWindow` should be preferentially assigned to rooms having at least
one exterior (mask-edge-adjacent) wall segment.

Interior walls for both strategies: for each `Room`, draw its rect perimeter with
`placeWall`, **but only on tiles inside the footprint mask** — never re-trace or
overwrite the mask edge, and skip tiles already carved as doors.

### 5.5 Doors, windows, connectivity

- **Room→corridor / room→room doors:** pick the middle third of the shared wall
  segment, choose one tile, `clearWall`, place `EntityDoor` (unlocked; VAULT and
  CELL doors locked with high hp — vault door hp ~200, normal 5 as today).
- **Main entrance:** exactly one per building, on a street-facing exterior wall of
  the LOBBY / LIVING_ROOM / SHOP_FLOOR. `clearWall` + `EntityDoor` (locked for
  BANK/OFFICE at "night", unlocked otherwise — if no clock exists, banks/offices
  locked, rest unlocked; apartments locked as today's 20% exterior doors were).
  Store in `building.entrance`; add the tile in front of it as a pathfinder
  milestone so NPCs path to doors (`chunk.addMilestone`).
- **Windows:** on exterior wall segments of rooms with `wantsWindow`, every 3–4
  tiles: `clearWall`, `EntityFurniture` window, hp 1, `setBlockSight(false)`
  (same as today). Street-facing walls get priority; never on VAULT/CELL/BATHROOM.
- **Connectivity guarantee (new, fixes the phantom-wall bug class):** build a graph
  of rooms with edges = placed doors; BFS from the entrance room. For every
  unreachable room, find a wall segment shared with any reachable room and punch a
  door there. Iterate until all rooms reachable. This is mandatory — the old
  code could generate sealed rooms.

### 5.6 Stage 5 — Furnishing

Replace the `fillRoom` switch with a per-RoomType placement table. Placement
helper (new, replaces blind `getFreeTile` for furniture):

```java
/** Free interior tiles adjacent to a wall, not blocking a door tile's
    4-neighbourhood. Furniture against walls == mock's look. */
List<Point> wallAdjacentFreeTiles(Room room, WorldLayer layer)
```

| RoomType | Furniture (entity, count, placement) |
|---|---|
| KITCHEN | fridge 1 (wall), table 0–1 (center), food in fridge as today |
| BEDROOM | bed 1 per owner (wall, non-blocking as today), wardrobe 0–1 (wall) |
| BATHROOM | bathtub 1 (wall) — new `EntityFurniture`, hp 30 |
| STOREROOM | crates 1–3 (wall), ladder if basement roll passes (keep `BasementGenerator.addLadder` + the 40%/100% safehouse rule) |
| LIVING_ROOM | sofa/table 1–2 (wall/center) |
| CORRIDOR | nothing (must stay walkable) |
| LOBBY / RECEPTION | desk 1 (facing entrance), chairs 1–2 |
| OFFICE_ROOM | desk 1–2 + chair each (mock shows desk+chair pairs), crate 0–1, papers item on desk |
| VAULT | 2–4 crates/safes containing money items (`ItemFactory` — add a `produceMoney` if absent; else reuse generic loot) |
| MANAGER_OFFICE | desk+chair, safe 0–1 |
| SHOP_FLOOR | shelves 2–4 (walls), counter 1 |
| BACKROOM | crates 1–3 |
| PRIVATE_ROOM | bed 1 (register in `building.beds` so NPC sleep AI works) |
| CELL | bed 1, locked door |

All furniture: `EntityFurniture` + `AsciiEntRenderer` symbol/color via the existing
`placeEntity` helpers. New symbols are fine (desk `d`, crate `x`, shelf `s`,
counter `c`, safe `$`, bathtub `b`); TUI/ascii renderer path already handles
arbitrary symbols.

### 5.7 Population & ownership

Keep `populateMap()` behaviour, with two changes:

1. **Ownership stamping** must iterate `building.footprint` interior cells, not the
   lot rect (current code stamps the whole district rect —
   `TownChunkGenerator.java:431-435`).
2. Only `APARTMENT` buildings receive resident owners/beds via
   `fillApartmentRooms`-equivalent logic. Commercial buildings get 0–2 "staff" NPCs
   spawned inside during generation (pedestrian AI is fine for now), and are valid
   `setApartment` targets only if nothing residential exists (avoid: filter the
   apartment list to residential before `chunk_random.nextInt(...)` — note the
   current code crashes with `nextInt(0)` if the list is empty, guard it).

Police count: keep `MAX_POLICEMAN_COUNT=4`, but if a POLICE_STATION was generated,
spawn them at its LOBBY instead of random roads.

---

## 6. Building type selection

Per lot, filter templates by `minLotW/minLotH` fit, then weighted pick:

```
APARTMENT 55, SHOP 15, OFFICE 15, BANK 5, BROTHEL 5, POLICE_STATION 5
```

Constraints enforced per **chunk**: ≥ 60% of buildings APARTMENT (NPCs need homes),
≤ 1 BANK, ≤ 1 POLICE_STATION, ≤ 1 BROTHEL. Safehouse lot is forced APARTMENT and
selected before the loop (keep the existing safehouse flow order: safehouse →
others → populate → furnish; family spawn code in `generateSafehouse` is untouched
except it should call the new interior generator).

---

## 7. Integration notes & pitfalls (read carefully)

1. **Keep `Apartment` API alive.** `RLWorldModel.getApartments()`, NPC
   `setApartment`, sleep AI and tile owners all reference `Apartment`. Making
   `Building extends Apartment` (as in §4) keeps every call site compiling.
   `Apartment.rooms` (`List<Block>`) can hold `Room` instances since
   `Room extends Block`.
2. **Generation order matters.** Ownership stamping (`populateMap`) currently runs
   BEFORE `fillApartmentRooms` because bedroom count = tile owner count. Preserve:
   footprints+walls → populate/owners → furnish.
3. **Safehouse explored-tiles loop** (`TownChunkGenerator.java:181-185`) iterates
   the rect — switch it to footprint mask cells or it will reveal yard tiles
   (harmless) / crash if you shrink the block (it won't, rect is fine as a superset;
   leave it if lazy, but mask iteration is 3 lines).
4. **`Block.getFreeTile` infinite loop** (`Block.java:234-243`): with small rooms
   (3×3 interior) and furniture, a room can fill up. Add a bounded variant:
   try N=50 random samples, then linear-scan the interior, then return `null`; all
   new callers must handle `null` by skipping placement. Do not change the old
   method's behaviour for legacy callers — add `getFreeTileSafe`.
5. **Roads/milestones untouched.** Do not modify `generateRoads`, milestone
   registration, or `AdaptivePathfinder` calls — only ADD entrance milestones
   (§5.5). Pathfinder cost is already flagged "sub-optimal"; more milestones is
   fine, ~1 per building.
6. **BasementGenerator contract:** ladders registered per z-index during town gen
   are consumed when the basement layer generates. Keep `addLadder` calls exactly
   where storerooms with ladders are furnished.
7. **Determinism:** everything from `chunk_random`. Do not `new Random()` anywhere
   (note: `MapGenerator` has an unseeded-by-default `Random` — it's fine because
   `setSeed` is always called; new code should take `Random` in constructors).
8. **TeaVM:** no streams/lambdas beyond what already compiles in this branch, no
   `String.format` in hot loops, plain collections only.
9. **Coordinate convention gotcha:** `Block` w/h are used inclusively in wall code
   (`traceBlock` draws at `x+w`, `y+h`) but exclusively in `getTiles()`/`getArea()`.
   `GridMask` sidesteps this — define mask size as `lot.w+1 × lot.h+1` and treat a
   set cell as "this tile belongs to the building", edges become walls. Interior
   area of a room rect = `(w-1)*(h-1)`.
10. **Do not delete `MapGenerator`** — district stage still uses `process()`. The
    room-related methods (`roomProcess`) become dead once stage 4 lands; delete them
    in a final cleanup commit only after visual verification.

---

## 8. Tuning constants (single place, e.g. `TownGenConfig`)

```java
public class TownGenConfig {
    public static int DISTRICT_MIN_AREA   = 1000;  // was 1800
    public static int ROAD_SIZE           = 3;     // unchanged
    public static int LOT_MIN_STRIP       = 10;
    public static int LOT_MAX_STRIP       = 18;
    public static int MIN_ROOM_DIM        = 3;     // interior tiles
    public static int MAX_ROOM_AREA       = 40;    // interior tiles (BSP clamp)
    public static float MAX_ROOM_ASPECT   = 2.5f;
    public static int CORRIDOR_WIDTH      = 2;
    public static int WINDOW_SPACING      = 3;
    public static int LAMPPOST_SPACING    = 9;
    public static int PARK_DISTRICT_PCT   = 20;
    public static int NPC_PER_ROAD_RATE   = 35;    // unchanged
    public static int MAX_POLICEMAN_COUNT = 4;     // unchanged
}
```

---

## 9. Implementation phases (each independently shippable & testable)

**Phase 1 — Room size fix (small, immediate visual win).**
Constrained BSP (§5.4B) replacing `roomProcess` for apartments, connectivity pass
(§5.5), `getFreeTileSafe`. No footprint/type changes yet.
*Accept:* no room's interior exceeds `MAX_ROOM_AREA`; every room reachable from an
exterior door (assert via BFS in a debug check); game boots, safehouse works.

**Phase 2 — Lots + footprints.**
Stage 2 lots, `GridMask`, stage 3 footprints, `traceMask`, yards, sidewalks +
lampposts. All buildings still APARTMENT.
*Accept:* visibly non-rectangular buildings (~60% non-rect), multiple buildings per
large district, no sealed rooms, ownership stamping uses masks, no
`getFreeTile` hangs across ≥ 20 different seeds (loop seeds in a test/main harness).

**Phase 3 — Building types + furnishing.**
Templates (§4), corridor-spine layout (§5.4A), furnishing table (§5.6), type
selection (§6), population changes (§5.7).
*Accept:* a chunk contains ≥ 3 distinct building types; office layout shows
corridor + side rooms with desks (compare against the mock); bank has a locked
vault; safehouse flow and basement ladders still work.

**Phase 4 — Cleanup.**
Delete dead `roomProcess`/merge code, remove commented legacy blocks in
`TownChunkGenerator`, extract remaining magic numbers into `TownGenConfig`.

Verification tooling suggestion for all phases: a `main()` harness that generates a
chunk for seeds 1..20 and prints an ASCII dump of `RLTile.isWall`/type per tile —
cheap regression check without booting the full game (LWJGL not needed if you only
touch tiles; entity spawn requires environment, so gate NPC/furniture spawning
behind a `dryRun` flag if the harness fights the engine).

---

## 10. Reference: the target look (nanobanana mock)

Key features of the mock to reproduce, in priority order:
1. Central 2-wide corridor with office rooms on both sides, one door per room.
2. Street-facing windows at regular intervals; windowless service rooms.
3. Building is a large L/rect occupying most (not all) of its lot; sidewalk +
   lampposts + dotted parking strip outside.
4. Furniture: desk+chair pairs in offices, crate stacks in storerooms/along walls.
5. Marked entrances (colored door tiles) — use door entities with distinct
   renderer colors (cyan/red as in mock) for main vs. service entrances.
