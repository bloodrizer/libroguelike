package com.nuclearunicorn.libroguelike.core.replay;

/**
 * Web build: recording writes a JSONL trace to disk and registers a shutdown
 * hook to close it — no filesystem and no shutdown hooks in a browser tab.
 *
 * {@code open} returns null, which is the same "not recording" state Replay
 * reaches on the desktop when -Dlrl.replay.record is unset.
 */
public class ReplayRecorder {

    private ReplayRecorder() {
    }

    public static ReplayRecorder open(String path) {
        return null;
    }

    public void write(String type, Object... kv) {
    }

    public void close() {
    }
}
