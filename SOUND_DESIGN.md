# Sound Propagation — Design Document

Target: replacement of the radius-based hearing model in the `serialkiller` module.
Audience: implementing agent (Opus). All file paths are repo-relative. Read the
"Current state" section first — it tells you exactly what exists and what to reuse,
so you don't need to re-explore the codebase.

---

## 1. Current state (what exists today)

### 1.1 Files

| File | Role |
|---|---|
| `serialkiller/.../game/events/SuspiciousSoundEvent.java` | The entire sound model: `(origin, radius)` |
| `serialkiller/.../game/combat/RLCombat.java:159-171` | Only emitter. Hardcodes `radius 10`, **only when the attacker is the player** |
| `serialkiller/.../game/social/SocialController.java:116-123` | Dispatch: `RLMath.getEntitiesInRadius`, pure Euclidean |
| `serialkiller/.../game/ai/llm/sense/HearingSensor.java` | Speech path. `Fov.get_entity_in_radius`, also pure Euclidean |
| `libroguelike/.../utils/Fov.java:19-30` | `in_range` — three lines of Pythagoras, no occlusion anywhere |
| `serialkiller/.../game/ai/PedestrianAI.java:70-75` | Consumer: a heard sound becomes an anonymous crime report |
| `serialkiller/.../render/LightMap.java` | **Precedent to mirror structurally, not algorithmically** (see §2.3) |
| `serialkiller/.../game/world/RLTile.java` | `isWall`, `isWallGap`, `isIndoor`, `isPathBlocked` |
| `serialkiller/.../game/world/entities/EntityDoor.java` | `lock()`/`unlock()`; calls `LightMap.invalidate()` on change |

### 1.2 Current pipeline

1. `RLCombat.attack()` posts a `SuspiciousSoundEvent(target.origin, 10)`.
2. `SocialController.e_on_event` collects every entity within Euclidean radius 10
   and forwards the event to each `EntityRLActor`.
3. `PedestrianAI.e_on_event` turns that into `SocialController.reportCrime(origin,
   null, ...)` — a scene to investigate with no suspect named.
4. Speech is an entirely **separate** path: `HearingSensor` listens for
   `EChatMessage` and bands listeners by radius — `directedRadius` (4) →
   `Salience.DIRECTED`, `earshotRadius` (10) → `Salience.NOTABLE`.

### 1.3 Known problems (why we're replacing it)

- **Walls do not exist.** An NPC hears a beating through a brick exterior wall at
  exactly the same strength as across an open street.
- **Loudness does not exist.** A whisper, a knife, and a gunshot are all radius 10.
- **NPC-on-NPC violence is silent.** `RLCombat.java:167` gates emission on
  `owner.isPlayerEnt()`, so a mugging two streets over makes no sound at all.
- **Two independent hearing paths** (combat noise, speech) with no shared model.
- **The `HearingSensor` dead band.** Its own comment (lines 27-31) documents the
  symptom: "standing near someone" in a room is 5-10 tiles, but only radius ≤ 4
  counted as addressed, so a deliberate hello landed as NOTABLE and got no reply.
  The fix in place is a nearest-listener hack. Radius is the wrong signal; the real
  distinction is *how loud it arrived*, which nothing currently computes.
- **No arrival direction.** An NPC that hears something knows only a point. It will
  walk at the wall between it and the noise rather than round to the door.

---

## 2. Goals & non-goals

**Goals**

1. Sound attenuates through **connected space**, so it diffracts round corners and
   pours through open doorways — and is stopped by walls, *unless* it is loud enough
   to punch through.
2. One model for every noise in the game: speech, footsteps, combat, breaking glass.
3. Every sound carries an **arrival direction** at each listener, so investigation
   heads for the doorway the sound came through.
4. **Ambient masking**: a noise floor per tile, from room type and time of day, so
   *when* and *where* you kill someone is an acoustic decision.
5. The player hears the town too — directional, attenuated messages.
6. Preserve current outdoor feel: existing radius-4/radius-10 speech behaviour should
   fall out of the new numbers, not be re-tuned by hand (§4.5 shows it does).

**Non-goals (do not build these now)**

- Reverb, echo, decay time. We compute arrival loudness and direction only.
- Reflection/specular paths. Diffraction is 90% of what matters here; a flood gives
  it for free and ray tracing does not.
- Sound occlusion across z-layers (basements stay acoustically isolated).
- Actual audio output. This is a simulation system, not an audio engine.

**Constraint: TeaVM.** This branch compiles Java → JS. Plain arrays, `java.util`
collections, no streams, no reflection, no floats in the hot loop (§5.2 is
integer-only by design). Match the Java-6-ish style of the module.

### 2.3 Do not reuse `LightMap`'s algorithm

`LightMap` is the right *shape* — a field computed over a tile window, invalidated
by an epoch that `EntityDoor` already bumps — and the wrong *algorithm*. It uses
`rlforj` shadowcasting (`PrecisePermissive`), which is line-of-sight. Light does not
bend round corners; sound does. Shadowcast sound would ship NPCs who cannot hear you
around a bend in an open corridor, which is a worse bug than the one we are fixing.

Copy the class layout. Do not copy the LOS call.

---

## 3. The model

Sound is a scalar field over tiles, in integer "dB-ish" units on a 0-120 scale:

```
received(tile) = loudness − accumulated_loss(tile)
heard(listener) = received(listener.tile) > max(listener.threshold, ambient(listener.tile))
```

`accumulated_loss` is the cost of the cheapest acoustic path from source to tile,
where each step costs **air** (spreading) plus the **transmission loss** of the tile
being entered.

### 3.1 Why linear falloff, not `20·log₁₀(d)`

Physically correct free-field spreading is logarithmic in distance. We use a
constant dB-per-tile instead, deliberately:

1. **Dijkstra stays exactly correct.** With log falloff, the search state is
   two-dimensional — `(path length, accumulated transmission loss)` — because a
   longer path with less wall may beat a shorter path through more. A scalar
   priority over a 2-D state is a *resource-constrained* shortest path, and plain
   Dijkstra is no longer optimal for it. With linear air cost the key **is** the
   state, and the algorithm is textbook-correct.
2. **Every edge weight becomes a small non-negative integer**, which buys a bucket
   queue (§5.2): O(cells), no heap, no floats, no `Math.log`. TeaVM-friendly.
3. **It is legible.** "Gunshot 92, air 2/tile, floor 12 → 40 tiles in the open" is
   something you can hold in your head while tuning. Log falloff is not.

Linear dB/distance is not even that unphysical indoors or down a street canyon,
where spreading is closer to cylindrical than spherical, and atmospheric absorption
is genuinely linear in dB/m.

### 3.2 Diagonal cost

Air costs `2` cardinal, `3` diagonal — the classic 2/3 chamfer metric, which
approximates Euclidean distance to within ~4% and keeps everything integral.

---

## 4. Data

### 4.1 Tile transmission loss

Added when the flood **enters** a tile. Stored as `byte RLTile.soundLoss`.

| Tile / occupant | Loss | Note |
|---|---|---|
| Floor, road, grass, doorway gap | 0 | |
| Prop / crate / furniture | 4 | anything blocking that isn't a person |
| Tree | 3 | |
| Window, closed | 14 | a scream gets out through a window |
| Window, broken | 2 | |
| Door, open | 1 | |
| Door, closed (unlocked) | 14 | set by the eavesdrop case, §4.5 — do not raise without rechecking it |
| Door, closed & locked/reinforced | 22 | |
| Interior wall | 32 | |
| Exterior wall | 46 | **primary tuning knob** |

People never contribute loss. A neighbour standing in a doorway does not make the
house soundproof, for the same reason `RLTile.isPathBlocked()` excludes them.

### 4.2 Source loudness

`SoundKind` enum, one value each:

| Kind | dB | Kind | dB |
|---|---|---|---|
| `WHISPER` | 18 | `PUNCH` | 32 |
| `TALK` | 32 | `KNIFE` | 26 |
| `SHOUT` | 55 | `BONE_BREAK` | 48 |
| `SCREAM` | 70 | `BODY_FALL` | 40 |
| `FOOTSTEP_SNEAK` | 8 | `DOOR_OPEN` | 22 |
| `FOOTSTEP_WALK` | 20 | `DOOR_SLAM` | 45 |
| `FOOTSTEP_RUN` | 34 | `DOOR_KICK` | 60 |
| `GLASS_BREAK` | 58 | `GUNSHOT` | 92 |

`FOOTSTEP_SNEAK` at 8 is below the global floor: it is inaudible everywhere by
construction, and the flood early-outs without doing any work. That is intended —
it makes sneaking a real state, and costs nothing.

### 4.3 Listener threshold

Base `12` for an alert adult. Additive modifiers:

| Condition | +dB |
|---|---|
| Asleep | +16 |
| Drunk | +8 |
| Mid-conversation | +6 |
| Elderly | +6 |

### 4.4 Ambient noise floor

Per tile, from `RoomType` (indoors) or road/park (outdoors), scaled by
`WorldTimer.is_night()`.

| Location | Day | Night |
|---|---|---|
| Road / street | 22 | 8 |
| Park / open ground | 6 | 4 |
| Indoor room (generic) | 10 | 4 |
| `SHOP_FLOOR`, `LOBBY`, `RECEPTION` | 26 | 6 |
| `PRIVATE_ROOM` (brothel) | 30 | 30 |
| `CELL`, `VAULT` | 4 | 4 |

A sound must beat `ambient + 3` to register — the just-noticeable difference.

**As built, only the first three rows are reachable.** `Ambient` keys off
`RLTile.isIndoor()` and `TileType.ROAD`, which is all a tile carries at runtime;
the per-`RoomType` rows need room retention (§11) and their constants sit unused in
`SoundConfig` until then. The day/night street split — the row that actually drives
the "kill at noon vs. kill at 3am" mechanic — works today.

### 4.5 Worked examples (verify these in tests)

Counting convention: `d` is cardinal steps, and **entering** a tile pays that tile's
loss — so crossing a door costs `AIR_CARD + TL_DOOR` for the door tile itself, plus
another `AIR_CARD` to step off it. `heard` is strict (`received > threshold`), so an
exact tie is inaudible; the numbers below are the last audible `d`.

| Scenario | Arithmetic | Result |
|---|---|---|
| Talk (32) outdoors, night (ambient 8 → thr 12) | `32 − 2d ≥ 13` | **9 tiles** — today's `earshotRadius` is 10 |
| Talk (32), DIRECTED band | `32 − 2d ≥ 26` | **3 tiles** — today's `directedRadius` is 4 |
| Talk (32) through an interior wall | `32 − (2+32) < 0` | **inaudible** |
| Talk (32), speaker and listener each flanking an **open** door | `32 − (2+1) − 2 = 27`, then `−2d ≥ 13` | **7 more tiles** — the next room hears you plainly |
| Talk (32), speaker and listener each flanking a closed door | `32 − (2+14) − 2 = 14` | **audible** — eavesdropping works |
| …same, listener one tile further back | `14 − 2 = 12` | **inaudible** — a one-tile knife edge |
| Scream (70) through an open door onto the street | `70 − (2+1) = 67`, then `−2d ≥ 13` | **27 tiles** — the whole street |
| Scream (70) through an exterior wall | `70 − (2+46) − 2 = 20`, then `−2d ≥ 13` | **3 tiles** past the wall face |
| Gunshot (92) through an exterior wall | `92 − 50 = 42`, then `−2d ≥ 13` | **14 tiles** indoors |
| Gunshot (92) outdoors | `92 − 2d ≥ 13` | **39 tiles** (`maxRange` bound is 40) |
| Walk (20) through a closed door | `20 − (2+14) − 2 = 2` | **inaudible** — you can sneak past |
| **Knife (26) on a busy street, day** (ambient 22 → thr 25) | `26 − 2d ≥ 26` | `d = 0` — **nobody but the victim** |
| **Knife (26) in a stairwell, 3am** (ambient 4 → thr 12) | `26 − 2d ≥ 13` | **6 tiles** |

The last two rows are the point of the whole exercise: the same murder is silent at
noon on the high street and reported at 3am in a stairwell, with no special-casing.

Rows 1 and 2 reproduce the existing hand-tuned speech radii from first principles
(9 vs. 10, 3 vs. 4), so the speech migration should be near-neutral outdoors and
correct indoors for the first time.

Rows 4 and 5 are what sets `TL_DOOR_SHUT` at 14 rather than 16. At 16 the eavesdrop
case computes to exactly 12 and fails the strict test, and pressing your ear to a
door stops working. If you retune door loss, recheck this pair first.

### 4.6 What the *player* hears

The player is a listener like anyone else, and gets the same `received > threshold`
test at their own tile. What is different is that they also have a screen, so speech
reaches them through two senses that can disagree — and the cases where they disagree
are the interesting ones. `PlayerEars` crosses them:

|  | **heard** | **not heard** |
|---|---|---|
| **seen** | `WORDS` — the line, in a bubble over their head | `LIPS` — a `...` bubble: you can see them talking and that is all |
| **unseen** | `EARSHOT` — a message-log line with a bearing, no bubble | `NOTHING` |

Seeing is `RLTile.isVisible()`, the renderer's own FOV mask — deliberately, because
the question a bubble asks is *"is there anything on screen to hang this on"*, and
that flag is the exact answer. (It is the wrong flag for anyone else: see pitfall 6
and `CrimeSensor`, where using the player's mask for an NPC made a crate a witness.)

`EARSHOT` is anonymous — *"You hear someone to the south-west say: …"*. Hearing gives
you the words, not the face, and naming the speaker would hand the player an
identification through a wall that they never earned. The bearing comes free from the
direction field, so it points at the doorway the voice came out of rather than at the
speaker.

Before this the player was wired straight to the raw `EChatMessage`, which is
broadcast to the whole layer: every line anyone said anywhere in town produced a
bubble, frequently floating in the black void outside the player's FOV, and a chat log
transcript of conversations four streets away through two exterior walls.

Note what falls out of §4.4 with no extra rules: on the high street at noon (ambient
22 → threshold 25) speech carries three tiles, so most of what you can see people
saying arrives as `LIPS`. The same conversation at 3am is `WORDS` across the road.
Getting close enough to hear is a thing the player now has to do.

---

## 5. Algorithm

### 5.1 Shape

One Dijkstra flood per sound event, over the tile grid, from the source outward,
terminating when `loudness − loss < FLOOR`.

```
budget   = loudness − FLOOR            // total loss we can afford
maxRange = budget / AIR_CARD           // bounding box half-width
```

Early-out **before allocating anything** if `budget <= 0`, or if no entity lies
within the `maxRange` Chebyshev box (cheap scan over the layer's entity list).

### 5.2 Bucket queue, not a heap

Total loss along any path is bounded by `budget` (≤ ~80), and every edge weight is a
small integer. So use a flat bucket array indexed directly by loss — no circular
buffer, no comparator, no boxing:

```java
List<int[]>[] buckets = new List[budget + 1];   // or int[] head/next chains
for (int cost = 0; cost <= budget; cost++) {
    // drain bucket[cost]; every relaxation lands in a strictly higher bucket
}
```

Because we drain in ascending cost order and never push backwards, the first time a
tile is popped its loss is final. This is Dial's algorithm; it is O(cells + budget)
and allocation-free if you reuse the scratch buffers.

### 5.3 Buffers

Dense arrays over the audible bounding box, held as reusable scratch on the
`Acoustics` singleton:

```java
private int[]  loss;   // (2R+1)^2, INT_MAX = unreached
private byte[] dir;    // (2R+1)^2, direction the wave ENTERED from (0..7, -1 = source)
```

Worst case is `GUNSHOT` outdoors: R=40 → 81×81 = 6561 cells ≈ 26 KB of `int`. Typical
sounds are R=3..11 → 50–500 cells. Grow the buffers on demand; never shrink.

### 5.4 The direction field

`dir[cell]` stores which of the 8 neighbours the wavefront arrived **from** — i.e.
one step back along the cheapest acoustic path. Two uses, both free:

- **Investigation.** Follow `dir` repeatedly and you walk the propagation path back
  to the source, through the doorway it actually came through. No A\* needed, and it
  reproduces the Thief behaviour where a guard heading for a noise goes to the door
  rather than into the wall.
- **Player messages.** `"You hear a scream to the north-east, muffled."` The
  compass word comes from `dir` at the player's tile; "muffled" from the ratio of
  `received` to `loudness`.

### 5.5 Fields are transient

Unlike `LightMap`, there is no cache and no epoch. A field is computed inside event
dispatch, queried, and discarded — the scratch buffers are reused, the field is not.
Keep a reference to the last-computed field **only** for the debug overlay (§7).

---

## 6. API surface

New package `serialkiller/src/main/java/com/nuclearunicorn/serialkiller/game/sound/`:

| Class | Role |
|---|---|
| `SoundKind` | Enum of §4.2, each with a `dB()` |
| `SoundEvent` | `extends PointBasedEvent`; carries `SoundKind kind`, `EntityActor source`, `int loudness` (kind's dB, adjustable per-emitter) |
| `SoundField` | Result: bounds, `loss[]`, `dir[]`; `received(x,y)`, `directionAt(x,y)`, `heardBy(Entity)` |
| `Acoustics` | The flood. `SoundField propagate(Point origin, int loudness, int layerId)`, plus `emit(SoundEvent)` which floods, then delivers to every audible listener |
| `Ambient` | `int at(RLTile)` — §4.4 |
| `SoundConfig` | Tunables, §8 |
| `PlayerEars` | The player's own hearing, §4.6. Crosses sight with `received` and answers `WORDS`/`LIPS`/`EARSHOT`/`NOTHING` |

`SuspiciousSoundEvent` is **deleted**; `SoundEvent` replaces it. Consumers switch on
`SoundKind` where they currently assume "suspicious".

### 6.1 Delivery

`Acoustics.emit()` owns dispatch, replacing
`SocialController.e_on_event`'s radius loop:

```java
SoundField field = propagate(origin, loudness, layer);
for (Entity ent : entitiesInBox(field.bounds)) {
    int received = field.received(ent.x(), ent.y());
    if (received <= threshold(ent, field.ambientAt(ent))) continue;
    ent.getAI().e_on_event(new SoundHeard(event, received, field.directionAt(ent)));
}
```

`SoundHeard` is what the AI actually sees: the original event plus *this listener's*
received level and arrival direction. That is the Project Acoustics parameterisation
(loudness + direction), which is all a game AI needs.

Speech does not go through `emit()`. It arrives as an `EChatMessage`, which the world
already broadcasts, and three listeners independently ask what to do with it:

| Listener | Question | Answer |
|---|---|---|
| `HearingSensor` | who in town heard this | a `Stimulus` + a transcript line per NPC in earshot |
| `PlayerEars` | what did *the player* get | §4.6's four-way verdict |
| `EffectsSystem` / `NE_GUI_Chat` | what do we draw | whatever `PlayerSpeech` says |

`PlayerSpeech` is the seam: an engine-side interface with a "hear everything" default,
because `libroguelike` has neither acoustics nor a field of view and must not grow
either. `PlayerEars` installs itself into it per world. The verdict is memoised against
the chat event, so three consumers asking in an order set by the listener list still
cost one flood.

---

## 7. Tooling (build this first)

Per the project's standing preference for debug harness over guesswork — and because
the cost tables in §4 are unfalsifiable without it.

**`render/overlays/DebugSoundField.java`** — *implemented*. Hold **ALT**, the same
gate `DebugOverlay.debugPathfinding` and `DebugPathfindingGraph` already use, so all
the debug layers come up together. Renders `received` as a heat map with
arrival-direction arrows, showing the last sound anything emitted and falling back to
the player's own footstep footprint when nothing has happened recently.

Drawn from inside `SceneRenderer.render()` rather than the screen-space overlay pass:
the camera matrix is still applied there, so `Grid.cellX/cellY` maps tiles directly,
exactly as `LightMap` does. The screen-space pass would need the projection undone by
hand.

Being able to *watch* a gunshot leak through a doorway and die against brick is what
makes §4 tunable. Build it in Phase 1, not last.

**`-Dlrl.sound=<KIND>`** — *implemented*. Pins the overlay on with no key held and
floods that kind from the player each frame, so `scripts/shot.sh` can capture the
field offscreen. `-Dlrl.sound=true` just forces the overlay on. Follows the
`RenderConfig.flag()` system-property convention.

```
LRL_SEED=7 LRL_OPTS="-Dlrl.sound=SCREAM -Dlrl.reveal=true" ./scripts/shot.sh out.png
```

**Player noise ring.** Once the field exists, flood from the player's own footsteps
each turn and render it faintly. This is Splinter Cell's noise meter, made spatial —
the player sees exactly which rooms their footsteps are reaching. It turns an
invisible simulation into the stealth feedback loop, and costs one extra flood of
radius ≤ 11 per turn.

### 7.1 Tests

`serialkiller/src/test/.../game/sound/AcousticsTest.java`, built on the existing
`TownFixture` (which already generates a real town headless, no GL context):

1. Every row of §4.5, as a table-driven assertion on a synthetic 3-room fixture.
2. **Wall isolation**: over 20 seeds, a `TALK` in any interior room is inaudible in
   any non-adjacent room.
3. **Open-door leak**: unlock a door between two rooms, assert a `TALK` that was
   inaudible becomes audible.
4. **Diffraction**: in an L-shaped corridor with no line of sight, a `SHOUT` is heard
   round the bend. (This is the test that fails if someone reintroduces shadowcast.)
5. **Direction sanity**: following `dir` from any audible tile terminates at the
   source in ≤ `budget` steps and never enters a tile with `loss == INT_MAX`.
6. **Monotonicity invariant**: `received` never increases along any `dir` chain.
   Cheap, and it catches bucket-queue bugs immediately.
7. **Budget**: `GUNSHOT` on an open map visits < 6000 cells.

---

## 8. Tuning constants (single place)

```java
public class SoundConfig {
    public static int AIR_CARD      = 2;    // dB per cardinal step
    public static int AIR_DIAG      = 3;    // dB per diagonal step
    public static int FLOOR         = 12;   // global cutoff; below this nobody hears
    public static int JND           = 3;    // must beat ambient by this much
    public static int DIRECTED_LEVEL= 26;   // received >= this => Salience.DIRECTED

    // transmission loss, §4.1
    public static int TL_PROP       = 4;
    public static int TL_TREE       = 3;
    public static int TL_WINDOW     = 14;
    public static int TL_WINDOW_BROKEN = 2;
    public static int TL_DOOR_OPEN  = 1;
    public static int TL_DOOR_SHUT  = 14;   // locked or not; see pitfall 4
    public static int TL_WALL_INNER = 32;
    public static int TL_WALL_OUTER = 46;

    // listener thresholds, §4.3
    public static int HEAR_BASE     = 12;
    public static int HEAR_ASLEEP   = 16;
    public static int HEAR_STUNNED  = 8;
}
```

As built, the listener modifiers are only the two the engine can actually answer:
asleep (the AI is in `SleepAction.STATE`) and stunned/fainted (from `BodySimulation`).
There is no drunkenness in the game, and "mid-conversation" would mean reaching into
the LLM transcript — a constant nobody reads is worse than a missing one, so both were
left out rather than left dead.

---

## 9. Integration notes & pitfalls (read carefully)

1. **`RLTile` has no wall *type*.** It knows `isWall` and `isWallGap`, not whether a
   wall is a building boundary. Add `byte soundLoss` to `RLTile` and write it during
   generation, where the answer is already known: `TownChunkGenerator.placeWall()`
   sets inner vs. outer from the `GridMask` (which already distinguishes
   building-boundary cells), `clearWall()` zeroes it, `punchDoor()` and
   `placeWindow()` set the door/window value. Acoustics then needs zero knowledge of
   buildings.

2. **Doors and windows are entities on non-wall tiles.** Read
   `TownChunkGenerator.java:1688-1710`: both `punchDoor` and `placeWindow` call
   `clearWall()` first, so the tile is `isWall == false, isWallGap == true` with an
   `EntityDoor` / `EntityFurniture("window")` on it. Do **not** walk `ent_list` in
   the flood's inner loop — bake the value into `RLTile.soundLoss` and keep the
   inner loop a pure array read.

3. **Keep `soundLoss` in sync.** Recompute the tile's value wherever an acoustically
   relevant entity changes. `EntityDoor.lock()`/`unlock()` already call
   `LightMap.invalidate()` — add the `soundLoss` write in the same two places. Note
   `Entity.tile` is assigned by `WorldTile.add_entity`, so it can be null before
   placement; guard it. Add an invariant test that recomputes every tile from
   scratch after a generated town and asserts it matches the incrementally
   maintained value — this class of drift bug is otherwise invisible.

4. **`EntityDoor.locked` *is* the open/shut state, and it is one-way.** Read the two
   methods rather than their names: `lock()` draws `+`, blocks movement and casts a
   shadow; `unlock()` draws `/`, lets people through and lets light past. That is
   shut and open. Sound keys off it directly — open pays `TL_DOOR_OPEN`, shut pays
   `TL_DOOR_SHUT` — and the *first cut of this got it backwards*, baking every door
   as shut, which sealed rooms that are physically connected and is the one bug you
   will actually notice in play. `SoundInvariantsTest.openDoorsLetSoundThrough` and
   `AcousticsTest.conversationCarriesThroughAnOpenDoor` exist to stop it recurring.

   `TL_DOOR_LOCKED` is gone: locking a door adds no mass, so it cannot change the
   transmission loss. What is genuinely missing is a *close* action — `unlock()` is
   one-way, so once an NPC opens a door it stays open forever, and the world contains
   no shut-but-unlocked door at all. Every generated door is open except bank/office
   entrances and vault rooms. Until closing exists, "shut the door before you start"
   is not a move the player can make, and that is the interesting half of the
   mechanic (Phase 4).

5. **Fix the player-only gate.** `RLCombat.java:167` emits sound only when the
   attacker is the player. Remove the condition — NPC-on-NPC violence must be
   audible or the town's sensor net is decorative. Expect an increase in police
   dispatch traffic; `SocialController`'s `dispatched` dedup set already handles the
   repeat-report case.

6. **Speech migration is a behaviour change even though the radii nearly match.**
   `HearingSensor` currently bands on radius; it should band on `received` against
   `DIRECTED_LEVEL`. §4.5 lands at 3/9 against today's 4/10 — one tile tighter on
   both bands, which is within playtest noise outdoors but is the whole point
   indoors, where a wall now exists between the speaker and the listener. So the
   nearest-listener hack at `HearingSensor.java:69` should be **deleted, not ported**
   — it exists to patch the dead band, and the dead band is gone once loudness is
   real. Verify by playtest before removing; if it still misfires, raise
   `SoundKind.TALK` rather than reinstating the hack.

7. **`SocialController` keeps its radio.** `dispatchToPolice` deliberately has no
   falloff (see its javadoc). Do not acousticise it — a radio is not a sound. Only
   the `SuspiciousSoundEvent` branch at lines 116-123 is replaced.

8. **`PainSensor` references `SuspiciousSoundEvent` in a comment** (line 20). Update
   the prose when you delete the class.

9. **Determinism.** The flood must be deterministic given the same tiles: iterate
   neighbours in a fixed order and break ties in the bucket queue by insertion order.
   Replay (`REPLAY.md`) depends on this.

10. **TeaVM.** No `Math.log` in the flood (§3.1 removes the need). No streams. Prefer
    `int[]` head/next chains over `List<int[]>[]` for the buckets if profiling shows
    allocation pressure.

11. **Z-layers.** Flood within one layer only. Basements are acoustically isolated
    until someone specifies stair coupling; do not silently leak across `zindex`.

12. **`HearingSensor` must re-subscribe on every world.** Not an acoustics problem, but
    it presents as one: "New game" runs `ClientGameEnvironment.reset()`, which empties
    the event manager's listener list, and the sensor's `init()` used to return early
    when its static instance already existed. The second town of a session was therefore
    completely deaf — NPCs heard neither the player nor each other — while every layer
    below (field, thresholds, doors) was working perfectly. Pinned by
    `SensorRewiringTest` and `EnvironmentResetTest`. The general rule: a service outside
    the environment re-subscribes on the way in; it never guards on having been created.

13. **"They talk to each other through walls" is two separate bugs, and only one of
    them is acoustic.** Hearing is `HearingSensor` and goes through the field. Being
    *told someone is there* was `Perception.appendNearby`, which used
    `Fov.in_range` — a class named for field of view containing no field of view, only
    a squared-distance test. An NPC handed `Nearby: BRET MAYNARD` through a bedroom
    wall will address him, and no amount of correct acoustics stops it. Fixed with
    `Sight.canSee`, a Bresenham ray (`SightTest`); the same check now gates *"The
    player is watching you"*, which used to fire on a player standing in the street
    outside.

    Two things to know about `Sight`. Its board treats **actors as transparent** —
    otherwise a crowded street makes everyone in it invisible to everyone else — and
    it exempts both endpoints, because an actor's own tile counts as blocked and
    rlforj tests the starting cell. And it uses `BresLos(false)` with the reverse ray
    cast by hand: rlforj's own `symmetric` branch dereferences the projection path it
    was told not to compute, so it throws the first time a ray is blocked and then
    silently stops throwing, because the failed attempt left the field non-null.

14. **One chat event, three consumers, no ordering.** `EffectsSystem` (bubble),
    `NE_GUI_Chat` (log) and `PlayerEars` (message line) all see the same
    `EChatMessage`, and the GUI overlay is notified before the listener list, so
    "compute it in the event handler and read it in the renderer" is not a thing that
    works. The verdict is memoised against the event instance instead; whoever asks
    first pays for the flood. While you are in there: `TooltipSystem extends
    EffectsSystem` and inherited its handler, so every bubble and damage number was
    built and drawn **twice**, from two roots.

---

## 10. Implementation phases (each independently shippable & testable)

> **Status:** Phases 1–3 are implemented and green (153 tests). Phase 4 is not started.
> Two deliberate scope cuts inside 1–3, both listed at the end of this section.

**Phase 1 — The field, and the ability to see it.**
`SoundKind`, `SoundConfig`, `SoundField`, `Acoustics.propagate()`, `RLTile.soundLoss`
written by the generator, `DebugSoundField` overlay + `-Dlrl.soundProbe`. Nothing is
wired to the AI yet; the old `SuspiciousSoundEvent` path still runs untouched.
*Accept:* overlay shows a gunshot leaking through a doorway and stopping at brick;
tests 1, 4, 6, 7 from §7.1 pass; the game boots and plays exactly as before.

**Phase 2 — Combat and footsteps on the new model.**
`SoundEvent` replaces `SuspiciousSoundEvent`, `Acoustics.emit()` replaces the radius
loop in `SocialController`, `RLCombat` emits per-weapon kinds and loses the
player-only gate, movement emits footsteps.
*Accept:* an NPC in a sealed room does not report a beating outside it; the same NPC
does report it once the door is unlocked; police still get dispatched; tests 2, 3, 5.

**Phase 3 — Ambient masking, speech, and the player's ears.**
`Ambient`, listener thresholds incl. asleep/drunk, `HearingSensor` banding on
`received`, directional messages to the player via `RLMessages`, player noise ring.
Then §4.6: `PlayerEars` behind the `PlayerSpeech` seam, so bubbles and the chat log are
gated by what the player can see and hear rather than by the broadcast.
*Accept:* a knife kill on a daytime street goes unreported while the same kill in a
3am stairwell is reported; sleeping NPCs ignore what waking ones notice; speech
behaviour outdoors is unchanged from before Phase 3; no bubble is ever drawn on a tile
the player cannot see.

**Phase 4 — Investigation uses the direction field.** *(not started)*
`InvestigateAction` follows `dir` instead of pathing to the raw origin. A door *close*
action, so shutting one is a move the player and the AI can make (pitfall 4).
Optional: room-portal coarse propagation (§11).
*Accept:* an NPC hearing a noise through a doorway walks to the doorway first;
screen-recorded before/after.

### 10.1 What was cut from 1–3, and why

1. **Movement does not emit footsteps.** `SoundKind.FOOTSTEP_*` exists, the overlay
   floods it to draw the player's noise ring, but nothing hooks entity movement. Every
   NPC stepping every turn would be ~30 extra floods and 30 extra dispatch sweeps per
   turn to deliver a 20dB sound that nothing currently reacts to — the AI ignores
   non-suspicious kinds. It becomes worth wiring the moment Phase 4 gives an
   investigating NPC a reason to care that someone is moving nearby.
2. **`SoundHeard` reports the true origin, not the heard direction.** `PedestrianAI`
   passes `heard.getOrigin()` to `reportCrime`, so an NPC who heard a scream through a
   wall reports where it *actually* happened rather than where they think it came
   from. Correcting that means walking `fromDir`, which is Phase 4's job; doing it here
   would have changed the input to every existing crime-scene consumer at the same time
   as replacing the propagation model underneath them.

---

## 11. Deferred: the room-portal tier

Thief's actual system was a precomputed room/portal graph, and this generator already
produces `Building.roomList` with typed `Room` rects. A graph of a few dozen nodes
instead of thousands of tiles would be far cheaper at long range, and would give
*semantics* the tile flood cannot: "the noise came from inside the Kowalski flat".

Two reasons it is not the foundation:

- Rooms are **not retained past generation** — `Building.roomList` is discarded once
  tiles are stamped. Retaining it is real work with real serialization consequences.
- Outdoor space has no rooms, and most of this game happens on the street.

The right eventual shape is two-tier: tile flood for near-field and outdoors, portal
graph for long-range indoor propagation, meeting at building entrances. Revisit after
Phase 4, once the tile system has proven its cost tables in play.

---

## 12. Prior art (why the design is what it is)

| Source | What we took |
|---|---|
| **Doom (1993)**, `P_RecursiveSound` | Sound floods the *connected topology*, not straight lines. Its limitation — flood stops after 2 sound-blocking lines, no attenuation — is what §3 replaces with real accumulated loss. |
| **Thief: The Dark Project (1998)**, Tom Leonard, GDC 2003 | Attenuation per portal traversal, and — the half everyone forgets — **the AI localises the sound to the portal it came through**. That is §5.4. |
| **Dungeon Crawl Stone Soup**, `noise.cc` | Closest genre match: BFS noise grid with per-step attenuation, extra loss through walls, arrival direction retained for the message log. Effectively the same algorithm, shipped and tuned. |
| **Angband / Sil**, `update_flow()` | The same flood used as the *primary* stealth mechanic rather than flavour: monsters perceive if the flow value at their tile beats their listening skill. Validates §4.3. |
| **Splinter Cell: Chaos Theory** | Loudness against an **ambient noise floor** — you can be loud where the world is loud. §4.4, and the best idea in the pile. |
| **Project Acoustics** (Raghuvanshi & Snyder, MSR; Gears 4, Sea of Thieves) | Not the wave solver — the **parameterisation**. They found games need only arrival loudness, arrival direction, decay. We take the first two (§6.1) and skip the third. |
| **Steam Audio / VRWorks Audio** | Rejected. Runtime path tracing is strong on specular reflection and weak on diffraction without explicit edge-diffraction terms, and diffraction is nearly all of what "can he hear me through the doorway" means. |
