package com.nuclearunicorn.libroguelike.core.replay;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Appends a replay as JSONL (§ {@link Replay}).
 *
 * <p>Every record is flushed as it is written rather than buffered until exit. The whole
 * point of a replay is to survive the run that produced it, and runs under investigation
 * get killed — a buffered file would lose exactly the tail you wanted. A shutdown hook adds
 * a footer for clean exits; a SIGKILL just leaves the file one line shorter, still valid
 * JSONL because each record is self-contained.
 */
public class ReplayRecorder {

    private final BufferedWriter out;
    private final String path;
    private boolean closed;

    private ReplayRecorder(BufferedWriter out, String path) {
        this.out = out;
        this.path = path;
    }

    /** Default directory and name for an unattended recording: replays/MM-DD-HH:MM.jsonl */
    private static final String DEFAULT_DIR = "replays";
    private static final String NAME_FORMAT = "MM-dd-HH:mm";

    /** {@code spec} is a file path, or "true" to auto-name one under replays/. */
    static ReplayRecorder open(String spec) {
        String target = "true".equalsIgnoreCase(spec) ? defaultName() : spec;
        try {
            File file = new File(target);
            if (file.getParentFile() != null) {
                file.getParentFile().mkdirs();
            }
            BufferedWriter writer = Files.newBufferedWriter(Paths.get(target), StandardCharsets.UTF_8);
            ReplayRecorder recorder = new ReplayRecorder(writer, target);

            recorder.write("header",
                    "version", 2,
                    "recorded", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new Date()),
                    "seed", com.nuclearunicorn.libroguelike.utils.Rng.seed(),
                    "llmEnabled", System.getProperty("llm.enabled", "(config)"));

            Runtime.getRuntime().addShutdownHook(new Thread(recorder::close, "replay-close"));
            System.out.println("replay: recording to " + target);
            return recorder;
        } catch (IOException e) {
            System.err.println("replay: cannot record to " + target + ": " + e);
            return null;
        }
    }

    /**
     * {@code replays/MM-DD-HH:MM.jsonl}. Minute resolution is not unique — two runs in the
     * same minute collide — so an existing file gets a {@code -2}, {@code -3} suffix rather
     * than being truncated. Silently overwriting the previous run's replay is exactly the
     * failure this system exists to prevent.
     */
    private static String defaultName() {
        String stamp = new SimpleDateFormat(NAME_FORMAT).format(new Date());
        File candidate = new File(DEFAULT_DIR, stamp + ".jsonl");
        for (int n = 2; candidate.exists(); n++) {
            candidate = new File(DEFAULT_DIR, stamp + "-" + n + ".jsonl");
        }
        return candidate.getPath();
    }

    synchronized void write(String type, Object... kv) {
        if (closed) {
            return;
        }
        StringBuilder sb = new StringBuilder(128);
        sb.append("{\"type\":").append(quote(type));
        for (int i = 0; i + 1 < kv.length; i += 2) {
            sb.append(',').append(quote(String.valueOf(kv[i]))).append(':').append(value(kv[i + 1]));
        }
        sb.append('}');
        try {
            out.write(sb.toString());
            out.newLine();
            out.flush();   // a killed process must still leave a usable replay
        } catch (IOException e) {
            System.err.println("replay: write failed: " + e);
            closed = true;
        }
    }

    private static String value(Object v) {
        if (v == null) {
            return "null";
        }
        if (v instanceof Number || v instanceof Boolean) {
            return v.toString();
        }
        return quote(String.valueOf(v));
    }

    private static String quote(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 8).append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n");  break;
                case '\r': sb.append("\\r");  break;
                case '\t': sb.append("\\t");  break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.append('"').toString();
    }

    synchronized void close() {
        if (closed) {
            return;
        }
        write("footer", "frames", Replay.frame());
        closed = true;
        try {
            out.close();
            System.out.println("replay: wrote " + path);
        } catch (IOException e) {
            System.err.println("replay: close failed: " + e);
        }
    }
}
