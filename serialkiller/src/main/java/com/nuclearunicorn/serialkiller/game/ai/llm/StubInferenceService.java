package com.nuclearunicorn.serialkiller.game.ai.llm;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Offline stand-in for {@link InferenceService} (§13). Returns a fixed command chain
 * one poll after submit, so the interpreter, threading boundary, and AI wiring can be
 * exercised headless with no model or server. Also the graceful path when
 * {@code llm.enabled = true} but no server is reachable.
 */
public class StubInferenceService implements InferenceService {

    private static final String CANNED_PLAN =
            "[{\"verb\":\"say\",\"text\":\"Just another day in this town.\"}," +
            " {\"verb\":\"goto\",\"target\":\"random\"}," +
            " {\"verb\":\"wait\",\"ticks\":3}]";

    private final String plan;
    private final ConcurrentHashMap<String, String> pending = new ConcurrentHashMap<>();

    public StubInferenceService() {
        this(CANNED_PLAN);
    }

    public StubInferenceService(String plan) {
        this.plan = plan;
    }

    @Override
    public void submit(String uid, String prompt) {
        pending.putIfAbsent(uid, plan);
    }

    @Override
    public boolean isBusy(String uid) {
        return pending.containsKey(uid);
    }

    @Override
    public String poll(String uid) {
        return pending.remove(uid);
    }

    @Override
    public void shutdown() {
        pending.clear();
    }
}
