package com.luckyowlstudios.pinrecipe.brewing;

import com.luckyowlstudios.pinrecipe.network.SyncKnownBrewsPayload;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/** Client side copy of the player's unlocked brewing recipes, sent by the server. */
public final class ClientBrewingKnowledge {
    private static final Set<String> KNOWN = new HashSet<>();

    /** Set by the client entrypoint; shows the "recipe learned" toast. Kept as a hook so servers never load toast code. */
    public static Consumer<List<String>> learnedListener = keys -> {
    };

    private ClientBrewingKnowledge() {
    }

    public static void handle(SyncKnownBrewsPayload payload) {
        set(payload.keys());
        if (!payload.learned().isEmpty()) {
            learnedListener.accept(payload.learned());
        }
    }

    public static synchronized void set(Collection<String> keys) {
        KNOWN.clear();
        KNOWN.addAll(keys);
    }

    public static synchronized void clear() {
        KNOWN.clear();
    }

    public static synchronized boolean isKnown(String key) {
        return KNOWN.contains(key);
    }
}
