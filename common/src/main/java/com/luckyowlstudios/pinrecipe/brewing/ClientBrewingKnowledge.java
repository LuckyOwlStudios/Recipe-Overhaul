package com.luckyowlstudios.pinrecipe.brewing;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/** Client side copy of the player's unlocked brewing recipes, sent by the server. */
public final class ClientBrewingKnowledge {
    private static final Set<String> KNOWN = new HashSet<>();

    private ClientBrewingKnowledge() {
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
