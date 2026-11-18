package com.luckyowlstudios.codex.brewing;

import java.util.Set;

/** Added to ServerPlayer by a mixin: the keys of the brewing recipes this player has unlocked. */
public interface KnownBrewsHolder {
    Set<String> codex$getKnownBrews();
}
