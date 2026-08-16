package com.nuclearunicorn.libroguelike.core.replay;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nuclearunicorn.libroguelike.core.Game;
import com.nuclearunicorn.libroguelike.core.Input;
import com.nuclearunicorn.libroguelike.events.EKeyPress;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Drives {@link Input} from a recorded replay instead of the keyboard (§ {@link Replay}).
 *
 * <p>Only {@code input} records are consumed; observations in the same file are ignored
 * here — they are output, not script. Records are released on their recorded frame so
 * playback keeps the original pacing by default. That matters more than it looks: NPC
 * inference is asynchronous and takes seconds of wall time, so fast-forwarding the input
 * would race past the very reactions the replay was made to inspect.
 *
 * <p>After the last input the session keeps running for {@code replay.tailFrames} so
 * in-flight plans still land in the log, then optionally stops the game.
 */
public class ReplayPlayer {

    private static class Frame {
        int frame;
        int key;
        char chr;
        boolean ctrl;
        boolean shift;
        boolean alt;
        /** Set for a scenario record; when present the frame is a command, not a keypress. */
        String verb;
        String[] args;
    }

    private final Deque<Frame> pending = new ArrayDeque<>();
    private final double speed;
    private final int tailFrames;
    private final boolean exitAtEnd;
    private final int total;

    private int startFrame = -1;
    private int lastInputFrame;
    private boolean finished;
    private boolean stopped;

    private ReplayPlayer(List<Frame> frames) {
        pending.addAll(frames);
        this.total = frames.size();
        this.speed = Double.parseDouble(System.getProperty("replay.speed", "1.0"));
        this.tailFrames = Integer.getInteger("replay.tailFrames", 600);
        this.exitAtEnd = Boolean.parseBoolean(System.getProperty("replay.exitAtEnd", "true"));
        this.lastInputFrame = frames.isEmpty() ? 0 : frames.get(frames.size() - 1).frame;
    }

    /**
     * The recorded seed, or null for a v1 file that predates it. Read on its own, before the
     * rest of the file: seeding has to happen before the world is built, and by the time the
     * player is constructed the first rolls are long spent.
     */
    static Long readSeed(String path) {
        Gson gson = new Gson();
        try {
            for (String line : Files.readAllLines(Paths.get(path), StandardCharsets.UTF_8)) {
                if (line.isEmpty()) {
                    continue;
                }
                JsonObject obj = gson.fromJson(line, JsonObject.class);
                if (obj != null && obj.has("type") && "header".equals(obj.get("type").getAsString())) {
                    return obj.has("seed") ? obj.get("seed").getAsLong() : null;
                }
            }
        } catch (Exception e) {
            System.err.println("replay: cannot read seed from " + path + ": " + e);
        }
        return null;
    }

    static ReplayPlayer open(String path) {
        Gson gson = new Gson();
        java.util.ArrayList<Frame> frames = new java.util.ArrayList<>();
        try {
            for (String line : Files.readAllLines(Paths.get(path), StandardCharsets.UTF_8)) {
                if (line.isEmpty()) {
                    continue;
                }
                JsonObject obj = gson.fromJson(line, JsonObject.class);
                if (obj == null || !obj.has("type")) {
                    continue;
                }
                String type = obj.get("type").getAsString();
                if ("cmd".equals(type)) {
                    Frame c = new Frame();
                    c.frame = obj.get("frame").getAsInt();
                    c.verb = obj.get("cmd").getAsString();
                    JsonArray raw = obj.has("args") ? obj.getAsJsonArray("args") : new JsonArray();
                    c.args = new String[raw.size()];
                    for (int i = 0; i < raw.size(); i++) {
                        c.args[i] = raw.get(i).getAsString();
                    }
                    frames.add(c);
                    continue;
                }
                if (!"input".equals(type)) {
                    continue;
                }
                Frame f = new Frame();
                f.frame = obj.get("frame").getAsInt();
                f.key = obj.get("key").getAsInt();
                String chr = obj.has("chr") ? obj.get("chr").getAsString() : "";
                f.chr = chr.isEmpty() ? 0 : chr.charAt(0);
                f.ctrl = obj.has("ctrl") && obj.get("ctrl").getAsBoolean();
                f.shift = obj.has("shift") && obj.get("shift").getAsBoolean();
                f.alt = obj.has("alt") && obj.get("alt").getAsBoolean();
                frames.add(f);
            }
        } catch (IOException e) {
            System.err.println("replay: cannot read " + path + ": " + e);
            return null;
        }
        System.out.println("replay: playing " + path + " (" + frames.size() + " records)");
        return new ReplayPlayer(frames);
    }

    /**
     * Release every input whose recorded frame has come due. Called from
     * {@link Input#update()} in place of polling the real keyboard.
     */
    public void pump() {
        if (startFrame < 0) {
            return;   // world not built yet; see Replay.markReady()
        }
        double elapsed = (Replay.frame() - startFrame) * speed;

        while (!pending.isEmpty() && pending.peek().frame <= elapsed) {
            Frame f = pending.poll();
            if (f.verb != null) {
                dispatch(f);
                continue;
            }
            // Modifier state first: attack is ctrl+direction, and the controller reads the
            // flag when the direction key is handled, not when ctrl was pressed.
            Input.key_state_ctrl = f.ctrl;
            Input.key_state_shft = f.shift;
            Input.key_state_alt = f.alt;
            new EKeyPress(f.key, f.chr).post();
            Replay.observe("replay-input", "frame", Replay.frame(), "key", f.key,
                    "chr", f.chr == 0 ? "" : String.valueOf(f.chr), "ctrl", f.ctrl);
        }
    }

    /**
     * Hand a scenario command to the game. Nothing here is reachable outside playback: the
     * records only exist in a replay file and only this class reads them.
     */
    private void dispatch(Frame f) {
        Scenario handler = Scenario.handler();
        String line = f.verb + " " + String.join(" ", f.args);
        if (handler == null) {
            System.err.println("replay: no scenario handler for '" + line + "'");
            return;
        }
        boolean ok;
        try {
            ok = handler.run(f.verb, f.args);
        } catch (RuntimeException e) {
            System.err.println("replay: scenario '" + line + "' failed: " + e);
            Replay.observe("scenario", "cmd", line, "ok", false, "error", String.valueOf(e));
            return;
        }
        if (!ok) {
            System.err.println("replay: unknown scenario command '" + line + "'");
        }
        System.out.println("replay: scenario " + line + (ok ? "" : " (UNKNOWN)"));
        Replay.observe("scenario", "cmd", line, "ok", ok);
    }

    /** Anchor the replay clock to the frame the world became playable. */
    void markReady(int frame) {
        if (startFrame < 0) {
            startFrame = frame;
            System.out.println("replay: world ready at frame " + frame + ", starting playback");
        }
    }

    /** Frame bookkeeping: decide when the replay (plus its tail) is done. */
    void tick(int frame) {
        if (finished || startFrame < 0 || !pending.isEmpty()) {
            return;
        }
        double elapsed = (frame - startFrame) * speed;
        if (elapsed < lastInputFrame + tailFrames) {
            return;
        }
        finished = true;
        System.out.println("replay: finished (" + total + " inputs replayed)");
        Replay.observe("replay-end", "frame", frame, "inputs", total);
        if (exitAtEnd && !stopped) {
            stopped = true;
            Replay.shutdown();
            Game.stop();
        }
    }

    public boolean isFinished() {
        return finished;
    }
}
