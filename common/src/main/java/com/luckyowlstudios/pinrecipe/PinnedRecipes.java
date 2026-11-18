package com.luckyowlstudios.pinrecipe;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.luckyowlstudios.pinrecipe.platform.Services;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** The list of pinned recipe ids, saved to config/pinrecipe.json so pins survive restarts. */
public final class PinnedRecipes {
    public static final int MAX_PINS = 4;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final List<ResourceLocation> PINS = new ArrayList<>();
    private static boolean loaded;

    private PinnedRecipes() {
    }

    public static List<ResourceLocation> get() {
        ensureLoaded();
        return Collections.unmodifiableList(PINS);
    }

    public static boolean isPinned(ResourceLocation id) {
        ensureLoaded();
        return PINS.contains(id);
    }

    /** Pins the recipe, or unpins it if it is already pinned. Returns true if it is now pinned. */
    public static boolean toggle(ResourceLocation id) {
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

    public static void unpin(ResourceLocation id) {
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
            if (root != null && root.has("pinned")) {
                for (JsonElement element : root.getAsJsonArray("pinned")) {
                    ResourceLocation id = ResourceLocation.tryParse(element.getAsString());
                    if (id != null && !PINS.contains(id) && PINS.size() < MAX_PINS) {
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
        PINS.forEach(id -> array.add(id.toString()));
        root.add("pinned", array);
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException e) {
            Constants.LOG.warn("Could not save pinned recipes to {}", path, e);
        }
    }
}
