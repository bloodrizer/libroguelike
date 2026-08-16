package com.nuclearunicorn.serialkiller.game.ai.llm.command;

import com.google.gson.JsonObject;

/**
 * Registers one verb (§5.1). The registry uses {@link #grammarFragment} to assemble the
 * decoding grammar and {@link #parse} to turn parsed JSON args into a command instance.
 * Adding a verb = one factory + one command; nothing else changes.
 */
public interface CommandFactory {

    String verb();

    /** A GBNF rule body matching this verb's JSON object, e.g. the goto/say/wait shapes. */
    String grammarFragment();

    NpcCommand parse(JsonObject args);
}
