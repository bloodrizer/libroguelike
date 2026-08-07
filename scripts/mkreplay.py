#!/usr/bin/env python3
"""Build a replay file from a short action script, so a test can state a scenario
directly instead of needing someone to sit down and play it.

    ./scripts/mkreplay.py out.jsonl "wait 300; walk w 3; say hello there; attack d; wait 200"

Actions:
    wait <frames>       do nothing (use one at the start: the world has to generate)
    walk <wasd> [n]     step n times
    attack <wasd> [n]   step n times with ctrl held (ctrl+direction is the attack)
    say <text>          open talk mode, type the line, press Enter
    key <name> [n]      raw key by name, n times

Frames are 1/60s. Actions are spaced by --gap frames (default 20) so each keypress
lands on its own turn rather than arriving as a burst.
"""
import json, sys, argparse

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


def build(script, gap, start):
    out, frame = [], start

    def press(key, chr_='', ctrl=False):
        nonlocal frame
        out.append({"type": "input", "frame": frame, "key": key,
                    "chr": chr_, "ctrl": ctrl, "shift": False, "alt": False})
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
        else:
            raise SystemExit(f"unknown action: {verb}")
    return out, frame


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('out')
    ap.add_argument('script')
    ap.add_argument('--gap', type=int, default=20, help='frames between keypresses')
    ap.add_argument('--start', type=int, default=300, help='frames before the first input')
    a = ap.parse_args()

    records, last = build(a.script, a.gap, a.start)
    with open(a.out, 'w') as f:
        f.write(json.dumps({"type": "header", "version": 1, "generated": a.script}) + "\n")
        for r in records:
            f.write(json.dumps(r) + "\n")
    print(f"wrote {a.out}: {len(records)} inputs, last at frame {last} (~{last/60:.1f}s)")


if __name__ == '__main__':
    main()
