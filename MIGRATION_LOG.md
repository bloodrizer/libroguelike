# Migration log

Step-by-step record of porting libroguelike + serialkiller off IDEA and onto a CLI Maven build, per [PORTING.md](PORTING.md).

Format: each entry is dated, scoped to one task, and records what was done, why, what was verified, and what's open.

---

## 2026-05-11 — Kickoff

Plan agreed: discard `NamelessProject*` modules, then execute Milestone 1 (Maven build, no IDE required).

---

## Step 1 — Remove parked 3D MMO modules

**Goal:** delete the three modules that aren't part of the libroguelike/serialkiller pair, so they don't have to be carried through the Maven setup.

**Actions:**

- `git rm -rq NamelessProjectCommon NamelessProject3dServer NamesProject3dClient` — removed all three module directories from the index in one shot.
- Edited [.idea/modules.xml](.idea/modules.xml): removed the three `<module>` entries pointing at the deleted `.iml` files. Kept `libroguelike` and `serialkiller`.
- Removed two now-orphan IDEA library descriptors:
  - `.idea/libraries/gson_2_2_1.xml` (only the deleted `NamelessProjectCommon/libs/gson-2.2.1.jar` — the live libroguelike build uses `gson-2.2.4.jar`)
  - `.idea/libraries/netty_3_2_5_Final.xml` (no callers remain after `grep -rl "org.jboss.netty" libroguelike/src serialkiller/src` returned empty)
- `git rm -r libroguelike/lib/netty/` — the netty 3.2.5 jar is no longer referenced by anything in the surviving modules.

**Verified:**
- `grep -rl org.jboss.netty libroguelike/src serialkiller/src` → no matches.
- `.idea/libraries/` now lists only the libs that the surviving modules actually use.

**Open questions:** none. The 3D MMO experiment is parked and any future revival can pull from git history.

---

## Step 2 — Reorganise sources to Maven layout

**Goal:** move from IDEA's `src/com/...` to Maven's `src/main/java/com/...` + `src/main/resources/...` so a stock Maven build finds everything without custom `<sourceDirectory>` overrides.

**Actions (libroguelike):**
- `git mv libroguelike/src/com libroguelike/src-tmp-com`
- `mkdir -p libroguelike/src/main/java libroguelike/src/main/resources`
- `git mv libroguelike/src-tmp-com libroguelike/src/main/java/com`
- moved `src/resources/billboard_point.vert` and `src/resources/invalid_texture.png` into `src/main/resources/`, removed the now-empty `src/resources/`.

The two-step rename via `src-tmp-com` is to dodge the case-insensitive macOS filesystem when the destination path's middle segment differs only by depth.

**Actions (serialkiller):**
- Same dance for `serialkiller/src/com` → `serialkiller/src/main/java/com`.
- Moved each top-level subdir of `serialkiller/src/resources/` (`fonts/`, `gfx/`, `namegen/`, `terrain/`, `ui/`, `road_16.png`) into `serialkiller/src/main/resources/`.

**Critical detail — runtime resource paths:**

Code calls things like `NameGenerator.class.getResourceAsStream("/resources/namegen/surnames.csv")` ([NameGenerator.java:32](serialkiller/src/main/java/com/nuclearunicorn/serialkiller/generators/NameGenerator.java#L32)) and `Render.class.getResourceAsStream("/resources/invalid_texture.png")` ([Render.java:66](libroguelike/src/main/java/com/nuclearunicorn/libroguelike/render/Render.java#L66)). The lookup expects `resources/` to be a top-level entry inside the jar.

Maven by default copies `src/main/resources/*` to the jar root, which would put files at `/namegen/...` instead of `/resources/namegen/...` and break every callsite.

Fix: in each module's `pom.xml`, override the resource block with `<targetPath>resources</targetPath>`. That preserves the jar layout the code already expects, with zero changes to Java source.

**Verified:**
- `find libroguelike/src/main -type d` and `find serialkiller/src/main -type d` show clean Maven layout.
- `grep -rn 'src/resources' libroguelike/src serialkiller/src` → no matches (no hard-coded build-tree paths in source).
- `grep -rn 'getResourceAsStream' …` listed every resource-loading site; all use `/resources/...` so the `<targetPath>` fix covers them.

---

## Step 3 — Vendor legacy jars into a project-local Maven repository

**Goal:** make the build hermetic. None of `lwjgl 2.7.1`, `slick-util`, `rlforj 0.2` are on Maven Central, and pulling them at build time would hit corporate proxies in this environment.

**Actions:**
- Created `local-repo/` at the project root.
- Ran `mvn install:install-file -DlocalRepositoryPath=local-repo -DcreateChecksum=true` for each:
  | Jar | groupId : artifactId : version |
  |---|---|
  | `libroguelike/lib/lwjgl-2.7.1/jar/lwjgl.jar` | `org.lwjgl.legacy:lwjgl:2.7.1` |
  | `libroguelike/lib/lwjgl-2.7.1/jar/lwjgl_util.jar` | `org.lwjgl.legacy:lwjgl_util:2.7.1` |
  | `libroguelike/lib/lwjgl-2.7.1/jar/jinput.jar` | `org.lwjgl.legacy:jinput:2.7.1` |
  | `libroguelike/lib/slick-util/slick-util.jar` | `org.newdawn.slick:slick-util:1.0.0` |
  | `libroguelike/lib/rlforj.0.2.jar` | `net.sourceforge.rlforj:rlforj:0.2` |

The `org.lwjgl.legacy` prefix prevents collisions if a future commit adds the modern `org.lwjgl:lwjgl:3.x` deps.

- Wrote [scripts/install-local-jars.sh](scripts/install-local-jars.sh) so `local-repo/` can be regenerated from `libroguelike/lib/` at any time.

**Open question:** keep `libroguelike/lib/` around or delete it? It still holds the OS native libs (`liblwjgl.so` / `lwjgl.dll`) that `local-repo/` doesn't carry, and it's the source of truth for `install-local-jars.sh`. **Decision:** keep both for now; revisit when M2 replaces LWJGL 2 entirely.

---

## Step 4 — Maven POMs

**Files written:**
- [pom.xml](pom.xml) — parent aggregator with both modules listed, `dependencyManagement` for every legacy artifact, `pluginManagement` pinning maven-compiler / maven-jar / maven-resources / maven-dependency versions.
- [libroguelike/pom.xml](libroguelike/pom.xml) — engine module. Depends on lwjgl + lwjgl_util + jinput + slick-util + gson + commons-pool + slf4j + log4j.
- [serialkiller/pom.xml](serialkiller/pom.xml) — game module. Depends on `libroguelike` itself plus `rlforj`. Builds an executable jar (manifest mainClass `com.nuclearunicorn.serialkiller.game.Main`, classpath prefix `dependency/`) and runs `maven-dependency-plugin:copy-dependencies` to drop runtime deps into `serialkiller/target/dependency/`.

**Bug encountered and fixed — `${project.basedir}` resolves to child:**

First attempt used `<url>file://${project.basedir}/local-repo</url>` in the parent. When Maven inherited that into the `libroguelike` module, `${project.basedir}` resolved to `libroguelike/` rather than the reactor root, so it looked for jars at `libroguelike/local-repo/...` and the build failed with `Could not find artifact org.lwjgl.legacy:lwjgl:jar:2.7.1`.

**Fix:** use `${maven.multiModuleProjectDirectory}` instead. That property only resolves when Maven sees a `.mvn/` directory at the reactor root, so `mkdir -p .mvn && touch .mvn/.gitkeep` is also part of this step.

**Java version:** kept on `<source>8</source>` / `<target>8</target>` for now. The codebase compiles with `javac --release 8` flags, JDK 17 just emits deprecation warnings for `java.applet.Applet` (used in [serialkiller/.../MainApplet.java](serialkiller/src/main/java/com/nuclearunicorn/serialkiller/game/MainApplet.java)) and `unchecked` warnings around generic collections in InGameMode. Both are M2 cleanup, not M1 blockers.

---

## Step 5 — Verify CLI build

```
$ mvn clean package
…
[INFO] Reactor Summary for libroguelike (parent) 0.1.0-SNAPSHOT:
[INFO] libroguelike (parent) .............................. SUCCESS [  0.063 s]
[INFO] libroguelike (engine) .............................. SUCCESS [  7.667 s]
[INFO] serialkiller (game) ................................ SUCCESS [  9.739 s]
[INFO] BUILD SUCCESS
[INFO] Total time:  17.509 s
```

Outputs:
- `libroguelike/target/libroguelike-0.1.0-SNAPSHOT.jar` (242,602 bytes)
- `serialkiller/target/serialkiller-0.1.0-SNAPSHOT.jar` (313,985 bytes), executable
- `serialkiller/target/dependency/` (11 jars: legacy LWJGL trio, slick-util, rlforj, gson, commons-pool, log4j, slf4j × 2, libroguelike)

Resource layout inside the jars verified with `unzip -l`:
- `serialkiller-…jar` contains `resources/{ui,terrain,namegen,fonts,gfx}/...` at the expected paths.
- `libroguelike-…jar` contains `resources/{billboard_point.vert, invalid_texture.png}`.

---

## Step 6 — Run script

[scripts/run.sh](scripts/run.sh) wraps `java -jar` with the right `-Djava.library.path` for the host OS and warns on macOS that no LWJGL 2 natives ship for it (see PORTING.md M2).

**Smoke test (macOS arm64 host):**

```
$ ./scripts/run.sh
WARN: no LWJGL 2.7.1 macOS natives present in this repo.
      Display.create() will fail. See PORTING.md M2.
Exception in thread "main" java.lang.UnsatisfiedLinkError: no lwjgl in java.library.path: …
        at org.lwjgl.opengl.Display.<clinit>(Display.java:130)
        at com.nuclearunicorn.libroguelike.render.WindowRender.create(WindowRender.java:43)
        at com.nuclearunicorn.libroguelike.core.Game.run(Game.java:84)
        at com.nuclearunicorn.serialkiller.game.Main.main(Main.java:29)
```

This is the **expected and correct** outcome for M1 on this host:
- `serialkiller.game.Main.main` ran, meaning the manifest `Main-Class`, classpath, and inter-module dependency all wire up correctly.
- `Game.run()` reached `WindowRender.create()`, meaning resource loading and event-bus initialisation work.
- The crash is the LWJGL 2 native loader looking for `liblwjgl.dylib` that doesn't exist in the repo. On a Linux x86_64 host the same script would launch the game window. M2 replaces LWJGL 2 with LWJGL 3 + GLFW, which has macOS arm64 natives published to Maven Central.

**Bug encountered and fixed — empty bash array under `set -u`:**

Initial script used `exec java "${JAVA_OPTS[@]}" …` with `set -u`. On macOS the array was empty (no native dir set), and bash 3.2 raises `unbound variable`. Switched to `${JAVA_OPTS[@]+"${JAVA_OPTS[@]}"}` which is the canonical "expand only if set" idiom for arrays.

---

## Step 7 — .gitignore hygiene

Updated [.gitignore](.gitignore) to:
- ignore `target/` everywhere instead of two specific module paths
- ignore IDEA noise files that shouldn't be committed (`workspace.xml`, `tasks.xml`, `usage.statistics.xml`, `shelf/`)
- ignore `.vscode/`
- ignore `.DS_Store`

`local-repo/` is intentionally **not** ignored — committing it makes the build hermetic and works around the lack of these legacy jars on public Maven repositories.

---

## Milestone 1 — done

What was achieved:
- Both modules build via `mvn package` from a clean checkout, no IDE required.
- All five legacy jars vendored into `local-repo/`, build is hermetic.
- The 3D MMO experiment is removed.
- Source layout is canonical Maven, no custom directory plumbing in the POMs.
- Resource layout inside the jars matches what the `getResourceAsStream` callsites expect.
- `scripts/run.sh` provides a one-line launch path that handles native-lib wiring per OS.

What did NOT change:
- No Java source was modified. Same fixed-function GL, same LWJGL 2 imports, same slick-util usage. M1 is a build-system-only change.
- The IDEA `.iml` and `.idea/` files are still in the tree. They're now redundant — any modern IDE imports `pom.xml` directly. Recommend removing in M2.

What's next (M2 — see [PORTING.md §5](PORTING.md)):
- Bump compiler `<release>` to 17.
- Replace log4j 1.2.17 binding with logback (CVE hygiene).
- Replace LWJGL 2 with LWJGL 3 + GLFW in [WindowRender.java](libroguelike/src/main/java/com/nuclearunicorn/libroguelike/render/WindowRender.java), [Input.java](libroguelike/src/main/java/com/nuclearunicorn/libroguelike/core/Input.java), [Game.run()](libroguelike/src/main/java/com/nuclearunicorn/libroguelike/core/Game.java#L80).
- Replace slick-util `TextureLoader`/`TrueTypeFont` with STBImage/STBTruetype.
- Decide GL 2.1 compat vs GL 3.3 core (recommendation: core, see PORTING.md §5).

---

## Milestone 2 — LWJGL 3 + GLFW

Goal: replace LWJGL 2.7.1 (no macOS arm64 support, dead since 2014) with LWJGL 3 + GLFW so the game runs natively on Apple Silicon. Decision recorded earlier: **shim layer** approach — keep the game's source unchanged and add small implementations of the LWJGL 2 classes the game imports, in their original packages, delegating to LWJGL 3.

### Step 1 — Toolchain & deps

- Bumped `<source>`/`<target>` from 8 to 17 in the parent POM.
- Replaced LWJGL 2 dep block with the LWJGL 3 BOM (`org.lwjgl:lwjgl-bom:3.3.6` imported as `<scope>import</scope>` `<type>pom</type>`).
- libroguelike module declares `lwjgl`, `lwjgl-glfw`, `lwjgl-opengl`, `lwjgl-stb`, plus a runtime-scope copy of each with the per-OS native classifier.
- Native classifier resolved via parent-POM `<profiles>`: `natives-macos-arm64`, `natives-macos`, `natives-linux`, `natives-linux-arm64`, `natives-windows`. OS+arch detection happens automatically via Maven's `<activation><os>…</os></activation>`.
- Bumped gson 2.2.4 → 2.10.1 and slf4j 1.7.5 → 2.0.13 to match logback 1.5.x.
- Replaced log4j 1.2.17 binding with logback 1.5.12 (slf4j-log4j12 dropped). Old `application.properties` deleted; new `libroguelike/src/main/resources/logback.xml` lands at the jar root via a dedicated `<resource>` block in `libroguelike/pom.xml`.
- `scripts/install-local-jars.sh` simplified — only slick-util and rlforj need to be vendored now. The LWJGL 2 jars are no longer referenced.

### Step 2 — Shim classes for the LWJGL 2 → LWJGL 3 surface

Audited the entire codebase for LWJGL 2 imports:

| Package | Used | Status in LWJGL 3 |
|---|---|---|
| `org.lwjgl.opengl.Display` / `DisplayMode` | yes | gone — must shim |
| `org.lwjgl.opengl.GLContext` | yes | renamed to `GL.getCapabilities()` — must shim |
| `org.lwjgl.opengl.ARBDebugOutputCallback` / `AMDDebugOutputCallback` | yes | replaced by `GLDebugMessageARBCallbackI`/`AMDCallbackI` lambdas — patched the callsite directly in `WindowRender.java` (only place using these) |
| `org.lwjgl.opengl.EXTFramebufferObject` | yes | still present in LWJGL 3, no shim needed |
| `org.lwjgl.opengl.GL11` / `GL12` / `GL14` | yes | still present, identical signatures |
| `org.lwjgl.input.{Keyboard, Mouse, Cursor}` | yes | gone — must shim |
| `org.lwjgl.util.Point` | yes (very widely) | gone — must shim |
| `org.lwjgl.util.vector.Vector3f` | yes (one file) | gone — must shim |
| `org.lwjgl.util.glu.GLU.gluPerspective` | yes | gone — must shim |
| `org.lwjgl.LWJGLException` | yes | gone — must shim |
| `org.lwjgl.BufferUtils` | yes | still present, no shim needed |

Wrote shims under `libroguelike/src/main/java/org/lwjgl/...`:

- `LWJGLException` — empty wrapper exception.
- `util/Point` — int x/y, getters/setters/equals/hashCode (only what callers use).
- `util/vector/Vector3f` — public x/y/z floats + `set(...)`, `getX/Y/Z`, `setX/Y/Z`.
- `util/glu/GLU` — single static `gluPerspective` that builds the matrix and calls `glMultMatrixf`.
- `opengl/Display` — wraps GLFW. `setDisplayMode/setTitle/setVSyncEnabled/create/update/sync/destroy/isCloseRequested/setParent`. `create()` initialises GLFW, requests a 2.1 compatibility context (so `glBegin`/`glEnd` keep working), creates the window centered on the primary monitor, makes the context current, runs `GL.createCapabilities()`, attaches `InputBridge`, shows + focuses the window.
- `opengl/DisplayMode` — width/height holder.
- `opengl/GLContext` — `getCapabilities()` returns an LWJGL 2-shaped `ContextCapabilities` populated from `GL.getCapabilities()`. Required because slick-util's compiled bytecode (initially still on classpath) references the LWJGL 2 type.
- `opengl/ContextCapabilities` — boolean fields the game queries (`GL_ARB_vertex_buffer_object`, `GL_ARB_debug_output`, `GL_AMD_debug_output`, `GL_EXT_framebuffer_object`, `GL_EXT_texture_mirror_clamp`).
- `input/InputBridge` — internal; holds GLFW callbacks + per-frame event queues. `attach(window)` registers char/key/mousebutton/cursor-pos/scroll callbacks; `beginFrame()` clears the queues each frame before `glfwPollEvents`.
- `input/Keyboard` — LWJGL 2-style polling API (`next()`, `getEventKey()`, `getEventKeyState()`, `getEventCharacter()`). Key constants alias to GLFW codes — safe because the game only ever compares them by name.
- `input/Mouse` — `next()`, `getEventButton()`, `getEventDX/DY()`, `getX/Y()` (Y inverted to LWJGL 2 convention), `isButtonDown(b)`, `setGrabbed(...)`, no-op `setNativeCursor(...)`.
- `input/Cursor` — opaque struct, never actually applied (Mouse.setNativeCursor is no-op for now; the legacy bitmap-cursor code in `Render.set_cursor` is best-effort).

`WindowRender.java` was patched in place for the two debug-callback sites — LWJGL 3 uses lambdas that take `(source, type, id, severity, length, messagePtr, userParam)` rather than legacy `Callback` objects.

### Step 3 — Slick-util replacement

slick-util (the texture/font helper) is shipped as a binary jar from 2011 with calls to LWJGL 2 GL11 method overloads that LWJGL 3 has renamed (`glGetInteger(int, IntBuffer)` → `glGetIntegerv`). Three options were considered:

1. ASM-rewrite the slick-util bytecode at install-time.
2. Rebuild slick-util from source.
3. Reimplement the small surface the game uses with STB.

Chose **option 3** (per request) — smallest surface, no extra build steps. Wrote four classes under `libroguelike/src/main/java/org/newdawn/slick/...`:

- `Color` — int and float ctors, the named statics the game uses (`white`, `black`, `red`, `green`, `blue`, `yellow`, `orange`, `cyan`, `magenta`, `gray`, `lightGray`, `darkGray`), float `r/g/b/a` fields.
- `opengl/Texture` — id + width/height + image-width/image-height; `bind()` calls `glBindTexture`.
- `opengl/TextureLoader` — `getTexture(format, InputStream)` reads bytes, decodes with `STBImage.stbi_load_from_memory`, uploads via `glTexImage2D(GL_RGBA, GL_UNSIGNED_BYTE)`. NPOT textures used directly — modern GL handles this natively.
- `TrueTypeFont` — bakes glyphs (chars 32-256) from a `java.awt.Font` into a single `BufferedImage` via `Graphics2D.drawString`, converts ARGB → RGBA, uploads as a GL texture, draws strings as one `glBegin(GL_QUADS)` batch per call.

Slick-util jar dropped from POM.

### Step 4 — macOS-specific fixes

A series of macOS-only issues, each isolated by adding diagnostic prints, then removing them once understood:

1. **GLFW thread requirement.** GLFW must run on macOS process main thread. Added `-XstartOnFirstThread` to `scripts/run.sh` for `Darwin*`.
2. **AWT runloop hijacking.** Once `java.awt` is touched (the font baker uses `Graphics2D`), Java's AWT installs `NSApplicationAWT` on the main thread and `glfwPollEvents` blocks forever. Diagnosed via `sample`-stack on the running JVM — the trace ended in `[NSApplicationAWT runAWTLoopWithApp:]`. Fixed by adding `-Dapple.awt.UIElement=true` and `-Djava.awt.headless=true` to `scripts/run.sh`. With those, AWT still rasterises fonts to `BufferedImage` (it's purely off-screen) but doesn't try to seize the runloop.
3. **Viewport cropping (Retina).** Window=1024×768, framebuffer=2048×1536. Initial fix attempted `glViewport(0,0, framebufferSize)` to match physical pixels — produced bottom-left quadrant rendering (game draws in window units, ortho mapped 0..1024 across 2048 viewport, but a follow-up `glViewport` in slick-util-derived code reset it back to window units). Final fix: **viewport in window units, ortho in window units** — Apple's GL-on-Metal layer scales internally for Retina. So: `glViewport(0, 0, w, h); glOrtho(0, w, h, 0, -1, 1);` where w/h are window units.
4. **Tileset not rendering.** Even after viewport was correct the world tiles didn't appear. Probed `RLWorldModel.visit(x,y)` and `ConsoleRenderer.render_tile` — confirmed tile objects are the same and `isVisible()` returns `true` for FOV-marked tiles. The actual culprit: `WorldViewCamera.update()` and `setMatrix()` were never called from anywhere in the codebase, so the camera stayed at `(0,0)` and `tile_in_fov` only matched tiles in the world's first 1024×768 pixel block — never the area around the player. Added both calls at the top of `WorldView.render()` in [libroguelike/src/main/java/com/nuclearunicorn/libroguelike/game/world/WorldView.java](libroguelike/src/main/java/com/nuclearunicorn/libroguelike/game/world/WorldView.java#L155).

### Milestone 2 — done

What works today:
- `mvn package` produces an executable jar with all native LWJGL 3 binaries for the host OS+arch.
- `./scripts/run.sh` opens a 1024×768 window on macOS arm64, renders the world tileset around the player, the GUI overlay, the console TUI, the FPS counter, debug overlay, and version overlay.
- All five LWJGL 2-derived legacy jars (`lwjgl-2.7.1.jar`, `lwjgl_util-2.7.1.jar`, `jinput-2.7.1.jar`, `slick-util-1.0.0.jar`, plus the bundled native dirs) are out of the build. Only `rlforj-0.2.jar` and our own STB-backed slick replacements remain.
- log4j 1.2 is gone; logging goes through slf4j → logback.

What's next (M2b — see [PORTING.md §5](PORTING.md)):
- Replace `glBegin`/`glEnd` immediate mode with batched VBOs and a single textured-quad shader. Required to move to GL3 core profile.
- Wire `glfwSetCursor` so `Mouse.setNativeCursor` shim isn't a no-op.
- Audit `Cursor` shim — currently the cursor texture upload is no-op so the in-game cursor is the OS default.

---

## Milestone 3 — TeaVM WebAssembly

Goal: compile the game to wasm and run it in a browser. Decision taken during the
work: **skip libGDX**. PORTING.md §6 assumed the renderer would be ported onto
libGDX's API to get a web backend for free, but an audit showed the game issues
only ~45 distinct GL calls from 19 `glBegin` sites, all fixed-function 2D. Writing
a GL 1.1 emulation over WebGL was smaller than a libGDX migration and left every
gameplay and renderer file untouched.

Full detail lives in [PORTING.md §10](PORTING.md); this is the sequence and what
each step cost.

### Step 1 — Prove the toolchain before porting anything

TeaVM 0.15.0, `targetType=WEBASSEMBLY_GC`. A hello-world that touched WebGL,
collections and `String.format` compiled to a 213KB wasm and ran under headless
Chromium. Only then was any game code involved.

Wrote [scripts/webtest.sh](scripts/webtest.sh) at this point — it serves
`web/target/web`, loads it in headless Chromium and reads a verdict out of the
page's `#status` element. Every step below was checked with it.

**Bug encountered and fixed — sandboxed network namespaces.** The static server
and Chromium must start in the *same* shell invocation; a server backgrounded in
one command was unreachable from the next. The script also waits for the listener
before launching Chromium, which otherwise lands on an error page and reports a
confusing "no #status found".

### Step 2 — Module layout

`web/` copies the engine and game sources into `target/generated-sources/game`
with an exclude list and compiles them next to `web/src/main/java`. Copy-with-
excludes rather than `build-helper:add-source` over the original trees, because
the wasm build has to replace desktop-only classes *under the same FQN* — a
compiler `<excludes>` pattern would have matched both copies.

The exclusion list is the honest statement of what a browser cannot do, and each
entry either has a replacement in `web/src/main/java` or is documented as absent.

### Step 3 — GL 1.1 over WebGL

`org.lwjgl.opengl.GL11` in the web module accumulates `glBegin`…`glEnd` into a
float array and flushes one `glDrawArrays` per `glEnd`, keeps its own
projection/modelview matrix stacks, expands `GL_QUADS` to triangles (WebGL has no
quads), and maps GL's integer texture names onto `WebGLTexture` objects. One
shader — vertex colour × optional texture — covers every draw the game makes.
The renderer already batched long runs between one begin/end pair, so per-flush
draw calls were never a concern.

### Step 4 — Making the shared code platform-neutral

Six small refactors, all behaviour-preserving on the desktop; see PORTING.md §10
for the list. The two worth repeating: `Game.run()`'s blocking `while` was split
so the browser can drive `runFrame()` from `requestAnimationFrame`, and
`TrueTypeFont` now takes a `FontSpec` rather than a `java.awt.Font`, which is what
freed `OverlaySystem` and `Glyphs` from AWT and avoided duplicating them.

### Step 5 — Getting TeaVM to link

Four rounds, each surfacing one class of gap: slf4j's provider binding, Gson's
reflective type adapters, `Runtime.addShutdownHook` in the replay recorder, and
three missing JDK classes that Gson's `TypeAdapters` initialiser references.

The last needed `--patch-module java.base=...` in a dedicated compile pass, since
javac otherwise refuses to define a class in a `java.*` package. Swapping
`Gson.fromJson` for `JsonParser.parseString` in `CommandRegistry` removed the
reflective path entirely — tree parsing needs no type adapters, and this is
better code on the desktop too.

### Step 6 — Making it actually render

Two silent failures, neither of which threw where the fault was:

1. **Texture uploads did nothing.** Handing a `java.nio.ByteBuffer` to WebGL's
   `texImage2D` relies on a by-reference view Wasm GC does not provide; the call
   succeeded and the texture stayed empty. This rendered as a world with correct
   lighting and no tiles at all. `GL11.glTexImage2D` now copies into a
   `Uint8Array` explicitly.
2. **Missing textures returned null.** The web `Render` skipped the desktop's
   invalid-texture fallback, so callers that dereference the result crashed.

**Bug encountered and fixed — rlforj is miscompiled by TeaVM.**
`PrecisePermissive.visitFieldOfView` null-derefs inside its own scan on any board
with walls at radius ≳12. Ruling it in took a dedicated diagnostic entry point
(`-Dweb.mainClass=...WebProbe`) plus a JavaScript-backend build whose traces keep
Java frames, both of which are now permanent debugging tools:

- the same call succeeds on the JVM, so it is not the algorithm;
- it fails with the vendored Java 5 jar *and* with its sources recompiled by
  javac 17, so it is not the bytecode vintage;
- `LinkedList`/`ListIterator` semantics under TeaVM match the JDK exactly in every
  pattern rlforj uses, so it is not the collections.

The web build substitutes `Shadowcast`, a self-contained recursive shadowcaster,
through a new `FovFactory` seam. `ShadowcastTest` covers the invariants that
matter (radius, wall shadows, a sealed room leaking nothing, a doorway letting
sight through). **This is a genuine behavioural difference** from the desktop
build around corners, and the first thing to revisit if TeaVM fixes the defect.

Also learned: the rlforj jar's bundled `.java` sources are not in sync with its
`.class` files, so recompiling it from what it ships is not a safe swap.

### Milestone 3 — done

- `mvn -pl web package` produces `web/target/web`: an 880KB wasm plus runtime,
  page and assets, servable by any static host.
- The game boots in Chromium, generates a town, spawns ~320 entities, and renders
  tiles, sprites, lighting, the ASCII layer, the GUI and the console at the same
  fidelity as the desktop build (verified by screenshot against `scripts/shot.sh`).
- Keyboard and mouse are wired through DOM events.
- All 162 desktop tests still pass; 7 new tests cover the web FOV.

What's next:
- Measure performance on a real GPU; the headless software-GL numbers say nothing.
- Revisit `Shadowcast` if TeaVM's rlforj defect is fixed, and report it upstream.
- Custom TTFs, bitmap cursors, and `-D` debug flags remain unimplemented on web.
