package com.luckyowlstudios.pinrecipe.brewing;

import com.luckyowlstudios.pinrecipe.network.SyncKnownBrewsPayload;
import com.luckyowlstudios.pinrecipe.platform.Services;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Server side: which brewing recipes each player has unlocked, saved with the player's data. */
public final class BrewingKnowledge {
    private BrewingKnowledge() {
    }

    public static Set<String> known(ServerPlayer player) {
        return ((KnownBrewsHolder) player).pinrecipe$getKnownBrews();
    }

    /** Unlocks one random recipe the player doesn't know yet. Empty if they already know them all. */
    public static Optional<BrewingRecipe> unlockRandom(ServerPlayer player) {
        Set<String> known = known(player);
        List<BrewingRecipe> locked = new ArrayList<>();
        for (BrewingRecipe recipe : BrewingRecipes.all(player.level())) {
            if (!known.contains(recipe.key())) {
                locked.add(recipe);
            }
        }
        if (locked.isEmpty()) {
            return Optional.empty();
        }
        BrewingRecipe recipe = locked.get(player.getRandom().nextInt(locked.size()));
        known.add(recipe.key());
        sync(player);
        return Optional.of(recipe);
    }

    public static int unlockAll(ServerPlayer player) {
        Set<String> known = known(player);
        int before = known.size();
        BrewingRecipes.all(player.level()).forEach(r -> known.add(r.key()));
        sync(player);
        return known.size() - before;
    }

    public static void reset(ServerPlayer player) {
        known(player).clear();
        sync(player);
    }

    public static void sync(ServerPlayer player) {
        Services.PLATFORM.sendToPlayer(player, new SyncKnownBrewsPayload(List.copyOf(known(player))));
    }
}
