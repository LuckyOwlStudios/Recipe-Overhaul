package com.luckyowlstudios.pinrecipe.brewing;

import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Predicate;

/**
 * One brewing stand recipe as shown in the brewing recipe book.
 *
 * @param key        stable id used to remember which recipes a player has unlocked
 * @param inputs     bottles that go in the bottom slots; container recipes cycle through every potion
 * @param ingredients items that can go in the top slot; usually one, but several when they all brew the same
 *                    thing (vanilla has 12 ways to turn a Water Bottle into a Mundane Potion)
 * @param outputs    the result for each entry in {@code inputs}
 * @param inputName  name of the base bottle, e.g. "Awkward Potion" or "Potion"
 * @param outputName name of the result, e.g. "Potion of Swiftness" or "Splash Potion"
 * @param inputTest  whether a stack can be used as the base bottle
 */
public record BrewingRecipe(String key, List<ItemStack> inputs, List<ItemStack> ingredients, List<ItemStack> outputs,
                           Component inputName, Component outputName, Predicate<ItemStack> inputTest) {

    /** Index into inputs/outputs to show right now, changing once per second. */
    public int cycleIndex() {
        return (int) ((Util.getMillis() / 1000L) % inputs.size());
    }

    public ItemStack currentInput() {
        return inputs.get(cycleIndex());
    }

    public ItemStack currentOutput() {
        return outputs.get(cycleIndex());
    }

    /** The ingredient to show right now; cycles once per second when there are several. */
    public ItemStack ingredient() {
        return ingredients.get((int) ((Util.getMillis() / 1000L) % ingredients.size()));
    }

    public boolean ingredientMatches(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        for (ItemStack ingredient : ingredients) {
            if (ItemStack.isSameItem(stack, ingredient)) {
                return true;
            }
        }
        return false;
    }

    /** e.g. "Potion of Swiftness (Awkward Potion + Sugar)". */
    public Component describe() {
        return Component.translatable("pinrecipe.brewing.recipe", outputName, inputName, ingredient().getHoverName());
    }
}
