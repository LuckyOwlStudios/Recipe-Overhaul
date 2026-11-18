package com.luckyowlstudios.codex.client;

import com.luckyowlstudios.codex.mixin.RecipeBookComponentAccessor;
import com.luckyowlstudios.codex.mixin.RecipeBookPageAccessor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.recipebook.GhostRecipe;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookPage;
import net.minecraft.client.gui.screens.recipebook.RecipeButton;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;

/**
 * Draws the same gold outline the brewing book uses around the recipe you last clicked in a vanilla
 * recipe book (inventory, crafting table, furnaces). It's drawn on top after the screen renders and
 * doesn't change any vanilla rendering, so other recipe book mods aren't affected.
 */
public final class RecipeBookSelection {
    private static final int OUTLINE_COLOR = 0xFFFFD700;
    private static final int BUTTON_SIZE = 25;

    private RecipeBookSelection() {
    }

    public static void render(Screen screen, GuiGraphics graphics) {
        if (!(screen instanceof RecipeUpdateListener listener)) {
            return;
        }
        RecipeBookComponent book = listener.getRecipeBookComponent();
        if (book == null || !book.isVisible()) {
            return;
        }
        RecipeBookPage page = ((RecipeBookComponentAccessor) book).codex$getRecipeBookPage();
        if (page == null) {
            return;
        }
        RecipeBookPageAccessor pageAccess = (RecipeBookPageAccessor) page;
        if (pageAccess.codex$getOverlay() != null && pageAccess.codex$getOverlay().isVisible()) {
            return; // The alternatives popup covers the grid.
        }
        RecipeHolder<?> selected = selected(book, page);
        if (selected == null) {
            return;
        }
        for (RecipeButton button : pageAccess.codex$getButtons()) {
            if (button.visible && button.getCollection() != null
                    && button.getCollection().getRecipes().contains(selected)) {
                // The book draws at z 100: go just above its background but below its items and tooltips.
                graphics.pose().pushPose();
                graphics.pose().translate(0, 0, 120);
                graphics.renderOutline(button.getX(), button.getY(), BUTTON_SIZE, BUTTON_SIZE, OUTLINE_COLOR);
                graphics.pose().popPose();
                return;
            }
        }
    }

    /** The recipe shown as a ghost in the grid, or else the last one clicked in the book. */
    @Nullable
    private static RecipeHolder<?> selected(RecipeBookComponent book, RecipeBookPage page) {
        GhostRecipe ghost = ((RecipeBookComponentAccessor) book).codex$getGhostRecipe();
        if (ghost != null && ghost.getRecipe() != null) {
            return ghost.getRecipe();
        }
        return page.getLastClickedRecipe();
    }
}
