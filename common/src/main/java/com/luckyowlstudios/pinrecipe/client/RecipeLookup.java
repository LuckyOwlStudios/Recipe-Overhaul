package com.luckyowlstudios.pinrecipe.client;

import com.luckyowlstudios.pinrecipe.PinnedRecipes;
import com.luckyowlstudios.pinrecipe.brewing.BrewingRecipe;
import com.luckyowlstudios.pinrecipe.brewing.BrewingRecipes;
import com.luckyowlstudios.pinrecipe.brewing.ClientBrewingKnowledge;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * "How do I make this?": finds a recipe the player knows that makes an item, and pins it.
 * Used when clicking a missing ingredient (a ghost item, or a red slot on a pinned recipe).
 */
public final class RecipeLookup {
    private RecipeLookup() {
    }

    /** Pins a known recipe that makes this item. Shows a message either way. Returns true if one was found. */
    public static boolean pinRecipeFor(ItemStack stack) {
        return pinRecipeFor(stack, List.of());
    }

    /**
     * For a slot that accepts several items (any planks, say), {@code shown} is the one on screen right now.
     * Looks up the variant the player already has some of first (Oak Planks if they hold Oak Planks), then the
     * shown one, then the rest, so the pinned recipe matches what they're actually making.
     */
    public static boolean pinRecipeFor(ItemStack shown, List<ItemStack> options) {
        Minecraft mc = Minecraft.getInstance();
        if (shown.isEmpty() || mc.level == null || mc.player == null) {
            return false;
        }
        List<ItemStack> order = new ArrayList<>();
        for (ItemStack option : options) {
            if (mc.player.getInventory().contains(s -> ItemStack.isSameItem(s, option))) {
                order.add(option);
            }
        }
        order.add(shown);
        order.addAll(options);
        for (ItemStack candidate : order) {
            String pin = findPin(mc, candidate);
            if (pin != null) {
                pin(mc, pin, candidate);
                return true;
            }
        }
        mc.gui.setOverlayMessage(Component.translatable("pinrecipe.lookup.unknown", shown.getHoverName())
                .withStyle(ChatFormatting.GRAY), false);
        return false;
    }

    @Nullable
    private static String findPin(Minecraft mc, ItemStack stack) {
        String pin = null;
        BrewingRecipe brewing = findBrewing(stack);
        if (brewing != null) {
            pin = PinnedRecipes.brewingPin(brewing.key());
        } else {
            RecipeHolder<?> recipe = findRecipe(mc.level, stack);
            if (recipe != null) {
                pin = recipe.id().toString();
            }
        }
        return pin;
    }

    private static void pin(Minecraft mc, String pin, ItemStack stack) {
        PinnedRecipes.pin(pin);
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        mc.gui.setOverlayMessage(Component.translatable("pinrecipe.lookup.pinned", stack.getHoverName()), false);
    }

    /** An unlocked brewing recipe that makes exactly this potion (same potion, same bottle type). */
    @Nullable
    public static BrewingRecipe findBrewing(ItemStack stack) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !stack.has(DataComponents.POTION_CONTENTS)) {
            return null;
        }
        for (BrewingRecipe recipe : BrewingRecipes.all(mc.level)) {
            if (!ClientBrewingKnowledge.isKnown(recipe.key())) {
                continue;
            }
            for (ItemStack output : recipe.outputs()) {
                if (ItemStack.isSameItemSameComponents(output, stack)) {
                    return recipe;
                }
            }
        }
        return null;
    }

    /** A recipe from the vanilla recipe book that the player has unlocked, preferring crafting recipes. */
    @Nullable
    private static RecipeHolder<?> findRecipe(Level level, ItemStack stack) {
        Minecraft mc = Minecraft.getInstance();
        RecipeHolder<?> best = null;
        for (RecipeHolder<?> holder : level.getRecipeManager().getRecipes()) {
            if (!mc.player.getRecipeBook().contains(holder)) {
                continue;
            }
            ItemStack result = holder.value().getResultItem(level.registryAccess());
            if (result.isEmpty() || !ItemStack.isSameItem(result, stack)) {
                continue;
            }
            if (holder.value().getType() == RecipeType.CRAFTING) {
                return holder;
            }
            if (best == null) {
                best = holder;
            }
        }
        return best;
    }
}
