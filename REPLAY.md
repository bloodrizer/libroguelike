# Replay system

Record a session's player input to a file; drive a later session from that file.

The NPC brains are asynchronous, model-driven and several turns deep, so the interesting
bugs live in sequences like *"walk up to the NPC, say something, hit it"* — not something a
unit test can express, and not something a human can repeat identically twice. A replay
turns one session into an artifact a test harness (or an LLM reading the log) can re-run
and inspect.

## Usage

**Every run records by default** to `replays/MM-DD-HH:MM.jsonl` — the session worth
replaying is always the one you did not think to arm beforehand. Opt out with
`-Dreplay.record=false`.

```sh
mvn -o package                                  # once

java -jar serialkiller/target/serialkiller-0.1.0-SNAPSHOT.jar   # records automatically
./scripts/replay.sh record my.jsonl             # same, but pin the filename
./scripts/replay.sh play my.jsonl out.jsonl     # re-run it headless, log to out.jsonl
```

Two runs in the same minute collide, so the second gets a `-2`, `-3` suffix instead of
overwriting the earlier run.

Scenarios can also be written directly, which is what tests should do:

```sh
./scripts/mkreplay.py my.jsonl "wait 400; walk d 2; walk w 9; say hello; wait 900; attack w 2"
```

Actions: `wait <frames>`, `walk <wasd> [n]`, `attack <wasd> [n]`, `say <text>`,
`key <name> [n]`. Frames are 1/60s.

### Scenario commands

A scenario can also **state its situation** instead of navigating to it:

```sh
./scripts/mkreplay.py flee.jsonl "tick 2; tp 77 44; hurt nearest 3; tick 40"
```

| verb | effect |
|---|---|
| `tp <x> <y>` | put the player here |
| `hurt <who> [times]` | the player strikes them; `who` is `nearest`, a name, or a uid prefix |
| `spawn <kind> <x> <y>` | `kind` is `pedestrian` or `police` |
| `settime <hour>` | 0-23. The clock starts at 21:00, so daytime otherwise costs 600 turns of waiting |
| `tick [n]` | advance n turns — **the only way to pass time without a keypress**, since turns advance on player input and `wait` burns frames, not turns |

These are honoured **only while a replay is driving the session**. There is no console; the
records do nothing in a normal run.

`hurt` is the one that earns its keep. Panic, flight, the police report and the whole crime
pipeline hang off one NPC being struck by the player. Scripted through the keyboard that
means solving the building as a maze and hoping the swing connects — an earlier attempt took
six runs and never landed a blow. As `tp` then `hurt` it is two lines and it works every time.

### Options

| Flag / env | Default | Meaning |
|---|---|---|
| `-Dreplay.record=<path\|true\|false>` | `true` | record here; `true` → `replays/MM-DD-HH:MM.jsonl`, `false` disables |
| `-Dreplay.play=<path>` | off | drive input from this file |
| `SPEED=` / `-Dreplay.speed` | `1.0` | playback rate |
| `TAIL=` / `-Dreplay.tailFrames` | `600` | frames to keep running after the last input |
| `-Dreplay.exitAtEnd` | `true` | quit once the tail elapses |
| `SHOW=1` | off | show the window during playback |
| `LLM=0` | off | force LLM NPCs off (fast, FSM only) |
| `-Dreplay.seed=<long>` | recorded | override the seed; see [Determinism](#determinism) |
| `DEBUG="..."` | off | debug flags, e.g. `DEBUG="world=ready census=50 path=validate"` |

`SPEED>1` fast-forwards the *input* only. Inference does not speed up with it, so a fast
replay outruns the very NPC reactions you are trying to observe.

## Debug probes

Standing instruments, off by default. Findings go to stderr as `DEBUG-*` lines and into the
replay log as observations. `DEBUG="a=b c=d"` on `replay.sh` becomes `-Ddebug.a=b -Ddebug.c=d`.

| flag | what it answers |
|---|---|
| `path=validate` | is every route contiguous, non-diagonal where A* built it, and clear of anything the pathfinder itself calls blocked? |
| `world=ready` | is the town **one connected place**, and can everyone reach a bed? One dump at world-ready, naming anyone stranded outside it |
| `world=map` | the finished town as ASCII, one `DEBUG-MAP` line per row — the picture, not the tally |
| `census=<n>` | every n turns, a town-wide tally: states, who is in bed, who holds a route, and **who actually moved** |
| `strict=true` | a violated check throws instead of printing — for a CI run |

`census` exists because the `npc` record only covers humans within twelve tiles of the
player, which answers "did this NPC react to me" and not "does the town work". Measuring a
commute from a near-player sample reads as broken whenever the player walks somewhere quiet.
The telling column is `moving`: NPCs holding a route but not advancing along it are queued
behind something that will never shift.

`world=ready` earns its place too — it is what found that furniture placed in doorways had
split the town into four components with 3 of 46 beds reachable, a generator bug that was
invisible until an NPC needed to walk through it.

`world=map` is the one to reach for when a town is *connected* and still wrong. A count cannot
tell you that a room came out ringed with doors, its windows torn out, or that a house was
furnished as a dormitory — those read at a glance in the picture and nowhere else. Both of
those were live bugs; the map dump is how they were found and how the fix was checked:

```sh
DEBUG="world=map" ./scripts/replay.sh play r.jsonl 2>&1 | grep DEBUG-MAP | sed 's/^DEBUG-MAP //'
```

Tiles are the entity's own render symbol where there is one (`/` open door, `+` locked, `=`
window, `B` bed, `T` tree), else `#` wall, `.` indoor floor, `,` outdoors.

## World-gen invariants, without a replay

The probes above need a session. §C of [INVARIANTS.md](INVARIANTS.md) does not:

```sh
mvn -o test -Dtest=TownInvariantsTest
```

`TownFixture` builds a whole town in the test JVM — no window, no game loop, no input — and
`TownInvariantsTest` asserts C1 (no prop in a doorway or under a window), C2 (no two openings
adjacent) and C3 (every non-police NPC starts in a room of their own home) over six seeds,
the last being the one off the replay that produced the house full of beds.

Six, not one, because every world-gen bug so far has been a rule that holds on most layouts
and fails on the one in front of you. A failure names the seed and the tile, so the town can
be walked with `-Dreplay.seed=<n>` and looked at with `DEBUG="world=map"`.

## File format

JSONL, two kinds of record.

**`input`** — a keypress, played back at its recorded frame:

```json
{"type":"input","frame":700,"key":84,"chr":"t","ctrl":false,"shift":false,"alt":false}
```

Modifiers are recorded because attack is **ctrl+direction** — a replay that dropped
`key_state_ctrl` would silently turn every attack into a walk.

**`cmd`** — a scenario command, dispatched at its frame (playback only):

```json
{"type":"cmd","frame":320,"cmd":"tp","args":["77","44"]}
```

**Observations** — never replayed; this is the output you read and diff:

| type | carries |
|---|---|
| `turn` | turn number, player position |
| `npc` | per-turn brain dump of every human within 12 tiles (see below) |
| `say` | every line spoken, by anyone |
| `damage` | attacker, target, amount, type |
| `trace` | LLM pipeline lines — sensing, queueing, plans, resolution |
| `census` | town-wide tally, with `-Ddebug.census` |
| `world` | connectivity summary, with `-Ddebug.world=ready` |
| `scenario` | each command that ran, and whether it was understood |
| `ready` / `replay-end` / `footer` | session boundaries |

The `npc` record is the one that makes "the NPC ignored me" diagnosable:

```
name=LAJUANA PITTS dist=1 ai=PedestrianAI
state=top=URGENT:the player just attacked you! topSalience=95 focus=same idle=false
      busy=false sinceRequest=2 dialogue=4 attention=712f9bec brain=PedestrianAI
      state=ai_state_FLEEING threat=712f9bec suspect=no scene=no near=true
```

— whether the stimulus arrived at all, what salience it carries *now*, and whether the
trigger or the queue is holding the reaction back. `dialogue` counts the lines held in the
NPC's transcript: an NPC that talks like it has never met you while this reads non-zero is
a prompt problem, not a memory one.

`brain` is the AI class and `state` the behaviour currently holding the body
(`FLEEING` / `PURSUING` / `INVESTIGATING` / `PATROLLING` / `GOING_HOME` / `SLEEPING` /
`DELIBERATE`). `GOING_HOME` is the walk home at dusk and `SLEEPING` is being in the bed at
the end of it — an NPC that moves while `SLEEPING` is a bug, one that moves while
`GOING_HOME` is a commute.
`threat`, `suspect` and `scene` are what the NPC *believes* — who hurt it, who is wanted,
where the last unattended crime was — and they are the inputs the impulses read, so a
`Policeman` sitting at `PATROLLING` with `suspect=no` never got told about the crime,
whereas one at `PATROLLING` with a live `suspect` is a bug in the impulse. Each holds a uid
or a coordinate, or `no`.

When inference is off the whole planner block collapses to `llm=off`; the reflexes are the
same either way, because they are the same classes.

`top` is what would *trigger* a re-plan; `focus` is what the prompt actually leads with, and
reads `same` unless they differ. They diverge exactly when something is still true but has
already been prompted about — `top=none focus=URGENT:…attacked you` is a victim being asked
about the weather while bleeding, and is the shape to look for after any violence.

`ai` is there because the first real bug found this way was not in the salience model at
all: the NPC next to the player had **no AI object**, so nothing could reach it. Dump
every human and name the AI class — filtering to LLM agents makes an inert NPC look like
an absent one, which is the one failure the log most needs to distinguish.

## Determinism

Every gameplay roll comes from `Rng`, whose seed is written into the header and read back
before the world is built. Replaying a file therefore regenerates the same town, the same
names, the same roaming and the same combat rolls. Two runs of the same file with `LLM=0`
are byte-identical.

Randomness is split into **independent named streams** — `worldgen`, `names`, `ai`,
`combat`, `world`. A stream is a function of the seed and its name alone, not of what any
other stream has consumed. With one shared sequence, changing where a crate goes reshuffled
the names, the combat and the AI too, so a fixed seed stopped meaning "the same town" the
moment anyone touched the generator — which is exactly when a before/after comparison is
worth most.

```json
{"type":"header","version":2,"seed":424242,"recorded":"...","llmEnabled":"(config)"}
```

Pass `-Dreplay.seed=<n>` to override the recorded seed and re-roll one scenario
deliberately; `mkreplay.py` takes `--seed` and otherwise rolls a fresh one per file. A v1
file (no `seed`) still plays, it just picks a new seed each time.

The seed reaches the town itself. Chunk generation used to seed from `x*10000 + y` — the map
coordinates and nothing else — so every seed built the identical town and `--seed` varied
only the names and the rolls. It now mixes the session seed in, keeping the property the
generators actually relied on (regenerating a chunk reproduces it) while making the seed
mean something. Run a few seeds before trusting a result: a bug that reproduces on one town
and not the next is a bug about that town's shape.

This matters more than it sounds. Before the seed, "walk up to your spouse and hit her"
reproduced about half the time — whether the blow landed depended on whether she happened
to drift a tile that run, and a scenario that silently becomes *"swing at empty air"* is
worse than no scenario.

**Model sampling** is seeded per request from `Rng.seed()`, the prompt text and the turn —
not from queue position, which depends on how long inference took. In practice two runs of
the same file produce identical dialogue, but this is not guaranteed the way the game
thread is: whether a request is submitted at all can depend on wall-clock inference timing,
and a diverged prompt gets a different seed.

Frames are recorded relative to `Replay.markReady()` (the frame the world becomes
playable), so a replay recorded after a slow model download still plays back promptly on a
warm start.

Records are flushed as they are written, so a killed process still leaves a usable file —
one line shorter, still valid JSONL.
