package com.luckyowlstudios.pinrecipe;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.luckyowlstudios.pinrecipe.platform.Services;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The list of pinned recipes, saved to config/pinrecipe.json so pins survive restarts.
 * A pin is either a recipe id ("minecraft:chest") or a brewing recipe key with the {@link #BREWING_PREFIX}.
 */
public final class PinnedRecipes {
    public static final int MAX_PINS = 4;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** '@' can't appear in a recipe id, so brewing pins never clash with recipe ids. */
    public static final String BREWING_PREFIX = "@brewing/";

    private static final List<String> PINS = new ArrayList<>();
    private static boolean loaded;
    /** The brewing recipe book's "show only brewable" toggle, saved alongside the pins. */
    private static boolean brewingFilter;

    private PinnedRecipes() {
    }

    public static boolean isBrewingFilterOn() {
        ensureLoaded();
        return brewingFilter;
    }

    public static void setBrewingFilter(boolean on) {
        ensureLoaded();
        brewingFilter = on;
        save();
    }

    public static String brewingPin(String brewingKey) {
        return BREWING_PREFIX + brewingKey;
    }

    /** The brewing recipe key of a brewing pin, or null for a regular recipe pin. */
    @Nullable
    public static String brewingKeyOf(String pin) {
        return pin.startsWith(BREWING_PREFIX) ? pin.substring(BREWING_PREFIX.length()) : null;
    }

    public static List<String> get() {
        ensureLoaded();
        return Collections.unmodifiableList(PINS);
    }

    public static boolean isPinned(String id) {
        ensureLoaded();
        return PINS.contains(id);
    }

    /** Pins the recipe, or unpins it if it is already pinned. Returns true if it is now pinned. */
    public static boolean toggle(String id) {
        ensureLoaded();
        boolean pinned;
        if (PINS.remove(id)) {
            pinned = false;
        } else {
            // Newest pin goes on top; drop the oldest once the limit is reached.
            PINS.add(0, id);
            while (PINS.size() > MAX_PINS) {
                PINS.remove(PINS.size() - 1);
            }
            pinned = true;
        }
        save();
        return pinned;
    }

    /** Pins the recipe (moving it to the top if it's already pinned). */
    public static void pin(String id) {
        ensureLoaded();
        PINS.remove(id);
        PINS.add(0, id);
        while (PINS.size() > MAX_PINS) {
            PINS.remove(PINS.size() - 1);
        }
        save();
    }

    public static void unpin(String id) {
        ensureLoaded();
        if (PINS.remove(id)) {
            save();
        }
    }

    private static Path file() {
        return Services.PLATFORM.getConfigDir().resolve(Constants.MOD_ID + ".json");
    }

    private static void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;
        Path path = file();
        if (!Files.exists(path)) {
            return;
        }
        try {
            JsonObject root = GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), JsonObject.class);
            if (root != null && root.has("brewingFilter")) {
                brewingFilter = root.get("brewingFilter").getAsBoolean();
            }
            if (root != null && root.has("pinned")) {
                for (JsonElement element : root.getAsJsonArray("pinned")) {
                    String id = element.getAsString();
                    boolean valid = brewingKeyOf(id) != null || ResourceLocation.tryParse(id) != null;
                    if (valid && !PINS.contains(id) && PINS.size() < MAX_PINS) {
                        PINS.add(id);
                    }
                }
            }
        } catch (Exception e) {
            Constants.LOG.warn("Could not read pinned recipes from {}", path, e);
        }
    }

    private static void save() {
        Path path = file();
        JsonObject root = new JsonObject();
        JsonArray array = new JsonArray();
        PINS.forEach(array::add);
        root.add("pinned", array);
        root.addProperty("brewingFilter", brewingFilter);
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException e) {
            Constants.LOG.warn("Could not save pinned recipes to {}", path, e);
        }
    }
}
