package com.nuclearunicorn.serialkiller.game.ai.llm;

import java.io.File;
import java.util.List;

/**
 * "Why won't the LLM start?", answered without opening a window.
 *
 * <p>{@code ./scripts/run.sh --llm-check} runs the whole boot path — config, binary, model
 * files, both tiers, one real completion — and prints what happened at each step. The
 * alternative was launching the game, watching a loading screen do nothing for two minutes
 * and reading a one-line degradation notice that had already scrolled past.
 */
public final class LlmDoctor {

    private LlmDoctor() {}

    /** @return a process exit code: 0 when live inference works, 1 when it does not. */
    public static int run() {
        LlmConfig config = LlmRuntime.peekConfig();
        LlmDebug.setEnabled(true);

        say("config          enabled=%s binary=%s", config.enabled, config.serverBinary);
        if (!config.enabled) {
            say("llm.enabled is false - nothing to check. NPCs run the FSM.");
            return 0;
        }

        boolean onPath = LlamaServerManager.isBinaryAvailable(config.serverBinary);
        say("binary          %s on PATH: %s", config.serverBinary, onPath ? "yes" : "NO");
        if (!onPath) {
            say("FAIL            install llama.cpp, or set \"serverBinary\" to its full path");
            return 1;
        }

        if (!describeModel("reactor", config.reactor) | !describeModel("director", config.director)) {
            say("FAIL            stage the models first: scripts/stage-llm-models.sh");
            return 1;
        }

        say("");
        say("booting tiers (this is the part that takes a minute on a cold cache)...");
        Thread watcher = progressWatcher();
        watcher.start();
        LlmRuntime.init();
        watcher.interrupt();

        say("");
        InferenceService reactor = LlmRuntime.reactor();
        boolean live = reactor instanceof LlamaHttpInferenceService;
        say("reactor         %s", live ? "LIVE on port " + config.reactor.port
                : "DEGRADED (" + LlmRuntime.degradedReason() + ")");
        say("director        %s", LlmRuntime.director() == null ? "off" : "on");
        if (!live) {
            say("");
            say("FAIL            %s", LlmRuntime.degradedReason());
            say("                full server log: %s", LlamaServerManager.bootLog());
            return 1;
        }

        return probe(reactor) ? 0 : 1;
    }

    /** One real round-trip, because a healthy server that answers nothing is still broken. */
    private static boolean probe(InferenceService reactor) {
        String uid = "llm-doctor";
        long start = System.currentTimeMillis();
        reactor.submit(uid, "You are a townsperson. Reply with a short greeting.\n", 50);
        for (int i = 0; i < 60; i++) {
            String reply = reactor.poll(uid);
            if (reply != null) {
                say("completion      %dms: %s", System.currentTimeMillis() - start,
                        reply.trim().replace('\n', ' '));
                say("OK              live inference works");
                return true;
            }
            sleep(500);
        }
        say("FAIL            server is healthy but answered nothing in 30s");
        return false;
    }

    private static boolean describeModel(String label, LlmConfig.Tier tier) {
        if (tier == null || tier.model == null || tier.model.isEmpty()) {
            say("%-15s not configured", label);
            return true;
        }
        File f = new File(tier.model);
        say("%-15s %s port=%d ctx=%d threads=%d ngl=%d  %s", label, tier.model, tier.port,
                tier.contextSize, tier.threads, tier.gpuLayers,
                f.isFile() ? (f.length() >> 20) + "MB" : "MISSING");
        return f.isFile();
    }

    /** A line a second while the server loads, so the wait is visibly a wait and not a hang. */
    private static Thread progressWatcher() {
        Thread t = new Thread(() -> {
            String previous = null;
            while (!Thread.currentThread().isInterrupted()) {
                String tier = LlamaServerManager.bootTier();
                String line = (tier == null ? "" : tier + " ") + LlamaServerManager.bootStage()
                        + " " + LlamaServerManager.bootElapsedSeconds() + "s: "
                        + LlamaServerManager.bootDetail();
                if (tier != null && !line.equals(previous)) {
                    say("  %s", line);
                    previous = line;
                }
                sleep(1000);
            }
        }, "llm-doctor-progress");
        t.setDaemon(true);
        return t;
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void say(String format, Object... args) {
        System.out.println(args.length == 0 ? format : String.format(format, args));
    }
}
