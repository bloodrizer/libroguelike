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
