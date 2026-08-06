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

## Controls

- `wsad` / arrow keys — move
- `space` — skip turn
- `ctrl` + direction — attack
- `tab` — character screen / inventory
- `esc` — quit