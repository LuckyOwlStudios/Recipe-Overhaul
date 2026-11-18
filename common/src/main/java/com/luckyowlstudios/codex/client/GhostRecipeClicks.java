package com.luckyowlstudios.codex.client;

import com.luckyowlstudios.codex.mixin.AbstractContainerScreenAccessor;
import com.luckyowlstudios.codex.mixin.GhostIngredientAccessor;
import com.luckyowlstudios.codex.mixin.RecipeBookComponentAccessor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.recipebook.GhostRecipe;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import net.minecraft.world.inventory.Slot;

import java.util.List;

/**
 * Clicking a ghost ingredient that vanilla's recipe book shows in a crafting grid or furnace pins the
 * recipe for that ingredient, so you can see how to make what you're missing.
 */
public final class GhostRecipeClicks {
    private GhostRecipeClicks() {
    }

    public static boolean mouseClicked(Screen screen, double mouseX, double mouseY, int button) {
        if (button != 0 || !(screen instanceof RecipeUpdateListener listener)
                || !(screen instanceof AbstractContainerScreen<?> container)
                || !container.getMenu().getCarried().isEmpty()) {
            return false;
        }
        RecipeBookComponent book = listener.getRecipeBookComponent();
        if (book == null) {
            return false;
        }
        GhostRecipe ghost = ((RecipeBookComponentAccessor) book).codex$getGhostRecipe();
        if (ghost == null || ghost.size() == 0) {
            return false;
        }
        AbstractContainerScreenAccessor access = (AbstractContainerScreenAccessor) container;
        int left = access.codex$getLeftPos();
        int top = access.codex$getTopPos();
        // Entry 0 is the result; the rest are ingredients.
        for (int i = 1; i < ghost.size(); i++) {
            GhostRecipe.GhostIngredient ingredient = ghost.get(i);
            int x = left + ingredient.getX();
            int y = top + ingredient.getY();
            if (mouseX < x || mouseY < y || mouseX >= x + 16 || mouseY >= y + 16) {
                continue;
            }
            if (slotHasItem(container, ingredient.getX(), ingredient.getY())) {
                return false; // A real item is there now; let the click through.
            }
            RecipeLookup.pinRecipeFor(ingredient.getItem(),
                    List.of(((GhostIngredientAccessor) ingredient).codex$getIngredient().getItems()));
            return true;
        }
        return false;
    }

    private static boolean slotHasItem(AbstractContainerScreen<?> container, int x, int y) {
        for (Slot slot : container.getMenu().slots) {
            if (slot.x == x && slot.y == y) {
                return slot.hasItem();
            }
        }
        return false;
    }
}
