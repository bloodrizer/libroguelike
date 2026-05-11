# Porting libroguelike / serialkiller to modern runtimes

> Audit + migration plan for the ~15-year-old codebase under this tree.
> Goal: get it building from CLI, kill obsolete dependencies, and land on
> a runtime that allows both desktop and web/wasm distribution.

---

## 1. What's actually here

Five IntelliJ modules are wired together via [.idea/modules.xml](.idea/modules.xml):

| Module | Java files | Role |
|---|---:|---|
| [libroguelike/](libroguelike/) | 164 | Core engine: render, input, GUI, world, events, FOV |
| [serialkiller/](serialkiller/) | 67 | The single-player game built on the engine |
| [NamelessProjectCommon/](NamelessProjectCommon/) | — | Shared protocol/entities for the 3D MMO experiment |
| [NamelessProject3dServer/](NamelessProject3dServer/) | — | Netty-based game server |
| [NamesProject3dClient/](NamesProject3dClient/) | — | Client with point-sprite ASCII shader |

Entry points:

- Single-player game: [serialkiller/src/com/nuclearunicorn/serialkiller/game/Main.java:20](serialkiller/src/com/nuclearunicorn/serialkiller/game/Main.java#L20)
- 3D experimental client: [NamesProject3dClient/src/com/nuclearunicorn/negame/client/Main.java](NamesProject3dClient/src/com/nuclearunicorn/negame/client/Main.java)
- 3D experimental server: [NamelessProject3dServer/src/com/nuclearunicorn/negame/server/Main.java](NamelessProject3dServer/src/com/nuclearunicorn/negame/server/Main.java)

The render loop lives in [libroguelike/src/com/nuclearunicorn/libroguelike/core/Game.java:80](libroguelike/src/com/nuclearunicorn/libroguelike/core/Game.java#L80) — it's a classic LWJGL2 `Display.create()` + `Display.update()` + `Display.sync(60)` loop.

### Dependencies in [libroguelike/lib/](libroguelike/lib/)

| Jar | Version | Released | Status |
|---|---|---|---|
| lwjgl | 2.7.1 | 2010 | **Obsolete.** API removed in LWJGL 3 (no `Display`, `Mouse`, `Keyboard`). |
| jinput | bundled with lwjgl 2 | 2010 | Obsolete. |
| slick-util | unknown | 2011-ish | **Dead project.** Pulls TrueTypeFont, Texture, TextureLoader. |
| netty | 3.2.5.Final | 2011 | Obsolete. Netty 4.x is current; API is incompatible. |
| gson | 2.2.4 / 2.2.1 | 2013 | Old but still works on JDK 17. |
| log4j | 1.2.17 | 2015 | EOL, has CVEs. Move to logback or log4j2. |
| slf4j-api | 1.7.5 | 2013 | Old facade, still functional. |
| commons-pool | 1.6 | 2012 | Apache Commons Pool 2 is current. |
| rlforj | 0.2 | 2010-ish | FOV/pathfinding lib. Niche but still works on JDK 17. |

### LWJGL surface in use

From `grep -rho "import org.lwjgl"`:

```
org.lwjgl.BufferUtils
org.lwjgl.LWJGLException
org.lwjgl.input.{Cursor, Keyboard, Mouse}
org.lwjgl.opengl.{Display, DisplayMode, GL11, GLContext,
                  ARBDebugOutput, AMDDebugOutput, EXTFramebufferObject}
org.lwjgl.util.glu.GLU
org.lwjgl.util.vector.Vector3f
org.lwjgl.util.Point
```

OpenGL features used:

- **Fixed-function pipeline:** `glMatrixMode`, `glOrtho`, `glLoadIdentity`, `glPushMatrix` ([WindowRender.java:73-90](libroguelike/src/com/nuclearunicorn/libroguelike/render/WindowRender.java#L73-L90))
- **Immediate mode:** `glBegin(GL_QUADS)` / `glBegin(GL_LINES)` / `glBegin(GL_POINTS)` — present in TilesetRenderer, Render, ConsoleRenderer and others
- **EXT_framebuffer_object** (the old pre-GL3 FBO API) at [FBO.java:14](libroguelike/src/com/nuclearunicorn/libroguelike/render/FBO.java#L14)
- **slick-util** for TTF (`TrueTypeFont`) and PNG loading (`TextureLoader`)
- **GLSL shaders** for point-sprite ASCII billboards: [billboard_point.vert](libroguelike/src/resources/billboard_point.vert), [asciiPoint.vert](NamesProject3dClient/src/resources/shaders/asciiPoint.vert), [asciiPoint.frag](NamesProject3dClient/src/resources/shaders/asciiPoint.frag)

That mix — fixed-function + immediate-mode + EXT FBO + a couple of shaders — pegs the rendering style firmly to OpenGL 2.x compatibility profile. Modern macOS only ships compat profiles up to 2.1, and Apple has formally deprecated OpenGL entirely. WebGL / WebGL2 do **not** have a compatibility profile at all — there is no `glBegin`. Any port to the web requires rewriting the draw calls onto a vertex-buffer, shader-driven pipeline.

### IDE coupling

- All project structure lives in [.idea/](.idea/) and per-module `.iml` files
- No `pom.xml`, no `build.gradle`, no `Makefile`
- [application.properties](application.properties) is just log4j config
- Native libs sit at `libroguelike/lib/lwjgl-2.7.1/native/{linux,windows}/` — **no macOS arm64 binaries**, which is likely why it doesn't run on a current Mac out of the box

---

## 2. Can it run today, as-is?

**On macOS (Apple Silicon, JDK 17)**: no. Three blockers:

1. LWJGL 2.7.1 ships no arm64 macOS native, and the bundled natives in `lib/lwjgl-2.7.1/native/` only cover linux/windows. You'd need to find LWJGL 2.9.x natives (last LWJGL 2 release with macOS support, x86_64 only) and run under Rosetta 2.
2. macOS dropped OpenGL compatibility profile support beyond 2.1; FBO_EXT and `glBegin` happen to still work on 2.1 contexts, but support is unmaintained.
3. log4j 1.2.17 has CVE-2019-17571 and friends — fine for local play but a non-starter for distribution.

**On Linux x86_64 (JDK 8 or 11)**: should still run. Everything in `lib/` resolves; the natives are present. JDK 17 may also work but the immediate-mode GL is on borrowed time even there.

**On Windows x86_64**: same as Linux.

So step zero of any plan is to be able to build it from the command line, then to fix the LWJGL 2 problem.

---

## 3. The migration plan

There are three increasingly aggressive milestones. Each one is independently shippable:

```
M1: CLI build           ─►  (Maven or Gradle, no IDE required)
M2: LWJGL 3 + GL3 core  ─►  (works on modern macOS; immediate mode → VBO+shaders)
M3: libGDX + TeaVM      ─►  (single source compiles to desktop JAR + WebGL/wasm)
```

You can stop at M2 and have a healthy desktop game. M3 is what unlocks the "ship a URL" distribution story.

### Recommended path: **M1 → M2 → M3**

I considered three alternatives for M3 and they're discussed in §6.

---

## 4. Milestone 1 — CLI build (Maven)

**Why Maven over Gradle:** project is small, dependencies are few, declarative POM is easier to read than a Kotlin/Groovy build script. Gradle's only advantage here would be Kotlin DSL niceness, which doesn't matter for a 230-file game.

### Layout transform

Move `src/com/...` → `src/main/java/com/...` and `src/resources/` → `src/main/resources/` so Maven's standard layout finds everything. Do this with `git mv` so blame is preserved.

### Top-level [pom.xml](pom.xml) (multi-module)

```xml
<project>
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.nuclearunicorn</groupId>
  <artifactId>libroguelike-parent</artifactId>
  <version>0.1.0-SNAPSHOT</version>
  <packaging>pom</packaging>

  <modules>
    <module>libroguelike</module>
    <module>serialkiller</module>
    <!-- 3D experiment modules can come along or stay parked -->
  </modules>

  <properties>
    <maven.compiler.source>17</maven.compiler.source>
    <maven.compiler.target>17</maven.compiler.target>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <lwjgl.version>3.3.6</lwjgl.version>
  </properties>
</project>
```

### Per-module [libroguelike/pom.xml](libroguelike/pom.xml)

During M1 the goal is just **compile what's there with Maven**. So keep LWJGL 2 jars temporarily as system-scoped or install them locally:

```bash
mvn install:install-file -Dfile=libroguelike/lib/lwjgl-2.7.1/jar/lwjgl.jar \
    -DgroupId=org.lwjgl.legacy -DartifactId=lwjgl -Dversion=2.7.1 \
    -Dpackaging=jar
# repeat for lwjgl_util, jinput, slick-util, rlforj, netty 3.2.5
```

Then declare them in the POM. This isolates the *build system* migration from the *dependency* migration — keep the diff small.

### Run from CLI

```bash
mvn -pl serialkiller -am package
java -Djava.library.path=libroguelike/lib/lwjgl-2.7.1/native/<os> \
     -cp serialkiller/target/serialkiller-0.1.0-SNAPSHOT.jar:... \
     com.nuclearunicorn.serialkiller.game.Main
```

Wrap that in a `run.sh` and a `Makefile` target. The IDEA `.iml` and `.idea/` directories can stay in the repo or be deleted — Maven will regenerate IDE projects on demand (`mvn idea:idea`, or any modern IDE imports `pom.xml` directly).

**Effort:** ~half a day. Mostly mechanical.

---

## 5. Milestone 2 — LWJGL 3 + modern GL

LWJGL 3 is a hard break from LWJGL 2. Roughly:

| LWJGL 2 | LWJGL 3 equivalent |
|---|---|
| `org.lwjgl.opengl.Display` | **GLFW** (`org.lwjgl.glfw.GLFW.glfwCreateWindow`) |
| `org.lwjgl.input.Keyboard` | GLFW key callbacks |
| `org.lwjgl.input.Mouse` | GLFW cursor + mouse-button callbacks |
| `org.lwjgl.util.vector.Vector3f` | **JOML** (`org.joml.Vector3f`) — separate dep |
| `org.lwjgl.util.glu.GLU.gluPerspective` | gone — compute the matrix in JOML |
| `EXTFramebufferObject` | `GL30.glGenFramebuffers` (FBO is core in GL3) |
| slick-util `TrueTypeFont` | `STBTruetype` via LWJGL 3 (`org.lwjgl.stb.*`) |
| slick-util `TextureLoader` | `STBImage.stbi_load` (`org.lwjgl.stb.STBImage`) |

The `Display`/`Keyboard`/`Mouse` classes are *gone* in LWJGL 3 — every callsite needs to change. Concrete touch-points:

- [WindowRender.java](libroguelike/src/com/nuclearunicorn/libroguelike/render/WindowRender.java) — full rewrite around GLFW
- [Input.java](libroguelike/src/com/nuclearunicorn/libroguelike/core/Input.java) — switch to GLFW callbacks; the existing event-bus pattern (`EMouseClick.post()`) stays, only the source changes
- [Game.java:80-110](libroguelike/src/com/nuclearunicorn/libroguelike/core/Game.java#L80-L110) — replace `Display.sync` / `Display.update` / `Display.isCloseRequested` with `glfwSwapBuffers` / `glfwPollEvents` / `glfwWindowShouldClose`

### The bigger question: keep fixed-function or move to GL3 core?

Two sub-options:

**M2a — Stay on GL 2.1 compatibility.** LWJGL 3 still exposes `GL11.glBegin` etc. when you create a 2.1 context. Cheapest path. **Risk:** macOS support is on a clock; future driver updates may drop it. Mobile and web are out.

**M2b — Rewrite renderers onto GL3 core.** Replace each `glBegin(GL_QUADS) … glEnd()` block with a small batched VBO. The good news: there are only ~10 such blocks in the codebase, and they all live in dedicated renderer classes ([TilesetRenderer.java](libroguelike/src/com/nuclearunicorn/libroguelike/render/TilesetRenderer.java), [ConsoleRenderer.java](serialkiller/src/com/nuclearunicorn/serialkiller/render/ConsoleRenderer.java), etc.). Pattern:

```java
// old
glBegin(GL_QUADS);
  glTexCoord2f(u0,v0); glVertex2f(x,   y);
  glTexCoord2f(u1,v0); glVertex2f(x+w, y);
  ...
glEnd();

// new: a SpriteBatch class that accumulates into a FloatBuffer and
// flushes with glDrawArrays(GL_TRIANGLES, ...). One shader (textured quad)
// covers tiles + sprites + GUI.
```

A single `SpriteBatch` plus the existing point-sprite shader covers 90% of the rendering. **Recommended: M2b.** It's more work (~3-5 days) but it's the prerequisite for M3 anyway, and it's what kills the "this is dead tech" smell.

### Other dep updates (do alongside)

- **log4j 1.2.17 → logback-classic 1.5.x** (slf4j is already on the classpath; just swap the binding)
- **netty 3.2.5 → netty 4.1.x** if you keep the multiplayer modules. The handler API is different (`ChannelInboundHandlerAdapter` vs `SimpleChannelHandler`, `ByteBuf` vs `ChannelBuffer`). Concentrated in [NamelessProject3dServer/](NamelessProject3dServer/) and the Netty client stub.
- **commons-pool 1.6 → commons-pool2 2.12.x** if it's used (low-touch, isolated)
- **gson** → bump to 2.10.x (drop-in)

### Distribution

LWJGL 3 + jpackage gives you a self-contained native installer per platform without the user needing a JDK:

```bash
jpackage --type dmg --name SerialKiller \
  --input target/dist --main-jar serialkiller.jar \
  --main-class com.nuclearunicorn.serialkiller.game.Main \
  --runtime-image $(jlink --add-modules ...)
```

This is the cleanest desktop-distribution story Java has ever had. It produces `.dmg`, `.msi`, `.deb`, `.rpm` — but doesn't help with browsers.

**Effort M2 total:** 1-2 weeks of focused work. After this, the game runs natively on macOS Apple Silicon, Windows, Linux with a single `mvn package && jpackage` flow.

---

## 6. Milestone 3 — Web / wasm

This is where the path forks. Three real options, ranked:

### Option A — **libGDX + TeaVM** (recommended)

[libGDX](https://libgdx.com/) is a Java game framework that *already* abstracts over LWJGL 3 for desktop and over [TeaVM](https://teavm.org/) for browsers (WebGL 2.0). TeaVM transpiles Java bytecode to JS or wasm directly — no JVM in the browser, no Emscripten, output is a few hundred KB of JS.

Benefits:

- **One source tree, multiple back-ends.** `gdx-backend-lwjgl3` for desktop, `gdx-backend-teavm` for browser (community-maintained: [com.github.xpenatan.gdx-teavm](https://github.com/xpenatan/gdx-teavm)).
- **GL2/GL3-style API** (`SpriteBatch`, `ShaderProgram`, `Texture`, `BitmapFont`) that maps 1:1 onto the GL3-core renderers you'd write in M2b. The work in M2b is *not* wasted — it's the same shape of code.
- **Input**, **audio**, **file I/O** are abstracted, so the same code reads `assets/terrain.png` from disk on desktop and from an HTTP fetch in the browser.
- TeaVM supports enough of the JDK to handle this codebase's needs (collections, gson works with minor patches, log4j won't but logback can be replaced with `java.util.logging`).

Costs:

- Replace your `WindowRender` / `Input` / texture-loading layer with libGDX equivalents — but you're already rewriting those in M2 anyway.
- **Netty doesn't run in the browser.** The multiplayer modules need a WebSocket transport for the web build. Keep Netty for the desktop server, swap the client transport via an interface. The protocol is line-delimited JSON ([DelimiterBasedFrameDecoder](NamelessProject3dServer/) + Strings) — trivial to send over WS.
- Some reflection-heavy code (gson type tokens, `Class.forName`) needs TeaVM hints. Manageable.
- TeaVM has gaps in `java.io.File` / threads — single-threaded browser model means any `Thread.sleep` / blocking call needs to be removed. The current game loop is already single-threaded, so this is mostly fine.

Effort: ~2-3 weeks on top of M2.

### Option B — **CheerpJ 3**

[CheerpJ](https://cheerpj.com/) runs *unmodified* JVM bytecode in the browser via wasm. Closest thing to "no porting needed."

Why I don't recommend it:

- It runs `java.awt`/`Swing`, but **LWJGL/JOGL native GL bindings don't work** — there's no `libGL.dylib` to JNI into. CheerpJ FAQ explicitly calls out OpenGL bindings as unsupported. You'd still have to rewrite the renderer onto something that works in CheerpJ's environment, at which point libGDX is the better target.
- License is commercial for non-AGPL use.

### Option C — **GraalVM Native Image + WebAssembly**

GraalVM's wasm backend is improving but can't yet emit a wasm binary that calls into WebGL. Would need a custom shim layer. Not production-ready for a game with rendering.

### Why **A** wins

You're already paying the renderer-rewrite cost in M2. libGDX gives you that rewrite plus a free web back-end. The other options either don't work for graphics (CheerpJ) or aren't ready (GraalVM wasm).

---

## 7. Concrete order of operations

Each step is mergeable on its own:

1. **PR 1 — Maven build** *(½ day)*
   - Add `pom.xml` files; install local jars; verify `mvn -pl serialkiller -am package` builds and `java -jar` runs on a Linux box (which is the only OS LWJGL 2 has natives for here). Delete the IDEA-only build path.
2. **PR 2 — Logging swap** *(½ day)*
   - log4j 1.2 → logback-classic. Same slf4j calls; just a binding flip. Removes the CVE-bearing dep before any further work.
3. **PR 3 — LWJGL 3 + GLFW window/input** *(2-3 days)*
   - Rewrite [WindowRender.java](libroguelike/src/com/nuclearunicorn/libroguelike/render/WindowRender.java), [Input.java](libroguelike/src/com/nuclearunicorn/libroguelike/core/Input.java), `Game.run()` ([Game.java:80](libroguelike/src/com/nuclearunicorn/libroguelike/core/Game.java#L80)). Keep the renderer on GL 2.1 compat for now so the game still runs at the end of this PR.
4. **PR 4 — Asset loading** *(1 day)*
   - Replace slick-util `TextureLoader` with `STBImage`, `TrueTypeFont` with `STBTruetype` (atlas baked at startup). slick-util jar deletable.
5. **PR 5 — GL3 core renderer** *(3-5 days)*
   - Introduce `SpriteBatch` (textured quad shader, batched VBO). Migrate each `glBegin` site one at a time. Switch the GL context to 3.3 core. `EXTFramebufferObject` → `GL30` core FBO. Native macOS arm64 now works.
6. **PR 6 — jpackage installers** *(1 day)*
   - CI matrix: dmg/msi/deb. End of desktop story.
7. **PR 7 — libGDX migration** *(1-2 weeks)*
   - Replace your custom renderer/input/asset layer with libGDX `ApplicationListener` + `SpriteBatch`/`ShaderProgram`/`AssetManager`. Keep all the gameplay code untouched. The fact that you already have a clean separation between `SkillerGame` (game logic) and `WindowRender`/`Input` (engine) makes this much easier than it sounds — most files in [serialkiller/src/com/nuclearunicorn/serialkiller/game/](serialkiller/src/com/nuclearunicorn/serialkiller/game/) are pure logic and won't change.
8. **PR 8 — TeaVM web build** *(3-5 days)*
   - Add `gdx-backend-teavm`. Address reflection/IO gaps. Replace Netty client transport with WebSocket behind an interface. Output: a static directory that any HTTP server (or GitHub Pages) can serve.
9. **PR 9 — Multiplayer modernisation** *(optional, 1 week)*
   - If the 3D modules are still interesting, port `NamelessProject3dServer` from netty 3 to netty 4, and have both the desktop libGDX client and the web build connect via the abstracted transport.

---

## 8. Risks & open questions

- **rlforj 0.2** is unmaintained but small (~few thousand lines, FOV + A*). If TeaVM has trouble with it, fork-and-fix is realistic; the algorithms are public and short.
- **Asset paths.** The codebase uses `new File("src/resources/...")`-style relative paths in places. These won't work in a packaged jar or in TeaVM. Audit needed during PR 4.
- **`Class.forName` and reflection.** A grep across both src trees is worth running before PR 7; TeaVM needs `@TeaReflectionSupplier` hints or pre-registration for any reflective lookup.
- **Threading.** [ClockSignal.java](libroguelike/src/com/nuclearunicorn/libroguelike/utils/ClockSignal.java), [TimedAction.java](libroguelike/src/com/nuclearunicorn/libroguelike/utils/TimedAction.java) hint at timers and possibly background threads. The browser is single-threaded; anything using `Thread` must move to a frame-based scheduler. Not a big deal for a turn-based roguelike.
- **The 3D MMO modules** ([NamelessProject*/](NamelessProject3dServer/), [NamesProject3dClient/](NamesProject3dClient/)) appear to be a parked experiment rather than the main game. Recommend explicitly deciding to port them or to delete them at the start of PR 1, rather than letting them rot in the build.
- **TTF rendering quality.** slick-util's `TrueTypeFont` rasterises via Java's `Graphics2D`. STBTruetype rasterises differently; expect to re-tune the GUI font sizes once.

---

## 9. TL;DR

- Today: it doesn't run on a modern Mac, mostly because LWJGL 2 is dead.
- Cheapest credible fix: **Maven build + LWJGL 3 + GL3-core renderer + jpackage installers** — ~2 weeks, get a clean desktop product.
- For web/wasm: **libGDX + TeaVM** is the only mature path that fits this codebase. The renderer rewrite you'd do for desktop modernisation is reusable here, so the marginal cost of adding a web build is ~3-5 days *if* you do M2 with libGDX-shaped APIs in mind.
- Avoid CheerpJ for this codebase — its OpenGL story is the wrong fit.
- Skip the IDEA project files; Maven (or Gradle) handles import for any modern IDE.
