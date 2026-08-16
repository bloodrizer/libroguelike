package com.nuclearunicorn.libroguelike.core.replay;

/**
 * Web build: playback reads a recorded JSONL trace from disk via Gson. Returns
 * the same "not playing" state the desktop reaches without -Dlrl.replay.play.
 */
public class ReplayPlayer {

    private ReplayPlayer() {
    }

    public static Long readSeed(String path) {
        return null;
    }

    public static ReplayPlayer open(String path) {
        return null;
    }

    public void markReady(int frame) {
    }

    public void tick(int frame) {
    }

    public void pump() {
    }

    public boolean isFinished() {
        return true;
    }
}
