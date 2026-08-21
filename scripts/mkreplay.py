#!/usr/bin/env python3
"""Build a replay file from a short action script, so a test can state a scenario
directly instead of needing someone to sit down and play it.

    ./scripts/mkreplay.py out.jsonl "wait 300; walk w 3; say hello there; attack d; wait 200"

The file carries a seed, so replaying it regenerates the same town, names and rolls.
Pass --seed to pin a specific one; otherwise a fresh seed is rolled per file.

Actions (keyboard):
    wait <frames>       do nothing (use one at the start: the world has to generate)
    walk <wasd> [n]     step n times
    attack <wasd> [n]   step n times with ctrl held (ctrl+direction is the attack)
    say <text>          open talk mode, type the line, press Enter
    key <name> [n]      raw key by name, n times

Actions (scenario) - state the situation instead of navigating to it. These are honoured
only while a replay is driving the session, so they cannot be used as a console:
    tp <x> <y>          put the player here
    tp <who>            put the player next to them; "mate" is the player's spouse
    hurt <who> [times]  the player strikes them; who is "nearest", a name, or a uid prefix
    spawn <kind> <x> <y>  kind is pedestrian|police
    saidby <who> <text> somebody other than the player says something out loud
    settime <hour>      0-23
    tick [n]            advance n turns without needing a keypress to do it

Frames are 1/60s. Actions are spaced by --gap frames (default 20) so each keypress
lands on its own turn rather than arriving as a burst.
"""
import json, sys, argparse, random

# GLFW codes - the org.lwjgl.input.Keyboard shim maps its KEY_* straight onto these.
K = {
    'a': 65, 'b': 66, 'c': 67, 'd': 68, 'e': 69, 'f': 70, 'g': 71, 'h': 72, 'i': 73,
    'j': 74, 'k': 75, 'l': 76, 'm': 77, 'n': 78, 'o': 79, 'p': 80, 'q': 81, 'r': 82,
    's': 83, 't': 84, 'u': 85, 'v': 86, 'w': 87, 'x': 88, 'y': 89, 'z': 90,
    'space': 32, 'enter': 257, 'return': 257, 'backspace': 259, 'escape': 256,
    'up': 265, 'down': 264, 'left': 263, 'right': 262, 'lctrl': 341, 'lshift': 340,
}
DIRS = {'w': 'w', 'a': 'a', 's': 's', 'd': 'd',
        'up': 'up', 'down': 'down', 'left': 'left', 'right': 'right'}

# Verbs handled by the game rather than the keyboard - see TownScenario.
SCENARIO = {'tp', 'hurt', 'spawn', 'settime', 'tick', 'saidby'}


def build(script, gap, start):
    out, frame = [], start

    def press(key, chr_='', ctrl=False):
        nonlocal frame
        out.append({"type": "input", "frame": frame, "key": key,
                    "chr": chr_, "ctrl": ctrl, "shift": False, "alt": False})
        frame += gap

    def cmd(verb, args):
        nonlocal frame
        out.append({"type": "cmd", "frame": frame, "cmd": verb, "args": list(args)})
        frame += gap

    for raw in script.split(';'):
        parts = raw.split()
        if not parts:
            continue
        verb, args = parts[0].lower(), parts[1:]

        if verb == 'wait':
            frame += int(args[0])
        elif verb in ('walk', 'attack'):
            d = DIRS[args[0].lower()]
            for _ in range(int(args[1]) if len(args) > 1 else 1):
                press(K[d], d if len(d) == 1 else '', ctrl=(verb == 'attack'))
        elif verb == 'say':
            press(K['t'], 't')                      # open talk mode
            for ch in ' '.join(args):
                press(K.get(ch.lower(), 32), ch)    # key code is ignored in talk mode; chr is what counts
            press(K['enter'])
        elif verb == 'key':
            name = args[0].lower()
            for _ in range(int(args[1]) if len(args) > 1 else 1):
                press(K[name], name if len(name) == 1 else '')
        elif verb in SCENARIO:
            cmd(verb, args)
        else:
            raise SystemExit(f"unknown action: {verb}")
    return out, frame


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('out')
    ap.add_argument('script')
    ap.add_argument('--gap', type=int, default=20, help='frames between keypresses')
    ap.add_argument('--start', type=int, default=300, help='frames before the first input')
    ap.add_argument('--seed', type=int, default=None,
                    help='RNG seed to pin the run (default: a fresh random one)')
    a = ap.parse_args()

    records, last = build(a.script, a.gap, a.start)
    # A generated scenario needs a seed as much as a recorded one does - without it the
    # town, the names and every roll differ per run, and a scenario like "attack the
    # person next to you" lands only when the target happens not to have wandered.
    seed = a.seed if a.seed is not None else random.getrandbits(48)
    with open(a.out, 'w') as f:
        f.write(json.dumps({"type": "header", "version": 2,
                            "seed": seed, "generated": a.script}) + "\n")
        for r in records:
            f.write(json.dumps(r) + "\n")
    inputs = sum(1 for r in records if r['type'] == 'input')
    cmds = len(records) - inputs
    print(f"wrote {a.out}: {inputs} inputs, {cmds} scenario cmds, seed {seed}, "
          f"last at frame {last} (~{last/60:.1f}s)")


if __name__ == '__main__':
    main()
