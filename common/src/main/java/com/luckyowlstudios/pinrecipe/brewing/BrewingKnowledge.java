package com.luckyowlstudios.pinrecipe.brewing;

import com.luckyowlstudios.pinrecipe.network.SyncKnownBrewsPayload;
import com.luckyowlstudios.pinrecipe.platform.Services;
import com.luckyowlstudios.pinrecipe.Constants;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Server side: which brewing recipes each player has unlocked, saved with the player's data. */
public final class BrewingKnowledge {
    public static final ResourceLocation LEARN_ALL_ADVANCEMENT =
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "brewing/learn_all_brewing");

    private BrewingKnowledge() {
    }

    public static Set<String> known(ServerPlayer player) {
        return ((KnownBrewsHolder) player).pinrecipe$getKnownBrews();
    }

    /**
     * Unlocks up to {@code count} random recipes the player doesn't know yet and tells the client which ones,
     * so it can show a toast. Returns the recipes learned; empty if they already know them all.
     */
    public static List<BrewingRecipe> unlockRandom(ServerPlayer player, int count) {
        Set<String> known = known(player);
        List<BrewingRecipe> locked = new ArrayList<>();
        for (BrewingRecipe recipe : BrewingRecipes.all(player.level())) {
            if (!known.contains(recipe.key())) {
                locked.add(recipe);
            }
        }
        List<BrewingRecipe> learned = new ArrayList<>();
        while (learned.size() < count && !locked.isEmpty()) {
            BrewingRecipe recipe = locked.remove(player.getRandom().nextInt(locked.size()));
            known.add(recipe.key());
            learned.add(recipe);
        }
        if (!learned.isEmpty()) {
            Services.PLATFORM.sendToPlayer(player, new SyncKnownBrewsPayload(List.copyOf(known),
                    learned.stream().map(BrewingRecipe::key).toList()));
            checkAdvancement(player);
        }
        return learned;
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

    /** Grants "Master Alchemist" once the player knows every brewing recipe. */
    public static void checkAdvancement(ServerPlayer player) {
        Set<String> known = known(player);
        for (BrewingRecipe recipe : BrewingRecipes.all(player.level())) {
            if (!known.contains(recipe.key())) {
                return;
            }
        }
        AdvancementHolder advancement = player.server.getAdvancements().get(LEARN_ALL_ADVANCEMENT);
        if (advancement != null) {
            player.getAdvancements().award(advancement, "learned_all");
        }
    }

    public static void sync(ServerPlayer player) {
        checkAdvancement(player);
        Services.PLATFORM.sendToPlayer(player, new SyncKnownBrewsPayload(List.copyOf(known(player)), List.of()));
    }
}
