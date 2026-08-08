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

`SPEED>1` fast-forwards the *input* only. Inference does not speed up with it, so a fast
replay outruns the very NPC reactions you are trying to observe.

## File format

JSONL, two kinds of record.

**`input`** — the only thing played back:

```json
{"type":"input","frame":700,"key":84,"chr":"t","ctrl":false,"shift":false,"alt":false}
```

Modifiers are recorded because attack is **ctrl+direction** — a replay that dropped
`key_state_ctrl` would silently turn every attack into a walk.

**Observations** — never replayed; this is the output you read and diff:

| type | carries |
|---|---|
| `turn` | turn number, player position |
| `npc` | per-turn brain dump of every human within 12 tiles (see below) |
| `say` | every line spoken, by anyone |
| `damage` | attacker, target, amount, type |
| `trace` | LLM pipeline lines — sensing, queueing, plans, resolution |
| `ready` / `replay-end` / `footer` | session boundaries |

The `npc` record is the one that makes "the NPC ignored me" diagnosable:

```
name=LAJUANA PITTS dist=1 ai=LLMAgentAI
state=top=URGENT:the player just attacked you! topSalience=95 idle=false
      busy=false sinceRequest=2 dialogue=4 attention=712f9bec fleeing=712f9bec near=true
```

— whether the stimulus arrived at all, what salience it carries *now*, and whether the
trigger or the queue is holding the reaction back. `dialogue` counts the lines held in the
NPC's transcript: an NPC that talks like it has never met you while this reads non-zero is
a prompt problem, not a memory one.

`ai` is there because the first real bug found this way was not in the salience model at
all: the NPC next to the player had **no AI object**, so nothing could reach it. Dump
every human and name the AI class — filtering to LLM agents makes an inert NPC look like
an absent one, which is the one failure the log most needs to distinguish.

## Determinism

World generation is seeded per chunk origin, so a replay regenerates the same town. NPC
name rolls, FSM roaming and model sampling are **not** seeded — runs are comparable, not
identical. Frames are recorded relative to `Replay.markReady()` (the frame the world
becomes playable), so a replay recorded after a slow model download still plays back
promptly on a warm start.

Records are flushed as they are written, so a killed process still leaves a usable file —
one line shorter, still valid JSONL.
