# libroguelike + serialkiller

A small roguelike engine (`libroguelike`) and the game built on it
Deps:

- **JDK 17**
- **Maven** 
- **LWJGL 3** + GLFW for window/input
- **STBImage** for textures, AWT-baked atlas for fonts
- **logback** for logging (replaces log4j 1.2)

Runs on macOS (Apple Silicon + Intel), Linux x86_64/arm64, and Windows x86_64.

## Quick start

Prerequisites: a JDK 17 (or newer) and Maven on `PATH`.

```sh
# One-time after a fresh clone (populates local-repo/ from libroguelike/lib/).
# Only slick-util and rlforj live there — neither is on Maven Central.
./scripts/install-local-jars.sh

# Build both modules and copy runtime deps into serialkiller/target/dependency/.
mvn package

# Launch the game.
./scripts/run.sh

# Render one frame offscreen to a PNG (no window).
./scripts/shot.sh /tmp/shot.png
```

The launcher picks the right JVM args per OS (e.g. `-XstartOnFirstThread` and
`-Dapple.awt.UIElement=true` on macOS, both required when GLFW shares the main
thread with AWT-based offscreen font rasterisation).

## Repository layout

```
.
├── libroguelike/        # engine module (LWJGL 3 + GLFW + STBImage + AWT fonts)
├── serialkiller/        # game module (depends on libroguelike + rlforj)
├── local-repo/          # project-local Maven repo for slick-util + rlforj
│                        # (gitignored — regenerate with the script above)
├── scripts/
│   ├── install-local-jars.sh
│   ├── run.sh
│   └── shot.sh          # offscreen single-frame screenshot
├── pom.xml              # parent aggregator
├── PORTING.md           # full porting/modernisation plan
├── MIGRATION_LOG.md     # step-by-step record of what changed and why
├── RENDERING.md         # world renderer: projection, lighting, sprite atlas
└── README.md
```

Source layout inside each module is canonical Maven:

```
src/main/java/...
src/main/resources/...
```

The resource block in each module's `pom.xml` keeps the in-jar layout at
`/resources/...` so existing `getResourceAsStream("/resources/foo.png")`
callsites don't need touching.

## Starting a new game

`New game` on the main menu opens a wizard: pick the life you start with, then
`Begin`. A preset is one object — role, spawn place, sex, age range and starting
kit — so adding one is a table entry in
`serialkiller/.../game/character/CharacterPresets.java` and nothing else. The
spawn place is looked up in the town once it exists (a room at the brothel, a
spot on the street); a town that has no such place starts you at home instead.

```sh
# skip the menu and start as somebody: citizen|prostitute|postman|shopkeeper|vagrant|random
./scripts/run.sh -Dlrl.preset=postman

# what the town was built out of, and where it put the player
./scripts/run.sh -Ddebug.world=town
```

## Controls

- `wsad` / arrow keys — move
- `space` — skip turn
- `ctrl` + direction — attack
- `t` — talk to nearby people
- `tab` — character screen / inventory
- `esc` — main menu (and back)
- `n` — new game, from the main menu

### Debug views

Hold `alt` to see what the town is thinking. Two panels share the key:

- right: the **NPC inspector** — one person's body, drives, impulse walk, memory,
  beliefs, reflections and the model's last reply. `F5` pins it, `F6` cycles,
  `F7` dumps the whole brain (last prompt included) to stdout and the replay.
- left: the **town view** — population, the libido drive across the whole town,
  what everybody is doing, the inference queue, and a feed of everything that has
  happened out of your sight. `F8` arms it, `shift+F8` dumps it.

The town feed is the unfiltered record on purpose: the message console only tells
you what you saw or heard, so it is not where you go to find out what the
simulation is doing.

`F1`-`F4` toggle render layers (sprites, smooth light, ascii overlay, reveal map).

## LLM NPCs

Live NPCs need `llama.cpp` on `PATH` and a GGUF staged under `models/`; see
`llm-config.json`. When it will not come up, ask it why instead of watching the
loading screen:

```sh
./scripts/run.sh --llm-check
```

That runs the whole boot path — config, binary, model files, both tiers, one real
completion — and prints where it stopped. Each server's own output is kept in
`logs/llama-<tier>-<port>.log`.

Both tiers pointing at the same GGUF is normal and costs nothing: they share one
`llama-server` rather than loading the weights twice.