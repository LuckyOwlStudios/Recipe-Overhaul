package com.luckyowlstudios.codex.brewing;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Lists every brewing stand recipe by asking the game's own {@link PotionBrewing} which bottle and
 * ingredient combinations brew something. Because it only uses the public brewing API, it also picks
 * up recipes other mods add (Fabric's brewing builder and NeoForge's brewing recipes).
 * <p>
 * Runs the same way on the server and the client, so recipe keys match on both sides.
 */
public final class BrewingRecipes {
    private static final Item[] CONTAINERS = {Items.POTION, Items.SPLASH_POTION, Items.LINGERING_POTION};

    private static PotionBrewing cachedFor;
    private static List<BrewingRecipe> cached = List.of();

    private BrewingRecipes() {
    }

    public static synchronized List<BrewingRecipe> all(Level level) {
        PotionBrewing brewing = level.potionBrewing();
        if (brewing != cachedFor) {
            cached = List.copyOf(build(level, brewing));
            cachedFor = brewing;
        }
        return cached;
    }

    public static Optional<BrewingRecipe> byKey(Level level, String key) {
        return all(level).stream().filter(r -> r.key().equals(key)).findFirst();
    }

    private static List<BrewingRecipe> build(Level level, PotionBrewing brewing) {
        List<Holder.Reference<Potion>> potions = level.registryAccess().registryOrThrow(Registries.POTION).holders().toList();
        List<ItemStack> ingredients = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            ItemStack stack = new ItemStack(item);
            if (!stack.isEmpty() && brewing.isIngredient(stack)) {
                ingredients.add(stack);
            }
        }

        Map<String, BrewingRecipe> recipes = new LinkedHashMap<>();

        // Potion recipes: one entry per base potion + result. Ingredients that brew the same result from the same
        // potion share an entry (e.g. Water Bottle + any of 12 items makes a Mundane Potion).
        for (Holder.Reference<Potion> potion : potions) {
            ItemStack input = PotionContents.createItemStack(Items.POTION, potion);
            Map<String, List<ItemStack>> ingredientsByResult = new LinkedHashMap<>();
            Map<String, ItemStack> outputsByResult = new LinkedHashMap<>();
            for (ItemStack ingredient : ingredients) {
                if (!brewing.hasMix(input, ingredient)) {
                    continue;
                }
                ItemStack output = brewing.mix(ingredient, input.copy());
                if (output.isEmpty() || ItemStack.isSameItemSameComponents(output, input)) {
                    continue;
                }
                if (!output.is(Items.POTION) && brewing.isContainerIngredient(ingredient)) {
                    continue; // Gunpowder and friends are listed once below instead of once per potion.
                }
                String result = id(output.getItem()) + "|" + output.getComponentsPatch();
                ingredientsByResult.computeIfAbsent(result, r -> new ArrayList<>()).add(ingredient);
                outputsByResult.putIfAbsent(result, output);
            }
            for (Map.Entry<String, List<ItemStack>> entry : ingredientsByResult.entrySet()) {
                List<ItemStack> group = entry.getValue();
                ItemStack output = outputsByResult.get(entry.getKey());
                String key;
                if (group.size() == 1) {
                    key = "potion/" + potion.key().location() + "/" + id(group.get(0).getItem());
                    if (!output.is(Items.POTION)) {
                        key += "/" + id(output.getItem());
                    }
                } else {
                    key = "potion/" + potion.key().location() + "/=" + resultId(output);
                }
                recipes.putIfAbsent(key, new BrewingRecipe(key, List.of(input), List.copyOf(group), List.of(output),
                        potionName(input), potionName(output), stack -> hasPotion(stack, potion)));
            }
        }

        // Container recipes (potion -> splash -> lingering): one entry each, cycling through every potion.
        for (Item container : CONTAINERS) {
            for (ItemStack ingredient : ingredients) {
                if (!brewing.isContainerIngredient(ingredient)) {
                    continue;
                }
                List<ItemStack> inputs = new ArrayList<>();
                List<ItemStack> outputs = new ArrayList<>();
                Item resultItem = null;
                for (Holder.Reference<Potion> potion : potions) {
                    ItemStack input = PotionContents.createItemStack(container, potion);
                    if (!brewing.hasMix(input, ingredient)) {
                        continue;
                    }
                    ItemStack output = brewing.mix(ingredient, input.copy());
                    if (output.isEmpty() || output.is(container)) {
                        continue;
                    }
                    inputs.add(input);
                    outputs.add(output);
                    resultItem = output.getItem();
                }
                if (inputs.isEmpty()) {
                    continue;
                }
                String key = "container/" + id(container) + "/" + id(ingredient.getItem());
                recipes.putIfAbsent(key, new BrewingRecipe(key, List.copyOf(inputs), List.of(ingredient), List.copyOf(outputs),
                        container.getDescription(), resultItem.getDescription(),
                        stack -> stack.is(container) && stack.has(DataComponents.POTION_CONTENTS)));
            }
        }
        return new ArrayList<>(recipes.values());
    }

    /**
     * The item name, marked "(Long)" or "II" for upgraded potions so that, for example,
     * Swiftness + Redstone doesn't read as making plain Swiftness.
     */
    private static Component potionName(ItemStack stack) {
        Component name = stack.getHoverName();
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        if (contents == null || contents.potion().isEmpty()) {
            return name;
        }
        String path = contents.potion().get().unwrapKey().map(key -> key.location().getPath()).orElse("");
        if (path.startsWith("long_")) {
            return Component.translatable("codex.brewing.long", name);
        }
        if (path.startsWith("strong_")) {
            return Component.translatable("codex.brewing.strong", name);
        }
        return name;
    }

    private static boolean hasPotion(ItemStack stack, Holder<Potion> potion) {
        if (!stack.is(Items.POTION) && !stack.is(Items.SPLASH_POTION) && !stack.is(Items.LINGERING_POTION)) {
            return false;
        }
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        return contents != null && contents.is(potion);
    }

    /** e.g. "minecraft:mundane" for a Mundane Potion, or the item id for anything else. */
    private static String resultId(ItemStack output) {
        PotionContents contents = output.get(DataComponents.POTION_CONTENTS);
        if (contents != null && contents.potion().isPresent()) {
            String potion = contents.potion().get().unwrapKey().map(k -> k.location().toString()).orElse("?");
            return output.is(Items.POTION) ? potion : id(output.getItem()) + "/" + potion;
        }
        return id(output.getItem()).toString();
    }

    private static ResourceLocation id(Item item) {
        return BuiltInRegistries.ITEM.getKey(item);
    }
}
