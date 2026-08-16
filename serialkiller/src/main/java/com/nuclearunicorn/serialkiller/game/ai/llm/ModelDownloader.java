package com.nuclearunicorn.serialkiller.game.ai.llm;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/**
 * Fetches the GGUF models declared in the config before the tiers boot — the in-engine
 * equivalent of scripts/stage-llm-models.sh (§10.1). Meant to run on a worker thread;
 * progress is published through volatile fields so the loading screen can read it every
 * frame without locking.
 *
 * <p>Resumable like the script's {@code curl -C -}: bytes land in {@code <model>.part}
 * and are renamed into place only once complete, so a truncated file never masquerades
 * as a staged model.
 */
public class ModelDownloader implements Runnable {

    private static final int BUFFER = 1 << 16;
    /** Multi-GB files over slow links: cap the handshake, never the transfer. */
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(15);
    /** A dropped connection costs nothing to retry — the .part resumes where it stopped. */
    private static final int ATTEMPTS = 3;

    private final List<LlmConfig.Tier> tiers;

    private volatile String status = "checking models...";
    private volatile long bytesDone;
    private volatile long bytesTotal;     // -1 when the server sends no content-length
    private volatile boolean done;
    private volatile String error;

    public ModelDownloader(LlmConfig.Tier... tiers) {
        this.tiers = Arrays.asList(tiers);
    }

    @Override
    public void run() {
        try {
            for (LlmConfig.Tier tier : tiers) {
                stage(tier);
            }
            status = "models ready";
        } catch (Exception e) {
            error = e.getMessage() == null ? e.toString() : e.getMessage();
            status = "model download failed";
            LlmDebug.log("model staging failed: %s", e);
        } finally {
            done = true;
        }
    }

    private void stage(LlmConfig.Tier tier) throws IOException, InterruptedException {
        if (tier == null || tier.model == null || tier.model.isEmpty()) {
            return;
        }
        Path dest = Paths.get(tier.model);
        if (Files.isRegularFile(dest)) {
            LlmDebug.log("model already staged: %s", dest);
            return;
        }
        if (tier.url == null || tier.url.isEmpty()) {
            throw new IOException("missing model " + tier.model + " and no \"url\" in llm-config.json");
        }

        String name = dest.getFileName().toString();
        for (int attempt = 1; ; attempt++) {
            try {
                download(URI.create(tier.url), dest);
                return;
            } catch (BadStatusException e) {
                throw new IOException(name + ": " + e.getMessage(), e);   //retrying won't help
            } catch (IOException e) {
                String cause = e.getMessage() == null ? e.toString() : e.getMessage();
                if (attempt >= ATTEMPTS) {
                    throw new IOException(name + ": " + cause, e);
                }
                LlmDebug.log("download of %s failed (%s), retrying %d/%d", name, cause, attempt + 1, ATTEMPTS);
                status = "retrying " + name + " (" + (attempt + 1) + "/" + ATTEMPTS + ")";
                Thread.sleep(2000);
            }
        }
    }

    private void download(URI url, Path dest) throws IOException, InterruptedException {
        Path parent = dest.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path part = dest.resolveSibling(dest.getFileName() + ".part");

        long resumeFrom = Files.isRegularFile(part) ? Files.size(part) : 0;
        String name = dest.getFileName().toString();
        status = "downloading " + name;
        bytesDone = resumeFrom;
        bytesTotal = -1;

        LlmDebug.log("downloading %s from %s (resume at %d bytes)", dest, url, resumeFrom);

        HttpClient http = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)   // huggingface hands off to a CDN
                .build();

        HttpRequest.Builder rb = HttpRequest.newBuilder(url).GET();
        if (resumeFrom > 0) {
            rb.header("Range", "bytes=" + resumeFrom + "-");
        }

        HttpResponse<InputStream> response = http.send(rb.build(), HttpResponse.BodyHandlers.ofInputStream());
        int code = response.statusCode();
        if (code != 200 && code != 206) {
            response.body().close();
            throw new BadStatusException("HTTP " + code);
        }

        boolean resumed = code == 206;   // 200 means the server ignored our Range: start over
        if (!resumed) {
            bytesDone = 0;
        }
        long remaining = response.headers().firstValueAsLong("content-length").orElse(-1L);
        bytesTotal = remaining < 0 ? -1 : bytesDone + remaining;

        try (InputStream in = response.body();
             OutputStream out = Files.newOutputStream(part, StandardOpenOption.CREATE,
                     resumed ? StandardOpenOption.APPEND : StandardOpenOption.TRUNCATE_EXISTING)) {
            byte[] buf = new byte[BUFFER];
            int read;
            while ((read = in.read(buf)) > 0) {
                out.write(buf, 0, read);
                bytesDone += read;
            }
        }

        Files.move(part, dest, StandardCopyOption.REPLACE_EXISTING);
        LlmDebug.log("staged %s (%d bytes)", dest, bytesDone);
    }

    public boolean isDone() {
        return done;
    }

    public String getError() {
        return error;
    }

    public String getStatus() {
        return status;
    }

    /** Transfer size as "1.40 / 2.51 GB", or an empty string when nothing is in flight. */
    public String getSizeLine() {
        long total = bytesTotal;
        long current = bytesDone;
        if (current <= 0 && total <= 0) {
            return "";
        }
        long scale = Math.max(current, total);   //both numbers share the bigger one's unit
        boolean useGb = scale >= (1L << 30);
        double div = useGb ? (1L << 30) : (1L << 20);
        String unit = useGb ? "GB" : "MB";
        if (total <= 0) {
            return String.format("%.2f %s", current / div, unit);
        }
        return String.format("%.2f / %.2f %s", current / div, total / div, unit);
    }

    /** 0..1 for the file in flight, or -1 when the total size is unknown. */
    public float getFraction() {
        long total = bytesTotal;
        if (total <= 0) {
            return -1;
        }
        return Math.min(1f, (float) bytesDone / total);
    }

    /** Non-retryable: the server answered, it just isn't handing over the file. */
    private static class BadStatusException extends IOException {
        BadStatusException(String message) {
            super(message);
        }
    }
}
